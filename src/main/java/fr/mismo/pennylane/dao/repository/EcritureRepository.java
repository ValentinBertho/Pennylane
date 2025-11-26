package fr.mismo.pennylane.dao.repository;

import fr.mismo.pennylane.dao.entity.SynchroEntity;
import fr.mismo.pennylane.dto.ath.Ecriture;
import fr.mismo.pennylane.dto.invoice.FactureDTO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EcritureRepository extends CrudRepository<SynchroEntity, Integer> {

    @Query(name = "EcritureEntity.getEcritures", nativeQuery = true)
    List<Ecriture> getEcrituresToExport(
            @Param("noEcritureLot") Integer noEcritureLot
    );

    @Query(value = "EXEC SP_PENNYLANE_EXPORT_LOT :codSite", nativeQuery = true)
    List<Integer> getLotEcritureToExport(@Param("codSite") int codSite);

    @Query(value = "EXEC SP_PENNYLANE_CUSTOMER_INVOICE_BAP :codSite", nativeQuery = true)
    List<String> getAFactureBAP(
            @Param("codSite") String codSite
    );

    @Query(value = "EXEC SP_PENNYLANE_SUPPLIER_INVOICE_REGLEMENT :codSite", nativeQuery = true)
    List<String> getMajReglement(
            @Param("codSite") String codSite
    );

}
