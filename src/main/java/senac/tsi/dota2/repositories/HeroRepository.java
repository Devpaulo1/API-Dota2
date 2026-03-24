package senac.tsi.dota2.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import senac.tsi.dota2.entities.Hero;

public interface HeroRepository extends JpaRepository<Hero, Long> {
}