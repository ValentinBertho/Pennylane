package fr.mismo.pennylane.dao.entity;

import lombok.Data;

import jakarta.persistence.*;


@Data
@Entity
@Table(name = "COURRIER_VERSION")
public class CourrierVersionEntity {

    @Id
    @Column(name = "NO_COURRIER_VERSION")
    private int noCourrierVersion;

    @Column(name = "NO_COURRIE")
    private int noCourrie;

    @Column(name = "EXTENSION")
    private String extension;
}
