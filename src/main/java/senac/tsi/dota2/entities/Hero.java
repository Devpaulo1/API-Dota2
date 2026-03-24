package senac.tsi.dota2.entities;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Hero {
    @Id
    @JsonProperty("id")
    private  Long id;

    @JsonProperty("localized_name")
    private String localizedName;

    @JsonProperty("attack_type")
    private String attackType;

    public Hero(Long id, String localizedName, String attackType) {
        this.id = id;
        this.localizedName = localizedName;
        this.attackType = attackType;
    }
}
