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
@Schema(description = "Entity representing a Dota 2 hero",
        example = "{\"id\": 999, \"localized_name\": \"killrockts\", \"attack_type\": \"Ranged\"}")
public class Hero {

    @Id
    @NotNull(message = "Hero ID is mandatory")
    @Schema(description = "Unique hero ID (manual/OpenDota)", example = "999")
    private Long id;

    @NotBlank(message = "Hero name cannot be blank")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    @JsonProperty("localized_name")
    @Schema(description = "Localized name of the hero", example = "killrockts")
    private String localizedName;

    @NotNull(message = "Attack type is mandatory")
    @Enumerated(EnumType.STRING)
    @JsonProperty("attack_type")
    @Schema(description = "Hero's attack type", example = "Ranged")
    private AttackType attackType;

    @Size(max = 6, message = "Hero inventory cannot exceed 6 items")
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinTable(
            name = "hero_item",
            joinColumns = @JoinColumn(name = "hero_id"),
            inverseJoinColumns = @JoinColumn(name = "item_id")
    )
    @Schema(description = "List of items equipped by the hero (max 6)")
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