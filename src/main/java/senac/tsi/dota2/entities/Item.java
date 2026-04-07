package senac.tsi.dota2.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Schema(description = "Entity representing a game item",
        example = "{\"name\": \"Blink Dagger\", \"cost\": 2250, \"description\": \"Teleport item\"}")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique item ID (auto-generated)", example = "1")
    private Long id;

    @NotBlank(message = "The item name cannot be empty")
    @Schema(description = "Item name (e.g., Blink Dagger)", example = "Blink Dagger")
    private String name;

    @NotNull(message = "The item cost cannot be null")
    @PositiveOrZero(message = "Cost must be zero or positive")
    @Schema(description = "Gold cost of the item", example = "2250")
    private Integer cost = 0; // Valor padrão para evitar nulos

    @NotBlank(message = "The item description cannot be empty")
    @Schema(description = "Detailed description of the item's effects",
            example = "Teleport to a target point up to 1200 units away.")
    private String description = "No description available"; // Valor padrão

    @ManyToMany(mappedBy = "items")
    @JsonIgnore
    @Schema(description = "List of heroes that can use this item")
    private List<Hero> heroes = new ArrayList<>();
}