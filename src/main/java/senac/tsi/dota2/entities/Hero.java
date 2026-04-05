package senac.tsi.dota2.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Entidade que representa um herói do Dota 2",
        example = "{\"id\": 999, \"localized_name\": \"killrockts\", \"attack_type\": \"Ranged\"}")
public class Hero {

    @Id
    @Schema(description = "ID único do herói (manual)", example = "999")
    private Long id;

    @NotBlank(message = "O nome do herói não pode estar em branco")
    @Size(min = 2, max = 255, message = "O nome deve ter entre 2 e 255 caracteres")
    @JsonProperty("localized_name")
    @Schema(description = "Nome localizado do herói", example = "killrockts")
    private String localizedName;

    @NotNull(message = "O tipo de ataque é obrigatório")
    @Enumerated(EnumType.STRING)
    @JsonProperty("attack_type")
    @Schema(description = "Tipo de ataque do herói", example = "Ranged")
    private AttackType attackType;

    @ManyToMany
    @JoinTable(
            name = "hero_item",
            joinColumns = @JoinColumn(name = "hero_id"),
            inverseJoinColumns = @JoinColumn(name = "item_id")
    )
    @Schema(description = "Lista de itens equipados pelo herói")
    private List<Item> items = new ArrayList<>();

    public Hero() {
    }

    public enum AttackType {
        @JsonProperty("Melee")
        Melee,

        @JsonProperty("Ranged")
        Ranged
    }
}