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
import senac.tsi.dota2.exceptions.PlayerNotFoundException;
import senac.tsi.dota2.repositories.PlayerRepository;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Players", description = "Routes to manage Dota 2 Professional Players")
@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerRepository repository;
    private final PagedResourcesAssembler<Player> pagedResourcesAssembler;

    public PlayerController(PlayerRepository repository, PagedResourcesAssembler<Player> pagedResourcesAssembler) {
        this.repository = repository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all players (Paginated)",
            description = "Returns a paginated list of all players along with their REST links.")
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<Player>>> getAllPlayers(@ParameterObject Pageable pageable) {
        Page<Player> playersPage = repository.findAll(pageable);

        PagedModel<EntityModel<Player>> pagedModel = pagedResourcesAssembler.toModel(playersPage, player ->
                EntityModel.of(player,
                        linkTo(methodOn(PlayerController.class).getPlayerById(player.getId())).withSelfRel(),
                        linkTo(methodOn(PlayerController.class).getAllPlayers(pageable)).withRel("players")
                ));

        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Filter by Nickname",
            description = "Filters players by searching for partial matches of their nickname.")
    @GetMapping("/filter/nickname")
    public ResponseEntity<PagedModel<EntityModel<Player>>> getPlayersByNickname(
            @RequestParam String nickname,
            @ParameterObject Pageable pageable) {

        Page<Player> playersPage = repository.findByNicknameContainingIgnoreCase(nickname, pageable);

        PagedModel<EntityModel<Player>> pagedModel = pagedResourcesAssembler.toModel(playersPage, player ->
                EntityModel.of(player,
                        linkTo(methodOn(PlayerController.class).getPlayerById(player.getId())).withSelfRel(),
                        linkTo(methodOn(PlayerController.class).getAllPlayers(pageable)).withRel("players")
                ));

        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Get player by ID",
            description = "Fetches a player by ID. The response JSON includes data from the Team they belong to.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Player found successfully"),
            @ApiResponse(responseCode = "404", description = "Player not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Player>> getPlayerById(@PathVariable Long id) {
        Player player = repository.findById(id)
                .orElseThrow(() -> new PlayerNotFoundException(id));

        EntityModel<Player> entityModel = EntityModel.of(player,
                linkTo(methodOn(PlayerController.class).getPlayerById(id)).withSelfRel(),
                linkTo(methodOn(PlayerController.class).getAllPlayers(Pageable.unpaged())).withRel("players"));

        return ResponseEntity.ok(entityModel);
    }

    @Operation(summary = "Create a new player",
            description = "Registers a new player. A valid existing Team ID is mandatory to establish the link (One-to-Many relationship).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Player created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or validation failed"),
            @ApiResponse(responseCode = "409", description = "Conflict: Player already exists")
    })
    @PostMapping
    public ResponseEntity<EntityModel<Player>> createPlayer(@Valid @RequestBody Player newPlayer) {
        Player savedPlayer = repository.save(newPlayer);

        EntityModel<Player> entityModel = EntityModel.of(savedPlayer,
                linkTo(methodOn(PlayerController.class).getPlayerById(savedPlayer.getId())).withSelfRel(),
                linkTo(methodOn(PlayerController.class).getAllPlayers(Pageable.unpaged())).withRel("players"));

        return ResponseEntity
                .created(URI.create("/api/players/" + savedPlayer.getId()))
                .body(entityModel);
    }

    @Operation(summary = "Update or Create a player",
            description = "Updates an existing player or creates a new one if the ID doesn't exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Player updated successfully"),
            @ApiResponse(responseCode = "201", description = "Player created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ID or request body")
    })
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<Player>> updatePlayer(@PathVariable Long id, @Valid @RequestBody Player updatedPlayer) {

        // 1. SEGURANÇA: Bloqueia IDs inválidos
        if (id <= 0) {
            return ResponseEntity.badRequest().build();
        }

        // 2. SINCRONIA: O ID da URL SEMPRE manda (garante que ele salve no ID 10 e não no 55)
        updatedPlayer.setId(id);

        // 3. VERIFICAÇÃO: Checamos se já existe para decidir o Status Code
        boolean exists = repository.existsById(id);

        // 4. PERSISTÊNCIA: O save faz Update se existir ou Insert se não existir
        // O Hibernate vai cuidar de vincular o Team corretamente pelo objeto updatedPlayer
        Player savedPlayer = repository.save(updatedPlayer);

        // 5. HATEOAS: Gera os links usando o seu método auxiliar
        EntityModel<Player> entityModel = createEntityModel(savedPlayer);

        // 6. RESPOSTA DINÂMICA (O que o professor quer ver)
        if (exists) {
            return ResponseEntity.ok(entityModel); // Status 200
        } else {
            return ResponseEntity.status(HttpStatus.CREATED).body(entityModel); // Status 201
        }
    }

    private EntityModel<Player> createEntityModel(Player player) {
        return EntityModel.of(player,
                linkTo(methodOn(PlayerController.class).getPlayerById(player.getId())).withSelfRel(),
                linkTo(methodOn(PlayerController.class).getAllPlayers(Pageable.unpaged())).withRel("players"));
    }

    @Operation(summary = "Delete a player",
            description = "Removes the player record from the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Player deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format"),
            @ApiResponse(responseCode = "404", description = "Player not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer(@PathVariable Long id) {
        Player player = repository.findById(id).orElseThrow(() -> new PlayerNotFoundException(id));
        repository.delete(player);
        return ResponseEntity.noContent().build();
    }
}