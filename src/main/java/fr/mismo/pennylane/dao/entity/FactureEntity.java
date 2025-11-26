package fr.mismo.pennylane.dao.entity;

import fr.mismo.pennylane.dto.invoice.FactureDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@NamedNativeQuery(
        name = "FactureEntity.getFacture",
        query = "EXEC SP_PENNYLANE_GET_FACTURE @NO_V_FACTURE = :noVFacture",
        resultSetMapping = "FactureMapping"
)

@SqlResultSetMapping(
        name = "FactureMapping",
        classes = @ConstructorResult(
                targetClass = FactureDTO.class,
                columns = {
                        @ColumnResult(name = "noVFacture", type = String.class),
                        @ColumnResult(name = "chronoVFacture", type = String.class),
                        @ColumnResult(name = "codSite", type = String.class),
                        @ColumnResult(name = "codEtat", type = String.class),
                        @ColumnResult(name = "dateFacture", type = Date.class),
                        @ColumnResult(name = "mttHt", type = Double.class),
                        @ColumnResult(name = "mttTtc", type = Double.class),
                        @ColumnResult(name = "netAPayer", type = Double.class),
                        @ColumnResult(name = "objet", type = String.class),
                        @ColumnResult(name = "noVLFacture", type = String.class),
                        @ColumnResult(name = "noLigne", type = Integer.class),
                        @ColumnResult(name = "typeLigne", type = String.class),
                        @ColumnResult(name = "noProduit", type = Integer.class),
                        @ColumnResult(name = "idProduit", type = Integer.class),
                        @ColumnResult(name = "desCom", type = String.class),
                        @ColumnResult(name = "codTaxe", type = String.class),
                        @ColumnResult(name = "tauxTaxe", type = Double.class),
                        @ColumnResult(name = "qteFac", type = Integer.class),
                        @ColumnResult(name = "puvb", type = Double.class),
                        @ColumnResult(name = "puNet", type = Double.class),
                        @ColumnResult(name = "totalNet", type = Double.class),
                        @ColumnResult(name = "totalHT", type = Double.class),
                        @ColumnResult(name = "noSociete", type = Integer.class),
                        @ColumnResult(name = "customerPennylaneId", type = String.class),
                        @ColumnResult(name = "invoicePennylaneId", type = String.class),
                        @ColumnResult(name = "cpte", type = String.class),
                        @ColumnResult(name = "startDate", type = Date.class),
                        @ColumnResult(name = "endDate", type = Date.class)

                }
        )
)


@Entity
@Table(name = "V_FACTURE")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FactureEntity {

    @Id
    @Column(name = "NO_V_FACTURE")
    private int id;

}
