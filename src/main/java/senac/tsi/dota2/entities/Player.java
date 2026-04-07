package senac.tsi.dota2.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Entity representing a professional player",
        example = "{\"nickname\": \"PauloProPlayer\", \"realName\": \"Paulo da Silva\", \"team\": {\"team_id\": 7819701}}")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique player ID (auto-generated)", example = "1")
    private Long id;

    @NotBlank(message = "The player's nickname cannot be empty")
    @Size(min = 2, max = 50, message = "Nickname must be between 2 and 50 characters")
    @Schema(description = "In-game nickname", example = "PauloProPlayer")
    private String nickname;

    @NotBlank(message = "The player's real name cannot be empty")
    @Size(min = 2, max = 100, message = "Real name must be between 2 and 100 characters")
    @Schema(description = "Full real name of the player", example = "Paulo da Silva")
    private String realName;

    @ManyToOne(fetch = FetchType.EAGER) // REMOVI O CASCADE DAQUI
    @JoinColumn(name = "team_id")
    @Schema(description = "The team the player is currently signed to")
    private Team team;

    @OneToOne(mappedBy = "player", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Schema(description = "Player's biographical profile")
    private PlayerProfile profile;

}