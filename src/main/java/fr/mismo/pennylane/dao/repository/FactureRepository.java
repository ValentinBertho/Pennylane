package fr.mismo.pennylane.dao.repository;

import fr.mismo.pennylane.dao.entity.FactureEntity;
import fr.mismo.pennylane.dao.entity.SynchroEntity;
import fr.mismo.pennylane.dto.invoice.FactureDTO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FactureRepository extends CrudRepository<FactureEntity, Integer> {

    @Query(name = "FactureEntity.getFacture", nativeQuery = true)
    List<FactureDTO> getFacture(
            @Param("noVFacture") int noVFacture
    );
}
