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
import senac.tsi.dota2.entities.Player;
import senac.tsi.dota2.entities.PlayerProfile;
import senac.tsi.dota2.exceptions.PlayerProfileNotFoundException;
import senac.tsi.dota2.repositories.PlayerProfileRepository;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Player Profiles", description = "Routes to manage Dota 2 Player Profiles (One-to-One)")
@RestController
@RequestMapping("/api/player-profiles")
public class PlayerProfileController {

    private final PlayerProfileRepository repository;
    private final PagedResourcesAssembler<PlayerProfile> pagedResourcesAssembler;

    // --- VARIÁVEIS DE IDEMPOTÊNCIA ---
    private final Map<String, IdempotentCreateResponse> createProfileResponses = new ConcurrentHashMap<>();
    private final Object createProfileIdempotencyLock = new Object();

    public PlayerProfileController(PlayerProfileRepository repository, PagedResourcesAssembler<PlayerProfile> pagedResourcesAssembler) {
        this.repository = repository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all profiles (Paginated)",
            description = "Lists the detailed profiles of all players, with pagination support.")
    @GetMapping
    public ResponseEntity<?> getAllProfiles(@ParameterObject Pageable pageable) {
        Page<PlayerProfile> profilesPage = repository.findAll(pageable);

        if (pageable.getPageNumber() >= profilesPage.getTotalPages() && profilesPage.getTotalPages() > 0) {
            return ResponseEntity.badRequest()
                    .body("Invalid page! The total number of available pages is " + profilesPage.getTotalPages() +
                            ". Remember that the first page index is 0.");
        }

        PagedModel<EntityModel<PlayerProfile>> pagedModel = pagedResourcesAssembler.toModel(profilesPage, profile ->
                EntityModel.of(profile,
                        linkTo(methodOn(PlayerProfileController.class).getProfileById(profile.getId())).withSelfRel(),
                        linkTo(methodOn(PlayerProfileController.class).getAllProfiles(pageable)).withRel("player-profiles")
                ));

        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Filter by Twitter",
            description = "Locates profiles using the Twitter handle ('@player').")
    @GetMapping("/filter/twitter")
    public ResponseEntity<PagedModel<EntityModel<PlayerProfile>>> getProfilesByTwitter(
            @RequestParam String twitterHandle,
            @ParameterObject Pageable pageable) {

        Page<PlayerProfile> profilesPage = repository.findByTwitterHandleContainingIgnoreCase(twitterHandle, pageable);

        PagedModel<EntityModel<PlayerProfile>> pagedModel = pagedResourcesAssembler.toModel(profilesPage, profile ->
                EntityModel.of(profile,
                        linkTo(methodOn(PlayerProfileController.class).getProfileById(profile.getId())).withSelfRel(),
                        linkTo(methodOn(PlayerProfileController.class).getAllProfiles(pageable)).withRel("player-profiles")
                ));

        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Get profile by ID",
            description = "Retrieves the biography, earnings, and social media links of a specific profile.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile found successfully"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<PlayerProfile>> getProfileById(@PathVariable Long id) {
        PlayerProfile profile = repository.findById(id)
                .orElseThrow(() -> new PlayerProfileNotFoundException(id));

        EntityModel<PlayerProfile> entityModel = EntityModel.of(profile,
                linkTo(methodOn(PlayerProfileController.class).getProfileById(id)).withSelfRel(),
                linkTo(methodOn(PlayerProfileController.class).getAllProfiles(Pageable.unpaged())).withRel("player-profiles"));

        return ResponseEntity.ok(entityModel);
    }

    // --- POST COM IDEMPOTÊNCIA ---
    @Operation(summary = "Create a new profile",
            description = "Creates a detailed profile. This profile must be tied to an existing Player, and each player can only have one profile (One-to-One relationship).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Profile created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data, validation failed, or missing Idempotency-Key"),
            @ApiResponse(responseCode = "409", description = "Conflict: Profile already exists or Idempotency Key reused with different payload")
    })
    @PostMapping
    public ResponseEntity<?> createProfile(
            @Parameter(description = "Required key used to make repeated create requests idempotent", required = true)
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PlayerProfile newProfile) {

        // 1. Bloqueia ID 0 ou negativo (caso seja manual)
        if (newProfile.getId() != null && newProfile.getId() <= 0) {
            return ResponseEntity.badRequest().body("Profile ID must be greater than zero.");
        }

        // 2. Valida Header de Idempotência
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // 3. Digital (Fingerprint). Usamos o ID do Player vinculado e a Biografia
        Long playerId = newProfile.getPlayer() != null ? newProfile.getPlayer().getId() : null;
        var requestFingerprint = new CreateProfileFingerprint(playerId, newProfile.getBiography());

        // 4. Bloqueio Sincronizado
        synchronized (createProfileIdempotencyLock) {
            var storedResponse = createProfileResponses.get(idempotencyKey);

            if (storedResponse != null) {
                if (!storedResponse.requestFingerprint().equals(requestFingerprint)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).build();
                }

                // Replay da resposta
                return ResponseEntity.created(storedResponse.location())
                        .body(storedResponse.entityModel());
            }

            // Impede sobrescrever um Profile que já existe
            if (newProfile.getId() != null && repository.existsById(newProfile.getId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            // Persistência
            PlayerProfile savedProfile = repository.save(newProfile);
            URI location = URI.create("/api/player-profiles/" + savedProfile.getId());

            EntityModel<PlayerProfile> entityModel = createEntityModel(savedProfile);

            // Grava no Cache
            createProfileResponses.put(idempotencyKey, new IdempotentCreateResponse(
                    requestFingerprint,
                    entityModel,
                    location
            ));

            return ResponseEntity.created(location).body(entityModel);
        }
    }

    @Operation(summary = "Update or Create a profile",
            description = "Updates an existing profile or creates a new one at the specified ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "201", description = "Profile created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ID or request body")
    })
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<PlayerProfile>> updateProfile(@PathVariable Long id, @Valid @RequestBody PlayerProfile updatedProfile) {

        // 1. SEGURANÇA: Bloqueia IDs inválidos
        if (id <= 0) {
            return ResponseEntity.badRequest().build();
        }

        // 2. SINCRONIA: O ID da URL manda. Se o cara mandou 300, vai salvar no 300.
        updatedProfile.setId(id);

        // 3. VERIFICAÇÃO: Checamos a existência antes de salvar
        boolean exists = repository.existsById(id);

        // 4. PERSISTÊNCIA: O save faz o trabalho pesado (Update ou Insert)
        // O Hibernate já vai lidar com o vínculo do Player se ele vier no JSON
        PlayerProfile savedProfile = repository.save(updatedProfile);

        // 5. HATEOAS: Gera os links usando o seu método auxiliar
        EntityModel<PlayerProfile> entityModel = createEntityModel(savedProfile);

        // 6. RESPOSTA DINÂMICA (Padrão de aula)
        if (exists) {
            return ResponseEntity.ok(entityModel); // Retorna 200
        } else {
            return ResponseEntity.status(HttpStatus.CREATED).body(entityModel); // Retorna 201
        }
    }

    // Método auxiliar para os links HATEOAS do Perfil
    private EntityModel<PlayerProfile> createEntityModel(PlayerProfile profile) {
        return EntityModel.of(profile,
                linkTo(methodOn(PlayerProfileController.class).getProfileById(profile.getId())).withSelfRel(),
                linkTo(methodOn(PlayerProfileController.class).getAllProfiles(Pageable.unpaged())).withRel("profiles"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable Long id) {
        // 1. Busca o perfil ou lança a exceção que você já criou
        PlayerProfile profile = repository.findById(id)
                .orElseThrow(() -> new PlayerProfileNotFoundException(id));

        // 2. BUSCA O JOGADOR: Pegamos o jogador vinculado a este perfil
        Player player = profile.getPlayer();

        // 3. QUEBRA O VÍNCULO: Avisamos ao objeto Player que o perfil dele agora é NULL
        if (player != null) {
            player.setProfile(null);
        }

        // 4. DELETA: Agora sim, removemos do banco de dados
        repository.delete(profile);

        // 5. Retorna 204 No Content
        return ResponseEntity.noContent().build();
    }

    // --- RECORDS NO PADRÃO DO PROFESSOR ---
    private record CreateProfileFingerprint(Long playerId, String biography) {}
    private record IdempotentCreateResponse(CreateProfileFingerprint requestFingerprint, EntityModel<PlayerProfile> entityModel, URI location) {}
}