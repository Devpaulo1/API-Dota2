package senac.tsi.dota2.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import senac.tsi.dota2.entities.PlayerProfile;

public interface PlayerProfileRepository extends JpaRepository<PlayerProfile, Long> {

    // REQUISITO: Consulta Personalizada (Busca parcial pelo Twitter do jogador)
    Page<PlayerProfile> findByTwitterHandleContainingIgnoreCase(String twitterHandle, Pageable pageable);
}