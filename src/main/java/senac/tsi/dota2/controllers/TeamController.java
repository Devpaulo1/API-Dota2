package senac.tsi.dota2.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import senac.tsi.dota2.entities.Team;
import senac.tsi.dota2.exceptions.TeamNotFoundException;
import senac.tsi.dota2.repositories.TeamRepository;

import java.net.URI;
import java.util.List;

@Tag(name = "Teams", description = "Rotas para gerenciar as Equipes Profissionais de Dota 2")
@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamRepository repository;

    public TeamController(TeamRepository repository) {
        this.repository = repository;
    }

    @Operation(summary = "Listar todas as equipes")
    @GetMapping // <-- Rota geral (sem ID)
    @ResponseStatus(HttpStatus.OK)
    public List<Team> getAllTeams() {
        return repository.findAll();
    }

    @Operation(summary = "Buscar equipe por ID")
    @GetMapping("/{id}")
    public Team getTeamById(@PathVariable("id") Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new TeamNotFoundException(id));
    }

    @Operation(summary = "Criar uma nova equipe")
    @PostMapping
    public ResponseEntity<Team> createTeam(@RequestBody Team newTeam) {
        Team savedTeam = repository.save(newTeam);
        return ResponseEntity
                .created(URI.create("/api/teams/" + savedTeam.getId()))
                .body(savedTeam);
    }

    @Operation(summary = "Atualizar uma equipe")
    @PutMapping("/{id}")
    public ResponseEntity<Team> updateTeam(@PathVariable("id") Long id, @RequestBody Team updatedTeam) {
        return repository.findById(id)
                .map(team -> {
                    team.setName(updatedTeam.getName());
                    team.setTag(updatedTeam.getTag());
                    team.setRating(updatedTeam.getRating());
                    return ResponseEntity.ok(repository.save(team));
                })
                .orElseGet(() -> {
                    updatedTeam.setId(id);
                    return ResponseEntity
                            .created(URI.create("/api/teams/" + id))
                            .body(repository.save(updatedTeam));
                });
    }

    @Operation(summary = "Deletar uma equipe")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable("id") Long id) {
        Team team = repository.findById(id).orElseThrow(() -> new TeamNotFoundException(id));
        repository.delete(team);
        return ResponseEntity.noContent().build();
    }
}