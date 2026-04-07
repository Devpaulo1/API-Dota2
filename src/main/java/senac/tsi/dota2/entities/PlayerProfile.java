package senac.tsi.dota2.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Entity
@Schema(description = "Detailed biographical profile of the player",
        example = "{\"biography\": \"TSI Student and Pro Player\", \"twitterHandle\": \"@paulo_dota\", \"totalEarnings\": 500.0, \"player\": {\"id\": 1}}")
public class PlayerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique profile ID (auto-generated)", example = "1")
    private Long id;

    @NotBlank(message = "Biography is required")
    @Size(max = 1000, message = "Biography cannot exceed 1000 characters")
    @Schema(description = "Player's biography", example = "TSI Student and Pro Player")
    private String biography = "No biography available";

    @NotBlank(message = "Twitter handle is required")
    @Size(max = 20, message = "Twitter handle cannot exceed 20 characters")
    @Schema(description = "Player's Twitter/X handle", example = "@paulo_dota")
    private String twitterHandle = "@none";

    @NotNull(message = "Total earnings is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total earnings cannot be negative")
    @Schema(description = "Total prize money won", example = "500.0")
    private Double totalEarnings = 0.0;

    @OneToOne
    @NotNull(message = "A profile must be linked to a player")
    @JoinColumn(name = "player_id", referencedColumnName = "id")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(description = "The player this profile belongs to")
    private Player player;
}