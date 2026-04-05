package senac.tsi.dota2.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private Long id;

    @NotBlank(message = "The item name cannot be empty")
    private String name;

    @NotNull(message = "The item cost cannot be null")
    private Integer cost; // Custo do item em ouro no jogo

    private String description;

    // Many-to-Many (Muitos Itens para Muitos Heróis)
    @ManyToMany(mappedBy = "items")
    @JsonIgnore // O nosso velho amigo salvador de Erro 500 (Loop infinito)
    private List<Hero> heroes = new ArrayList<>();
}