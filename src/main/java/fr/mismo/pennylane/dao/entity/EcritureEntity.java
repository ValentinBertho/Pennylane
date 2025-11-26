package fr.mismo.pennylane.dao.entity;

import fr.mismo.pennylane.dto.ath.Ecriture;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@NamedNativeQuery(
        name = "EcritureEntity.getEcritures",
        query = "EXEC SP_PENNYLANE_EXPORT_ECRITURES @NO_ECRITURE_LOT = :noEcritureLot",
        resultSetMapping = "EcritureMapping"
)

@SqlResultSetMapping(
        name = "EcritureMapping",
        classes = @ConstructorResult(
                targetClass = Ecriture.class,
                columns = {
                        @ColumnResult(name = "noEcriturePiece", type = Integer.class), // noEcriturePiece
                        @ColumnResult(name = "noEcritureLigne", type = Integer.class), // noEcritureLigne
                        @ColumnResult(name = "dateEcriture", type = LocalDate.class), // dateEcriture
                        @ColumnResult(name = "codeJournal", type = String.class), // codeJournal
                        @ColumnResult(name = "compteGeneral", type = String.class), // compteGeneral
                        @ColumnResult(name = "num", type = String.class), // num
                        @ColumnResult(name = "numeroDePiece", type = String.class), // numeroDePiece
                        @ColumnResult(name = "reference", type = String.class), // reference
                        @ColumnResult(name = "compteTiers", type = String.class), // compteTiers
                        @ColumnResult(name = "libelle", type = String.class), // libelle
                        @ColumnResult(name = "dateEcheance", type = LocalDate.class), // dateEcheance
                        @ColumnResult(name = "sens", type = String.class), // sens
                        @ColumnResult(name = "montant", type = Double.class), // montant
                        @ColumnResult(name = "type", type = String.class), // type
                        @ColumnResult(name = "sectionAxe", type = String.class), // sectionAxe
                        @ColumnResult(name = "section", type = String.class), // section
                        @ColumnResult(name = "noAFacture", type = Integer.class), // noAFacture
                        @ColumnResult(name = "noVFacture", type = Integer.class) // noVFacture
                }
        )
)



@Entity
@Table(name = "ECRITURE_LOT")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EcritureEntity {

    @Id
    @Column(name = "NO_ECRITURE_LOT")
    private int id;

}
