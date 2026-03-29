package senac.tsi.dota2.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import senac.tsi.dota2.entities.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {

    // REQUISITO: Consulta Personalizada (Busca parcial pelo nome do item)
    Page<Item> findByNameContainingIgnoreCase(String name, Pageable pageable);
}