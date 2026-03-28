package senac.tsi.dota2.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class Hero {

    @Id
    private Long id;

    //PErguntar para o professor, como o @NotBlank verifica se esta em branco e tambem se é null, não precisa usar o @NotNull correto?
    @NotNull(message = "O nome do herói não pode ser nulo")
    @NotBlank(message = "O nome do herói não pode estar em branco")
    @Size(min = 2, max = 255, message = "O nome deve ter entre 2 e 255 caracteres")
    @JsonProperty("localized_name")
    private String localizedName;

    @NotNull(message = "O tipo de ataque é obrigatório")
    @Enumerated(EnumType.STRING)
    @JsonProperty("attack_type")
    private AttackType attackType;

    public Hero() {
    }

    public enum AttackType {
        @JsonProperty("Melee")
        Melee,

        @JsonProperty("Ranged")
        Ranged
    }
}