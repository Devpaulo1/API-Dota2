package senac.tsi.dota2.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Schema(description = "Detailed biographical profile of the player",
        example = "{\"biography\": \"Estudante de TSI e Pro Player\", \"twitterHandle\": \"@paulo_dota\", \"totalEarnings\": 500.0, \"player\": {\"id\": 1}}")
public class PlayerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String biography;

    private String twitterHandle;

    private Double totalEarnings; // Total de dinheiro ganho em campeonatos

    // One-to-One (Um Perfil pertence a Um Jogador)
    @OneToOne
    @JoinColumn(name = "player_id", referencedColumnName = "id")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // O PULO DO GATO AQUI
    private Player player;
}