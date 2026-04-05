package senac.tsi.dota2.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Entity representing a professional team",
        example = "{\"team_id\": 7819701, \"name\": \"Team Liquid\", \"tag\": \"TL\", \"rating\": 1550.5}")
public class Team {

    @Id
    @JsonProperty("team_id")
    private Long id;

    @NotBlank(message = "The team name cannot be empty")
    private String name;

    private String tag; // sigla time
    private Double rating; // Ranking time

    // One-to-Many (Um Time tem Muitos Jogadores)
    @JsonIgnore
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Player> players = new ArrayList<>();

}