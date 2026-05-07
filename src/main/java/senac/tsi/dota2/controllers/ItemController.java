package senac.tsi.dota2.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.tsi.dota2.entities.Item;
import senac.tsi.dota2.exceptions.ItemNotFoundException;
import senac.tsi.dota2.repositories.ItemRepository;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Items",
        description = "Routes to manage Dota 2 In-Game Items")
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemRepository repository;
    private final PagedResourcesAssembler<Item> pagedResourcesAssembler;
    private final Map<String, IdempotentCreateResponse> createItemResponses = new ConcurrentHashMap<>();
    private final Object createItemIdempotencyLock = new Object();

    public ItemController(ItemRepository repository, PagedResourcesAssembler<Item> pagedResourcesAssembler) {
        this.repository = repository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all items (Paginated)",
            description = "Accesses the global catalog of available items and equipment in a paginated format.")
    @GetMapping
    public ResponseEntity<?> getAllItems(@ParameterObject Pageable pageable) {
        Page<Item> itemsPage = repository.findAll(pageable);

        if (pageable.getPageNumber() >= itemsPage.getTotalPages() && itemsPage.getTotalPages() > 0) {
            return ResponseEntity.badRequest()
                    .body("Invalid page! The total number of available pages is " + itemsPage.getTotalPages() +
                            ". Remember that the first page index is 0.");
        }

        PagedModel<EntityModel<Item>> pagedModel = pagedResourcesAssembler.toModel(itemsPage, this::createEntityModel);
        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Filter by Name",
            description = "Searches for items in the catalog by partial name matches.")
    @GetMapping("/filter/name")
    public ResponseEntity<PagedModel<EntityModel<Item>>> getItemsByName(@RequestParam String name, @ParameterObject Pageable pageable) {
        Page<Item> itemsPage = repository.findByNameContainingIgnoreCase(name, pageable);
        PagedModel<EntityModel<Item>> pagedModel = pagedResourcesAssembler.toModel(itemsPage, this::createEntityModel);
        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Get item by ID",
            description = "Displays detailed specifications (cost, description) of an item by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item found successfully"),
            @ApiResponse(responseCode = "404", description = "Item Record not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Item>> getItemById(@PathVariable Long id) {
        Item item = repository.findById(id).orElseThrow(() -> new ItemNotFoundException(id));
        return ResponseEntity.ok(createEntityModel(item));
    }

    // --- POST COM IDEMPOTÊNCIA ---
    @Operation(summary = "Create a new item",
            description = "Adds a new equipment or consumable to the global database. Independent entity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Item created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or missing Idempotency-Key"),
            @ApiResponse(responseCode = "409", description = "Conflict: Item already exists or Idempotency Key reused with different payload")
    })
    @PostMapping
    public ResponseEntity<?> createItem(
            @Parameter(description = "Required key used to make repeated create requests idempotent", required = true)
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody Item newItem) {

        // 1. Bloqueia ID 0 ou negativo
        if (newItem.getId() != null && newItem.getId() <= 0) {
            return ResponseEntity.badRequest().body("Item ID must be greater than zero.");
        }

        // 2. Valida o Header de Idempotência
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // 3. Cria a "digital" (fingerprint) do Item.
        var requestFingerprint = new CreateItemFingerprint(newItem.getName());

        // 4. Bloqueio de concorrência com objeto dedicado
        synchronized (createItemIdempotencyLock) {
            var storedResponse = createItemResponses.get(idempotencyKey);

            if (storedResponse != null) {
                if (!storedResponse.requestFingerprint().equals(requestFingerprint)) {
                    // Chave repetida com payload diferente
                    return ResponseEntity.status(HttpStatus.CONFLICT).build();
                }

                // Replay da resposta original salva
                return ResponseEntity.created(storedResponse.location())
                        .body(storedResponse.entityModel());
            }

            // CORREÇÃO AQUI: Só verifica se existe no banco caso o usuário tenha enviado um ID!
            if (newItem.getId() != null && repository.existsById(newItem.getId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            // Persistência
            Item savedItem = repository.save(newItem);
            URI location = URI.create("/api/items/" + savedItem.getId());

            EntityModel<Item> entityModel = createEntityModel(savedItem);

            // Grava no cache
            createItemResponses.put(idempotencyKey, new IdempotentCreateResponse(
                    requestFingerprint,
                    entityModel,
                    location
            ));

            return ResponseEntity.created(location).body(entityModel);
        }
    }

    @Operation(summary = "Update or Create an item",
            description = "Updates an item if it exists or creates a new one at the specified ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item Updated successfully"),
            @ApiResponse(responseCode = "201", description = "Item Created successfully (New Resource)"),
            @ApiResponse(responseCode = "400", description = "Invalid ID or request body")
    })
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<Item>> updateItem(@PathVariable Long id, @Valid @RequestBody Item updatedItem) {

        // 1. SEGURANÇA: Bloqueia IDs inválidos
        if (id <= 0) {
            return ResponseEntity.badRequest().build();
        }

        // 2. SINCRONIA: O ID da URL SEMPRE manda no objeto
        updatedItem.setId(id);

        // 3. VERIFICAÇÃO: Checamos se ele já existe antes de salvar
        boolean exists = repository.existsById(id);

        // 4. SALVAMENTO: O save() fará Update (se id existe) ou Insert (se id é novo)
        Item savedItem = repository.save(updatedItem);

        // 5. RESPOSTA COM HATEOAS
        EntityModel<Item> entityModel = createEntityModel(savedItem);

        if (exists) {
            return ResponseEntity.ok(entityModel); // Status 200
        } else {
            return ResponseEntity.status(HttpStatus.CREATED).body(entityModel); // Status 201
        }
    }

    @Operation(summary = "Delete an item",
            description = "Deletes an item from the game's catalog.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Item Deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format"),
            @ApiResponse(responseCode = "404", description = "Item not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        Item item = repository.findById(id).orElseThrow(() -> new ItemNotFoundException(id));
        repository.delete(item);
        return ResponseEntity.noContent().build();
    }

    // Método auxiliar para HATEOAS (evita repetição de código)
    private EntityModel<Item> createEntityModel(Item item) {
        return EntityModel.of(item,
                linkTo(methodOn(ItemController.class).getItemById(item.getId())).withSelfRel(),
                linkTo(methodOn(ItemController.class).getAllItems(Pageable.unpaged())).withRel("items"));
    }

    // --- RECORDS NO PADRÃO DO PROFESSOR ---
    private record CreateItemFingerprint(String name) {}
    private record IdempotentCreateResponse(CreateItemFingerprint requestFingerprint, EntityModel<Item> entityModel, URI location) {}
}