package fr.mismo.pennylane.dao.entity;

import fr.mismo.pennylane.dto.product.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@NamedNativeQuery(
        name = "ProduitsEntity.getProduit",
        query = "EXEC SP_PENNYLANE_GET_PRODUCTS @NO_PRODUIT = :noProduit",
        resultSetMapping = "ProductMapping"
)

@SqlResultSetMapping(
        name = "ProductMapping",
        classes = @ConstructorResult(
                targetClass = Product.class,
                columns = {
                        @ColumnResult(name = "id", type = Integer.class),
                        @ColumnResult(name = "externalReference", type = String.class),
                        @ColumnResult(name = "reference", type = String.class),
                        @ColumnResult(name = "label", type = String.class),
                        @ColumnResult(name = "description", type = String.class),
                        @ColumnResult(name = "vatRate", type = String.class),
                        @ColumnResult(name = "unit", type = String.class),
                        @ColumnResult(name = "currency", type = String.class),
                        @ColumnResult(name = "priceBeforeTax", type = String.class)
                }
        )
)


@Getter
@Setter
@Entity
@Table(name = "PRODUITS")
public class ProduitsEntity implements Identifiable, Serializable {
    @Id
    @Column(name = "NO_PRODUIT")
    private int id;

    @Column(name = "COD_PROD")
    private String codProd;

    @Column(name = "DES_COM")
    private String desCom;

    @Column(name = "COD_FAMPROD")
    private String codFamProd;

    @Column(name = "TYPE_PROD")
    private String typeProd;

}