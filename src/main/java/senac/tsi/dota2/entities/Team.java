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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
    @NotNull(message = "Team ID is mandatory")
    @JsonProperty("team_id")
    @Schema(description = "Official team ID (OpenDota)", example = "7819701")
    private Long id;

    @NotBlank(message = "The team name cannot be empty")
    @Schema(description = "Full organization name", example = "Team Liquid")
    private String name;

    @NotBlank(message = "Team tag is mandatory")
    @Schema(description = "Official team abbreviation", example = "TL")
    private String tag = "TBD"; // Valor padrão: To Be Defined

    @NotNull(message = "Team rating cannot be null")
    @PositiveOrZero(message = "Rating must be zero or positive")
    @Schema(description = "Team ranking score", example = "1550.5")
    private Double rating = 0.0; // Valor padrão: 0.0

    @JsonIgnore
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    @Schema(description = "List of players currently in this team")
    private List<Player> players = new ArrayList<>();

}