package senac.tsi.dota2.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import senac.tsi.dota2.entities.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {
}
