package senac.tsi.dota2.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import senac.tsi.dota2.entities.Player;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    // Busca parcial pelo Nickname do jogador
    Page<Player> findByNicknameContainingIgnoreCase(String nickname, Pageable pageable);
}