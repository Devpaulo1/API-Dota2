package senac.tsi.dota2.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import senac.tsi.dota2.entities.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

    // Mágica do Spring: Busca times que contenham o texto (ignorando maiúsculas/minúsculas) e retorna paginado
    Page<Team> findByNameContainingIgnoreCase(String name, Pageable pageable);
}