package senac.tsi.dota2.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Schema(description = "Entity representing a professional player",
        example = "{\"nickname\": \"PauloProPlayer\", \"realName\": \"Paulo da Silva\", \"team\": {\"team_id\": 7819701}}")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "The player's nickname cannot be empty")
    private String nickname;

    @NotBlank(message = "The player's real name cannot be empty")
    private String realName;

    // Many-to-One (Muitos Jogadores para Um Time)
    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    // One-to-One (Um Jogador tem Um Perfil)
    @OneToOne(mappedBy = "player", cascade = CascadeType.ALL)
    private PlayerProfile profile;

    public Player() {
    }

    public Player(Long id, String nickname, String realName, Team team) {
        this.id = id;
        this.nickname = nickname;
        this.realName = realName;
        this.team = team;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

}