package fr.mismo.pennylane.dao.repository;

import fr.mismo.pennylane.dao.entity.CourrierEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Stream;

@Repository
public interface CourrierRepository extends CrudRepository<CourrierEntity, Integer> {


    @Query(value = "EXEC SP_PENNYLANE_EXPORT_FACTURE_COURRIER :noFacture", nativeQuery = true)
    Stream<CourrierEntity> callExportFactureCourrier(@Param("noFacture") int noFacture);


}
