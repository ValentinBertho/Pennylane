package fr.mismo.pennylane.dao.entity;

import fr.mismo.pennylane.dto.ath.Tiers;
import lombok.Data;

import jakarta.persistence.*;

@NamedNativeQuery(
        name = "SocieteEntity.getTiers",
        query = "EXEC SP_PENNYLANE_GET_TIERS @NO_SOCIETE = :noSociete, @COD_SITE = :codSite",
        resultSetMapping = "TiersMapping"
)

@SqlResultSetMapping(
        name = "TiersMapping",
        classes = @ConstructorResult(
                targetClass = Tiers.class,
                columns = {
                        @ColumnResult(name = "idUnique", type = String.class),
                        @ColumnResult(name = "compteComptable", type = String.class),
                        @ColumnResult(name = "raisonSociale", type = String.class),
                        @ColumnResult(name = "typeTiers", type = String.class),
                        @ColumnResult(name = "adresse1", type = String.class),
                        @ColumnResult(name = "adresse2", type = String.class),
                        @ColumnResult(name = "cp", type = String.class),
                        @ColumnResult(name = "ville", type = String.class),
                        @ColumnResult(name = "pays", type = String.class),
                        @ColumnResult(name = "telephone", type = String.class),
                        @ColumnResult(name = "fax", type = String.class),
                        @ColumnResult(name = "email", type = String.class),
                        @ColumnResult(name = "codeApe", type = String.class),
                        @ColumnResult(name = "siret", type = String.class),
                        @ColumnResult(name = "emailRelance", type = String.class),
                        @ColumnResult(name = "tva", type = String.class),
                        @ColumnResult(name = "codRegion", type = String.class),
                        @ColumnResult(name = "codRglt", type = String.class),
                        @ColumnResult(name = "intituleBanque", type = String.class),
                        @ColumnResult(name = "structureBanque", type = String.class),
                        @ColumnResult(name = "codeBanque", type = String.class),
                        @ColumnResult(name = "guichetBanque", type = String.class),
                        @ColumnResult(name = "compteBanque", type = String.class),
                        @ColumnResult(name = "cleBanque", type = String.class),
                        @ColumnResult(name = "bicBanque", type = String.class),
                        @ColumnResult(name = "codeIso", type = String.class)
                }
        )
)

@Data
@Entity
@Table(name = "SOCIETE")
public class SocieteEntity {
    @Id
    @Column(name = "NO_SOCIETE")
    private int id;

    @Column(name = "JURIDIQ")
    private String juridique;

    @Column(name = "NOM")
    private String nom;

    @Column(name = "DEPEND")
    private String prenom;

    @Column(name = "ADRESSE1")
    private String adresse;

    @Column(name = "SIRET")
    private String siret;

    @Column(name = "CP")
    private String codePostal;

    @Column(name = "VILLE")
    private String ville;

    @Column(name = "PENNYLANE_ID")
    private String pennylaneId;

}
