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
import senac.tsi.dota2.entities.Hero;
import senac.tsi.dota2.exceptions.HeroNotFoundException;
import senac.tsi.dota2.repositories.HeroRepository;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Heroes", description = "Routes to manage Dota 2 Heroes")
@RestController
@RequestMapping("/api/heroes")
public class HeroController {

    private final HeroRepository repository;
    private final PagedResourcesAssembler<Hero> pagedResourcesAssembler;

    // --- VARIÁVEIS DE IDEMPOTÊNCIA (No padrão do professor) ---
    private final Map<String, IdempotentCreateResponse> createHeroResponses = new ConcurrentHashMap<>();
    private final Object createHeroIdempotencyLock = new Object();

    public HeroController(HeroRepository repository, PagedResourcesAssembler<Hero> pagedResourcesAssembler) {
        this.repository = repository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all heroes (Paginated)",
            description = "Returns a paginated list of all registered heroes, including HATEOAS navigation links.")
    @GetMapping
    public ResponseEntity<?> getAllHeroes(@ParameterObject Pageable pageable) {
        Page<Hero> heroesPage = repository.findAll(pageable);

        if (pageable.getPageNumber() >= heroesPage.getTotalPages() && heroesPage.getTotalPages() > 0) {
            return ResponseEntity.badRequest()
                    .body("Invalid page! The total number of available pages is " + heroesPage.getTotalPages() +
                            ". Remember that the first page index is 0.");
        }

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
            @ApiResponse(responseCode = "400", description = "Invalid request data or missing Idempotency-Key"),
            @ApiResponse(responseCode = "409", description = "Conflict: Hero ID already exists or Idempotency Key reused")
    })
    @PostMapping
    public ResponseEntity<?> createHero(
            @Parameter(description = "Required key used to make repeated create requests idempotent", required = true)
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody Hero newHero) {

        // Impede id 0 ou negativo
        if (newHero.getId() != null && newHero.getId() <= 0) {
            return ResponseEntity.badRequest().body("Hero ID must be greater than zero.");
        }

        // Validação da Chave
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().build(); // Retorna só 400, sem body
        }

        var requestFingerprint = new CreateHeroFingerprint(newHero.getLocalizedName(), newHero.getAttackType());

        // Lock dedicado
        synchronized (createHeroIdempotencyLock) {
            var storedResponse = createHeroResponses.get(idempotencyKey);

            if (storedResponse != null) {
                if (!storedResponse.requestFingerprint().equals(requestFingerprint)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).build(); // Retorna só 409, sem body
                }

                // Replay da resposta original (201 com o mesmo cabeçalho Location)
                return ResponseEntity.created(storedResponse.location())
                        .body(storedResponse.entityModel());
            }

            // Impede sobrescrever o herói (Sua trava original)
            if (repository.existsById(newHero.getId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            // Persiste no banco
            var savedHero = repository.save(newHero);
            URI location = URI.create("/api/heroes/" + savedHero.getId());

            EntityModel<Hero> entityModel = EntityModel.of(savedHero,
                    linkTo(methodOn(HeroController.class).getHeroById(savedHero.getId())).withSelfRel(),
                    linkTo(methodOn(HeroController.class).getAllHeroes(Pageable.unpaged())).withRel("heroes"));

            // Guarda tudo no cache para um possível replay
            createHeroResponses.put(idempotencyKey, new IdempotentCreateResponse(
                    requestFingerprint,
                    entityModel,
                    location
            ));

            return ResponseEntity.created(location).body(entityModel);
        }
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
        if (id <= 0) {
            return ResponseEntity.badRequest().build();
        }
        updatedHero.setId(id);
        boolean exists = repository.existsById(id);
        Hero savedHero = repository.save(updatedHero);

        EntityModel<Hero> entityModel = EntityModel.of(savedHero,
                linkTo(methodOn(HeroController.class).getHeroById(id)).withSelfRel(),
                linkTo(methodOn(HeroController.class).getAllHeroes(Pageable.unpaged())).withRel("heroes"));

        if (exists) {
            return ResponseEntity.ok(entityModel);
        } else {
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

    // --- RECORDS NO PADRÃO DO PROFESSOR ---
    private record CreateHeroFingerprint(String name, Hero.AttackType attackType) {}
    private record IdempotentCreateResponse(CreateHeroFingerprint requestFingerprint, EntityModel<Hero> entityModel, URI location) {}
}