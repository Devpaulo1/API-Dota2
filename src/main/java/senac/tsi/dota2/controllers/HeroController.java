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
import senac.tsi.dota2.entities.Hero;
import senac.tsi.dota2.exceptions.HeroNotFoundException;
import senac.tsi.dota2.repositories.HeroRepository;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Heroes", description = "Routes to manage Dota 2 Heroes")
@RestController
@RequestMapping("/api/heroes")
public class HeroController {

    private final HeroRepository repository;
    private final PagedResourcesAssembler<Hero> pagedResourcesAssembler;

    public HeroController(HeroRepository repository, PagedResourcesAssembler<Hero> pagedResourcesAssembler) {
        this.repository = repository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all heroes (Paginated)", description = "Returns the complete list of heroes with HATEOAS links and pagination.")
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<Hero>>> getAllHeroes(@ParameterObject Pageable pageable) {
        Page<Hero> heroesPage = repository.findAll(pageable);

        PagedModel<EntityModel<Hero>> pagedModel = pagedResourcesAssembler.toModel(heroesPage, hero ->
                EntityModel.of(hero,
                        linkTo(methodOn(HeroController.class).getHeroById(hero.getId())).withSelfRel(),
                        linkTo(methodOn(HeroController.class).getAllHeroes(pageable)).withRel("heroes")
                ));

        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Filter by Attack Type", description = "Finds heroes by attack type (Melee/Ranged) with pagination.")
    @GetMapping("/filter/attack-type")
    public ResponseEntity<PagedModel<EntityModel<Hero>>> getHeroesByAttackType(
            @RequestParam Hero.AttackType type,
            @ParameterObject Pageable pageable) {

        Page<Hero> heroesPage = repository.findByAttackType(type, pageable);

        PagedModel<EntityModel<Hero>> pagedModel = pagedResourcesAssembler.toModel(heroesPage, hero ->
                EntityModel.of(hero,
                        linkTo(methodOn(HeroController.class).getHeroById(hero.getId())).withSelfRel(),
                        linkTo(methodOn(HeroController.class).getAllHeroes(pageable)).withRel("heroes")
                ));

        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Get hero by ID", description = "Finds a specific hero. Returns 404 Not Found if the ID does not exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hero found successfully"),
            @ApiResponse(responseCode = "404", description = "Hero not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Hero>> getHeroById(@PathVariable Long id) {
        Hero hero = repository.findById(id)
                .orElseThrow(() -> new HeroNotFoundException(id));

        EntityModel<Hero> entityModel = EntityModel.of(hero,
                linkTo(methodOn(HeroController.class).getHeroById(id)).withSelfRel(),
                linkTo(methodOn(HeroController.class).getAllHeroes(Pageable.unpaged())).withRel("heroes"));

        return ResponseEntity.ok(entityModel);
    }

    @Operation(summary = "Create a new hero", description = "Adds a custom hero to the database.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Hero created successfully")
    })
    @PostMapping
    public ResponseEntity<EntityModel<Hero>> createHero(@Valid @RequestBody Hero newHero) {
        Hero savedHero = repository.save(newHero);

        EntityModel<Hero> entityModel = EntityModel.of(savedHero,
                linkTo(methodOn(HeroController.class).getHeroById(savedHero.getId())).withSelfRel(),
                linkTo(methodOn(HeroController.class).getAllHeroes(Pageable.unpaged())).withRel("heroes"));

        return ResponseEntity
                .created(URI.create("/api/heroes/" + savedHero.getId()))
                .body(entityModel);
    }

    @Operation(summary = "Update a hero", description = "Updates an existing hero's data. If the ID does not exist, it creates a new one.")
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<Hero>> updateHero(@PathVariable Long id, @Valid @RequestBody Hero updatedHero) {
        return repository.findById(id)
                .map(hero -> {
                    hero.setLocalizedName(updatedHero.getLocalizedName());
                    hero.setAttackType(updatedHero.getAttackType());
                    Hero savedHero = repository.save(hero);

                    EntityModel<Hero> entityModel = EntityModel.of(savedHero,
                            linkTo(methodOn(HeroController.class).getHeroById(savedHero.getId())).withSelfRel(),
                            linkTo(methodOn(HeroController.class).getAllHeroes(Pageable.unpaged())).withRel("heroes"));

                    return ResponseEntity.ok(entityModel);
                })
                .orElseGet(() -> {
                    updatedHero.setId(id);
                    Hero savedHero = repository.save(updatedHero);

                    EntityModel<Hero> entityModel = EntityModel.of(savedHero,
                            linkTo(methodOn(HeroController.class).getHeroById(savedHero.getId())).withSelfRel(),
                            linkTo(methodOn(HeroController.class).getAllHeroes(Pageable.unpaged())).withRel("heroes"));

                    return ResponseEntity
                            .created(URI.create("/api/heroes/" + id))
                            .body(entityModel);
                });
    }

    @Operation(summary = "Delete a hero", description = "Removes a hero from the local database by ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Hero deleted successfully (No content returned)"),
            @ApiResponse(responseCode = "404", description = "Hero not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHero(@PathVariable Long id) {
        Hero hero = repository.findById(id).orElseThrow(() -> new HeroNotFoundException(id));
        repository.delete(hero);
        return ResponseEntity.noContent().build();
    }
}