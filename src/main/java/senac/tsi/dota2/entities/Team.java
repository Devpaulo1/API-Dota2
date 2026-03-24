package senac.tsi.dota2.entities;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class Team {

    @Id
    @JsonProperty("team_id") //Defini como eu chamo na API Dota o id dos times
    private Long id;

    @NotBlank
    private String name;
    private String tag; //sigla time
    private Double rating; //Ranking time
}
