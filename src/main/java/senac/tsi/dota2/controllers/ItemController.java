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

@Tag(name = "Items", description = "Routes to manage Dota 2 In-Game Items (Many-to-Many)")
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemRepository repository;
    private final PagedResourcesAssembler<Item> pagedResourcesAssembler;

    public ItemController(ItemRepository repository, PagedResourcesAssembler<Item> pagedResourcesAssembler) {
        this.repository = repository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all items (Paginated)", description = "Returns the complete list of items with HATEOAS links.")
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<Item>>> getAllItems(@ParameterObject Pageable pageable) {
        Page<Item> itemsPage = repository.findAll(pageable);

        PagedModel<EntityModel<Item>> pagedModel = pagedResourcesAssembler.toModel(itemsPage, item ->
                EntityModel.of(item,
                        linkTo(methodOn(ItemController.class).getItemById(item.getId())).withSelfRel(),
                        linkTo(methodOn(ItemController.class).getAllItems(pageable)).withRel("items")
                ));

        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Filter by Name", description = "Finds items containing the specified name with pagination.")
    @GetMapping("/filter/name")
    public ResponseEntity<PagedModel<EntityModel<Item>>> getItemsByName(
            @RequestParam String name,
            @ParameterObject Pageable pageable) {

        Page<Item> itemsPage = repository.findByNameContainingIgnoreCase(name, pageable);

        PagedModel<EntityModel<Item>> pagedModel = pagedResourcesAssembler.toModel(itemsPage, item ->
                EntityModel.of(item,
                        linkTo(methodOn(ItemController.class).getItemById(item.getId())).withSelfRel(),
                        linkTo(methodOn(ItemController.class).getAllItems(pageable)).withRel("items")
                ));

        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Get item by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item found successfully"),
            @ApiResponse(responseCode = "404", description = "Item not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Item>> getItemById(@PathVariable Long id) {
        Item item = repository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));

        EntityModel<Item> entityModel = EntityModel.of(item,
                linkTo(methodOn(ItemController.class).getItemById(id)).withSelfRel(),
                linkTo(methodOn(ItemController.class).getAllItems(Pageable.unpaged())).withRel("items"));

        return ResponseEntity.ok(entityModel);
    }

    @Operation(summary = "Create a new item")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Item created successfully")
    })
    @PostMapping
    public ResponseEntity<EntityModel<Item>> createItem(@Valid @RequestBody Item newItem) {
        Item savedItem = repository.save(newItem);

        EntityModel<Item> entityModel = EntityModel.of(savedItem,
                linkTo(methodOn(ItemController.class).getItemById(savedItem.getId())).withSelfRel(),
                linkTo(methodOn(ItemController.class).getAllItems(Pageable.unpaged())).withRel("items"));

        return ResponseEntity
                .created(URI.create("/api/items/" + savedItem.getId()))
                .body(entityModel);
    }

    @Operation(summary = "Update an item")
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<Item>> updateItem(@PathVariable Long id, @Valid @RequestBody Item updatedItem) {
        return repository.findById(id)
                .map(item -> {
                    item.setName(updatedItem.getName());
                    item.setCost(updatedItem.getCost());
                    item.setDescription(updatedItem.getDescription());
                    Item savedItem = repository.save(item);

                    EntityModel<Item> entityModel = EntityModel.of(savedItem,
                            linkTo(methodOn(ItemController.class).getItemById(savedItem.getId())).withSelfRel(),
                            linkTo(methodOn(ItemController.class).getAllItems(Pageable.unpaged())).withRel("items"));

                    return ResponseEntity.ok(entityModel);
                })
                .orElseGet(() -> {
                    updatedItem.setId(id);
                    Item savedItem = repository.save(updatedItem);

                    EntityModel<Item> entityModel = EntityModel.of(savedItem,
                            linkTo(methodOn(ItemController.class).getItemById(savedItem.getId())).withSelfRel(),
                            linkTo(methodOn(ItemController.class).getAllItems(Pageable.unpaged())).withRel("items"));

                    return ResponseEntity
                            .created(URI.create("/api/items/" + id))
                            .body(entityModel);
                });
    }

    @Operation(summary = "Delete an item")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Item deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Item not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        Item item = repository.findById(id).orElseThrow(() -> new ItemNotFoundException(id));
        repository.delete(item);
        return ResponseEntity.noContent().build();
    }
}