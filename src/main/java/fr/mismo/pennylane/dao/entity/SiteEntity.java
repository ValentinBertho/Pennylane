package fr.mismo.pennylane.dao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "T_SITE")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteEntity {

    @Id
    @Column(name = "NO_T_SITE")
    private int id;

    @Column(name = "CODE")
    private String code;

    @Column(name = "LIBELLE")
    private String libelle;

    @Column(name = "NO_SOCIETE")
    private String noSociete;

    @Column(name = "PENNYLANE_TOKEN")
    private String pennylaneToken;

    @Column(name = "PENNYLANE_ACTIF")
    private Boolean pennylaneActif;

    @Column(name = "PENNYLANE_ACHAT")
    private Boolean pennylaneAchat;
}
