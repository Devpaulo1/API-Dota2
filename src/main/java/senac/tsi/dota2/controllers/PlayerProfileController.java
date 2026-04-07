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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.tsi.dota2.entities.Player;
import senac.tsi.dota2.entities.PlayerProfile;
import senac.tsi.dota2.exceptions.PlayerProfileNotFoundException;
import senac.tsi.dota2.repositories.PlayerProfileRepository;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Player Profiles", description = "Routes to manage Dota 2 Player Profiles (One-to-One)")
@RestController
@RequestMapping("/api/player-profiles")
public class PlayerProfileController {

    private final PlayerProfileRepository repository;
    private final PagedResourcesAssembler<PlayerProfile> pagedResourcesAssembler;

    public PlayerProfileController(PlayerProfileRepository repository, PagedResourcesAssembler<PlayerProfile> pagedResourcesAssembler) {
        this.repository = repository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all profiles (Paginated)",
            description = "Lists the detailed profiles of all players, with pagination support.")
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<PlayerProfile>>> getAllProfiles(@ParameterObject Pageable pageable) {
        Page<PlayerProfile> profilesPage = repository.findAll(pageable);

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

    @Operation(summary = "Create a new profile",
            description = "Creates a detailed profile. This profile must be tied to an existing Player, and each player can only have one profile (One-to-One relationship).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Profile created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or validation failed"),
            @ApiResponse(responseCode = "409", description = "Conflict: Profile already exists")
    })
    @PostMapping
    public ResponseEntity<EntityModel<PlayerProfile>> createProfile(@Valid @RequestBody PlayerProfile newProfile) {
        PlayerProfile savedProfile = repository.save(newProfile);

        EntityModel<PlayerProfile> entityModel = EntityModel.of(savedProfile,
                linkTo(methodOn(PlayerProfileController.class).getProfileById(savedProfile.getId())).withSelfRel(),
                linkTo(methodOn(PlayerProfileController.class).getAllProfiles(Pageable.unpaged())).withRel("player-profiles"));

        return ResponseEntity
                .created(URI.create("/api/player-profiles/" + savedProfile.getId()))
                .body(entityModel);
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
}