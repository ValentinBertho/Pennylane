package fr.mismo.pennylane.dao.repository;

import fr.mismo.pennylane.dao.entity.SocieteEntity;
import fr.mismo.pennylane.dto.ath.Tiers;
import fr.mismo.pennylane.dto.product.Product;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends CrudRepository<SocieteEntity, Integer> {

    @Query(name = "ProduitsEntity.getProduit", nativeQuery = true)
    Product getProduct(
            @Param("noProduit") int noProduit
    );
}
