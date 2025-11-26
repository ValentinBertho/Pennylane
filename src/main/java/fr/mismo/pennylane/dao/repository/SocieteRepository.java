package fr.mismo.pennylane.dao.repository;

import fr.mismo.pennylane.dao.entity.CourrierEntity;
import fr.mismo.pennylane.dao.entity.SocieteEntity;
import fr.mismo.pennylane.dto.ath.Tiers;
import fr.mismo.pennylane.dto.invoice.FactureDTO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SocieteRepository extends CrudRepository<SocieteEntity, Integer> {

    @Query(name = "SocieteEntity.getTiers", nativeQuery = true)
    Tiers getTiers(
            @Param("noSociete") int noSociete,
            @Param("codSite") String codSite
    );

}
