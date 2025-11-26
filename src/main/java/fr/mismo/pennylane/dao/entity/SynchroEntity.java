package fr.mismo.pennylane.dao.entity;

import fr.mismo.pennylane.dao.AtheneoGenerator;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "SYNCHRO_MARQUAGE")
public class SynchroEntity{
    @Id
    @Column(name = "NO_SYNCHRO_MARQUAGE")
    @GenericGenerator(name = "NO_SYNCHRO_MARQUAGE",
            strategy = "fr.mismo.pennylane.dao.AtheneoGenerator",
            parameters = {@org.hibernate.annotations.Parameter(name = AtheneoGenerator.IDENTIFIER, value = "NO_SYNCHRO_MARQUAGE")})
    @GeneratedValue(generator = "NO_SYNCHRO_MARQUAGE", strategy = GenerationType.SEQUENCE)
    private int noSynchro;

    @Column(name = "NOM_ENTITE")
    private String nomEntite;

    @Column(name = "NO_ENTITE")
    private String numeroEntite;

    @Column(name = "DATE_SYNCHRO", columnDefinition = "TIMESTAMP")
    private LocalDateTime dateSync;

    @Column(name = "COD_STATUT_SYNCHRO_MARQUAGE")
    private String codeStatut;

    @Column(name = "INFO")
    private String info;

    @Column(name = "CREER_LE", columnDefinition = "TIMESTAMP")
    private LocalDateTime creerLe;

    @Column(name = "MODIF_LE", columnDefinition = "TIMESTAMP")
    private LocalDateTime modifLe;

    @Column(name = "MODIF_PAR")
    private String modifPar;

    @Column(name = "CREER_PAR")
    private String creerPar;

    @Column(name = "REF_EXT")
    private String refExt;
}
