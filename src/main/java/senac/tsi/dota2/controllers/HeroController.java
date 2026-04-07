package senac.tsi.dota2.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
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

    @Operation(summary = "Get all heroes (Paginated)",
            description = "Returns a paginated list of all registered heroes, including HATEOAS navigation links.")
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

    @Operation(summary = "Filter by Attack Type",
            description = "Filters the list of heroes by attack type ('Melee' or 'Ranged'). Returns paginated results.")
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

    @Operation(summary = "Get hero by ID",
            description = "Retrieves the details of a specific hero by their unique ID. Returns 404 if not found.")
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

    @Operation(summary = "Create a new hero",
            description = "Manually registers a new hero into the database. ID must be greater than zero and unique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Hero Created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data: ID must be > 0"),
            @ApiResponse(responseCode = "409", description = "Conflict: Hero ID already exists")
    })
    @PostMapping
    public ResponseEntity<?> createHero(@Valid @RequestBody Hero newHero) {

        // TRAVA 1: Impedir IDs 0 ou negativos (o herói -1 que você criou)
        if (newHero.getId() != null && newHero.getId() <= 0) {
            return ResponseEntity.badRequest().body("Hero ID must be greater than zero.");
        }

        // TRAVA 2: Impedir sobrescrever o herói (o erro de apagar o Herói ID 1)
        if (repository.existsById(newHero.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Conflict: Hero with ID " + newHero.getId() + " already exists. Use PUT to update.");
        }

        // Se passou pelas travas, salvamos normalmente
        Hero savedHero = repository.save(newHero);

        EntityModel<Hero> entityModel = EntityModel.of(savedHero,
                linkTo(methodOn(HeroController.class).getHeroById(savedHero.getId())).withSelfRel(),
                linkTo(methodOn(HeroController.class).getAllHeroes(Pageable.unpaged())).withRel("heroes"));

        return ResponseEntity
                .created(URI.create("/api/heroes/" + savedHero.getId()))
                .body(entityModel);
    }

    @Operation(summary = "Update or Create a hero",
            description = "Updates an existing hero or creates a new one if the ID does not exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hero updated successfully"),
            @ApiResponse(responseCode = "201", description = "Hero created successfully (Save or Update)"),
            @ApiResponse(responseCode = "400", description = "Invalid ID or request data")
    })
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<Hero>> updateHero(@PathVariable Long id, @Valid @RequestBody Hero updatedHero) {

        // 1. SEGURANÇA: Impede IDs 0 ou negativos
        if (id <= 0) {
            return ResponseEntity.badRequest().build();
        }

        // 2. SINCRONIA: O ID da URL sempre manda (esmaga o 999 do body)
        updatedHero.setId(id);

        // 3. REGRA DO PROFESSOR: Checa se existe para decidir o status final
        boolean exists = repository.existsById(id);

        // 4. SALVAMENTO: O 'save' faz Update se existe ou Insert se não existe
        Hero savedHero = repository.save(updatedHero);

        // 5. HATEOAS: Gera os links obrigatórios do Nível 3
        EntityModel<Hero> entityModel = EntityModel.of(savedHero,
                linkTo(methodOn(HeroController.class).getHeroById(id)).withSelfRel(),
                linkTo(methodOn(HeroController.class).getAllHeroes(Pageable.unpaged())).withRel("heroes"));

        // 6. RESPOSTA DINÂMICA:
        if (exists) {
            // Se já existia, status 200 OK
            return ResponseEntity.ok(entityModel);
        } else {
            // Se não existia e foi criado agora, status 201 Created
            return ResponseEntity.status(HttpStatus.CREATED).body(entityModel);
        }
    }
    @Operation(summary = "Delete a hero",
            description = "Permanently removes a hero from the system. Returns 204 (No Content) upon successful deletion.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format"),
            @ApiResponse(responseCode = "404", description = "Hero not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHero(@PathVariable Long id) {
        Hero hero = repository.findById(id).orElseThrow(() -> new HeroNotFoundException(id));
        repository.delete(hero);
        return ResponseEntity.noContent().build();
    }
}