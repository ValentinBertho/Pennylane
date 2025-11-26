package fr.mismo.pennylane.dao.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "COURRIER")
public class CourrierEntity {

    @Id
    @Column(name = "NO_COURRIE")
    private int noCourrie;

    @Column(name = "TITRE_C", length = 200)
    private String titreC;

    @Column(name = "NO_COURRIER_VERSION_DERNIER")
    private int lastVersion;

    @Column(name = "FICHIER")
    private String fichier;

    @Column(name = "COD_TYPE_C")
    private String docType;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "NO_COURRIER_VERSION_DERNIER", referencedColumnName = "NO_COURRIER_VERSION", updatable = false, insertable = false)
    private CourrierVersionEntity courrierVersion;
}
