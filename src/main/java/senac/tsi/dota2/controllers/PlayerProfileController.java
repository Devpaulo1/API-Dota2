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

    @Operation(summary = "Get all profiles (Paginated)", description = "Returns the complete list of player profiles with HATEOAS links.")
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

    @Operation(summary = "Filter by Twitter", description = "Finds profiles containing the specified Twitter handle with pagination.")
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

    @Operation(summary = "Get profile by ID")
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

    @Operation(summary = "Create a new profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Profile created successfully")
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

    @Operation(summary = "Update a profile")
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<PlayerProfile>> updateProfile(@PathVariable Long id, @Valid @RequestBody PlayerProfile updatedProfile) {
        return repository.findById(id)
                .map(profile -> {
                    profile.setBiography(updatedProfile.getBiography());
                    profile.setTwitterHandle(updatedProfile.getTwitterHandle());
                    profile.setTotalEarnings(updatedProfile.getTotalEarnings());
                    profile.setPlayer(updatedProfile.getPlayer()); // Atualiza o vínculo com o jogador
                    PlayerProfile savedProfile = repository.save(profile);

                    EntityModel<PlayerProfile> entityModel = EntityModel.of(savedProfile,
                            linkTo(methodOn(PlayerProfileController.class).getProfileById(savedProfile.getId())).withSelfRel(),
                            linkTo(methodOn(PlayerProfileController.class).getAllProfiles(Pageable.unpaged())).withRel("player-profiles"));

                    return ResponseEntity.ok(entityModel);
                })
                .orElseGet(() -> {
                    updatedProfile.setId(id);
                    PlayerProfile savedProfile = repository.save(updatedProfile);

                    EntityModel<PlayerProfile> entityModel = EntityModel.of(savedProfile,
                            linkTo(methodOn(PlayerProfileController.class).getProfileById(savedProfile.getId())).withSelfRel(),
                            linkTo(methodOn(PlayerProfileController.class).getAllProfiles(Pageable.unpaged())).withRel("player-profiles"));

                    return ResponseEntity
                            .created(URI.create("/api/player-profiles/" + id))
                            .body(entityModel);
                });
    }

    @Operation(summary = "Delete a profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Profile deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable Long id) {
        PlayerProfile profile = repository.findById(id).orElseThrow(() -> new PlayerProfileNotFoundException(id));
        repository.delete(profile);
        return ResponseEntity.noContent().build();
    }
}