package senac.tsi.dota2.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
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