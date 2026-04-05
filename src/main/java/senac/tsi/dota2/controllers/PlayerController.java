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

    @Operation(summary = "Get all players (Paginated)", description = "Returns the complete list of players with HATEOAS links and pagination.")
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

    @Operation(summary = "Filter by Nickname", description = "Finds players containing the specified nickname (case-insensitive) with pagination.")
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

    @Operation(summary = "Get player by ID")
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

    @Operation(summary = "Create a new player")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Player created successfully")
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

    @Operation(summary = "Update a player (Upsert)")
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<Player>> updatePlayer(@PathVariable Long id, @Valid @RequestBody Player updatedPlayer) {
        return repository.findById(id)
                .map(player -> {
                    // 1. SE EXISTIR: Atualiza os dados (200 OK)
                    player.setNickname(updatedPlayer.getNickname());
                    player.setRealName(updatedPlayer.getRealName());

                    // Atualiza o time se for enviado no JSON
                    if (updatedPlayer.getTeam() != null) {
                        player.setTeam(updatedPlayer.getTeam());
                    }

                    Player savedPlayer = repository.save(player);
                    return ResponseEntity.ok(createEntityModel(savedPlayer));
                })
                .orElseGet(() -> {
                    // 2. SE NÃO EXISTIR: Cria um novo (201 Created)
                    // Limpamos o ID para o Hibernate usar a sequência IDENTITY
                    updatedPlayer.setId(null);
                    Player savedPlayer = repository.save(updatedPlayer);

                    return ResponseEntity
                            .created(URI.create("/api/players/" + savedPlayer.getId()))
                            .body(createEntityModel(savedPlayer));
                });
    }

    // Certifique-se de ter o método auxiliar de links (HATEOAS)
    private EntityModel<Player> createEntityModel(Player player) {
        return EntityModel.of(player,
                linkTo(methodOn(PlayerController.class).getPlayerById(player.getId())).withSelfRel(),
                linkTo(methodOn(PlayerController.class).getAllPlayers(Pageable.unpaged())).withRel("players"));
    }

    @Operation(summary = "Delete a player")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Player deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Player not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer(@PathVariable Long id) {
        Player player = repository.findById(id).orElseThrow(() -> new PlayerNotFoundException(id));
        repository.delete(player);
        return ResponseEntity.noContent().build();
    }
}