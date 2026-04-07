package senac.tsi.dota2.controllers;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.tsi.dota2.entities.Item;
import senac.tsi.dota2.exceptions.ItemNotFoundException;
import senac.tsi.dota2.repositories.ItemRepository;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Items",
        description = "Routes to manage Dota 2 In-Game Items")
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemRepository repository;
    private final PagedResourcesAssembler<Item> pagedResourcesAssembler;

    public ItemController(ItemRepository repository, PagedResourcesAssembler<Item> pagedResourcesAssembler) {
        this.repository = repository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all items (Paginated)",
            description = "Accesses the global catalog of available items and equipment in a paginated format.")
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<Item>>> getAllItems(@ParameterObject Pageable pageable) {
        Page<Item> itemsPage = repository.findAll(pageable);
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
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Item>> getItemById(@PathVariable Long id) {
        Item item = repository.findById(id).orElseThrow(() -> new ItemNotFoundException(id));
        return ResponseEntity.ok(createEntityModel(item));
    }

    @Operation(summary = "Create a new item",
            description = "Adds a new equipment or consumable to the global database. Independent entity.")
    @PostMapping
    public ResponseEntity<EntityModel<Item>> createItem(@Valid @RequestBody Item newItem) {
        Item savedItem = repository.save(newItem);
        return ResponseEntity
                .created(URI.create("/api/items/" + savedItem.getId()))
                .body(createEntityModel(savedItem));
    }

    @Operation(summary = "Update an item",
            description = "Updates the name, cost, or effects of an existing item.")
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<Item>> updateItem(@PathVariable Long id, @Valid @RequestBody Item updatedItem) {
        return repository.findById(id)
                .map(item -> {
                    // 1. SE EXISTIR: Atualiza os dados (Retorna 200 OK)
                    item.setName(updatedItem.getName());
                    item.setCost(updatedItem.getCost());
                    item.setDescription(updatedItem.getDescription());
                    Item savedItem = repository.save(item);
                    return ResponseEntity.ok(createEntityModel(savedItem));
                })
                .orElseGet(() -> {
                    // 2. SE NÃO EXISTIR: Cria um novo (Retorna 201 Created)
                    // Importante: limpamos o ID para o banco IDENTITY gerar o próximo corretamente
                    updatedItem.setId(null);
                    Item savedItem = repository.save(updatedItem);
                    return ResponseEntity
                            .created(URI.create("/api/items/" + savedItem.getId()))
                            .body(createEntityModel(savedItem));
                });
    }

    @Operation(summary = "Delete an item",
            description = "Deletes an item from the game's catalog.")
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
}