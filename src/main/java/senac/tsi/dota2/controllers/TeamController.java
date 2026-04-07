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
import senac.tsi.dota2.entities.Team;
import senac.tsi.dota2.exceptions.TeamNotFoundException;
import senac.tsi.dota2.repositories.TeamRepository;

import java.net.URI;

// Importações estáticas mágicas do HATEOAS para criar os links dinamicamente
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Teams", description = "Routes to manage Dota 2 Professional Teams")
@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamRepository repository;
    private final PagedResourcesAssembler<Team> pagedResourcesAssembler;

    public TeamController(TeamRepository repository, PagedResourcesAssembler<Team> pagedResourcesAssembler) {
        this.repository = repository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all teams (Paginated)",
            description = "Lists all registered competitive teams, using pagination and HATEOAS.")
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<Team>>> getAllTeams(@ParameterObject Pageable pageable) {
        Page<Team> teamsPage = repository.findAll(pageable);

        PagedModel<EntityModel<Team>> pagedModel = pagedResourcesAssembler.toModel(teamsPage, team ->
                EntityModel.of(team,
                        linkTo(methodOn(TeamController.class).getTeamById(team.getId())).withSelfRel(),
                        linkTo(methodOn(TeamController.class).getAllTeams(pageable)).withRel("teams")
                ));

        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Filter by Name",
            description = "Performs a dynamic search for teams by part of their name, ignoring case sensitivity.")
    @GetMapping("/filter/name")
    public ResponseEntity<PagedModel<EntityModel<Team>>> getTeamsByName(
            @RequestParam String name,
            @ParameterObject Pageable pageable) {

        // Chama o método que criamos no Repository
        Page<Team> teamsPage = repository.findByNameContainingIgnoreCase(name, pageable);

        PagedModel<EntityModel<Team>> pagedModel = pagedResourcesAssembler.toModel(teamsPage, team ->
                EntityModel.of(team,
                        linkTo(methodOn(TeamController.class).getTeamById(team.getId())).withSelfRel(),
                        linkTo(methodOn(TeamController.class).getAllTeams(pageable)).withRel("teams")
                ));

        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Get team by ID",
            description = "Locates a team by its official ID. Returns the team data and its self-rel link.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Team found successfully"),
            @ApiResponse(responseCode = "404", description = "Team not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Team>> getTeamById(@PathVariable("id") Long id) {
        Team team = repository.findById(id)
                .orElseThrow(() -> new TeamNotFoundException(id));

        EntityModel<Team> entityModel = EntityModel.of(team,
                linkTo(methodOn(TeamController.class).getTeamById(id)).withSelfRel(),
                linkTo(methodOn(TeamController.class).getAllTeams(Pageable.unpaged())).withRel("teams"));

        return ResponseEntity.ok(entityModel);
    }

    @Operation(summary = "Create a new team",
            description = "Registers a new team into the API ecosystem.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Team created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or validation failed"),
            @ApiResponse(responseCode = "409", description = "Conflict: Team already exists")
    })
    @PostMapping
    public ResponseEntity<EntityModel<Team>> createTeam(@Valid @RequestBody Team newTeam) {
        Team savedTeam = repository.save(newTeam);

        EntityModel<Team> entityModel = EntityModel.of(savedTeam,
                linkTo(methodOn(TeamController.class).getTeamById(savedTeam.getId())).withSelfRel(),
                linkTo(methodOn(TeamController.class).getAllTeams(Pageable.unpaged())).withRel("teams"));

        return ResponseEntity
                .created(URI.create("/api/teams/" + savedTeam.getId()))
                .body(entityModel);
    }

    @Operation(summary = "Update or Create a team",
            description = "Updates an existing team or creates a new one if the ID doesn't exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Team updated successfully"),
            @ApiResponse(responseCode = "201", description = "Team created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ID or request body")
    })
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<Team>> updateTeam(@PathVariable("id") Long id, @Valid @RequestBody Team updatedTeam) {

        // 1. SEGURANÇA: Bloqueia IDs inválidos
        if (id <= 0) {
            return ResponseEntity.badRequest().build();
        }

        // 2. SINCRONIA: O ID da URL manda
        updatedTeam.setId(id);

        // 3. VERIFICAÇÃO: Checamos a existência
        boolean exists = repository.existsById(id);

        // 4. PERSISTÊNCIA: Save faz tudo (Update ou Insert)
        Team savedTeam = repository.save(updatedTeam);

        // 5. HATEOAS: Gerando links
        EntityModel<Team> entityModel = EntityModel.of(savedTeam,
                linkTo(methodOn(TeamController.class).getTeamById(id)).withSelfRel(),
                linkTo(methodOn(TeamController.class).getAllTeams(Pageable.unpaged())).withRel("teams"));

        // 6. RESPOSTA DINÂMICA
        if (exists) {
            return ResponseEntity.ok(entityModel); // 200 OK
        } else {
            return ResponseEntity.status(HttpStatus.CREATED).body(entityModel); // 201 Created
        }
    }

    @Operation(summary = "Delete a team",
            description = "Deletes a team from the database. Warning: This may affect players linked to it.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Team deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format"),
            @ApiResponse(responseCode = "404", description = "Team not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable("id") Long id) {
        Team team = repository.findById(id).orElseThrow(() -> new TeamNotFoundException(id));
        repository.delete(team);
        return ResponseEntity.noContent().build();
    }
}