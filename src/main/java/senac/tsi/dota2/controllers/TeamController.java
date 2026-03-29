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

    // Injetando o repositório e o montador de páginas
    public TeamController(TeamRepository repository, PagedResourcesAssembler<Team> pagedResourcesAssembler) {
        this.repository = repository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all teams (Paginated)", description = "Returns the complete list of teams with HATEOAS links and pagination.")
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

    @Operation(summary = "Filter by Name", description = "Finds teams containing the specified name (case-insensitive) with pagination.")
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

    @Operation(summary = "Get team by ID", description = "Finds a specific team. Returns 404 Not Found if the ID does not exist.")
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

    @Operation(summary = "Create a new team", description = "Adds a custom team to the database.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Team created successfully")
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

    @Operation(summary = "Update a team", description = "Updates an existing team's data. If the ID does not exist, it creates a new one.")
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<Team>> updateTeam(@PathVariable("id") Long id, @Valid @RequestBody Team updatedTeam) {
        return repository.findById(id)
                .map(team -> {
                    team.setName(updatedTeam.getName());
                    team.setTag(updatedTeam.getTag());
                    team.setRating(updatedTeam.getRating());
                    Team savedTeam = repository.save(team);

                    EntityModel<Team> entityModel = EntityModel.of(savedTeam,
                            linkTo(methodOn(TeamController.class).getTeamById(savedTeam.getId())).withSelfRel(),
                            linkTo(methodOn(TeamController.class).getAllTeams(Pageable.unpaged())).withRel("teams"));

                    return ResponseEntity.ok(entityModel); // 200 OK
                })
                .orElseGet(() -> {
                    updatedTeam.setId(id);
                    Team savedTeam = repository.save(updatedTeam);

                    EntityModel<Team> entityModel = EntityModel.of(savedTeam,
                            linkTo(methodOn(TeamController.class).getTeamById(savedTeam.getId())).withSelfRel(),
                            linkTo(methodOn(TeamController.class).getAllTeams(Pageable.unpaged())).withRel("teams"));

                    return ResponseEntity
                            .created(URI.create("/api/teams/" + id))
                            .body(entityModel); // 201 Created
                });
    }

    @Operation(summary = "Delete a team", description = "Removes a team from the local database by ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Team deleted successfully (No content returned)"),
            @ApiResponse(responseCode = "404", description = "Team not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable("id") Long id) {
        Team team = repository.findById(id).orElseThrow(() -> new TeamNotFoundException(id));
        repository.delete(team);
        return ResponseEntity.noContent().build();
    }
}