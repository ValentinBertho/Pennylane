package fr.mismo.pennylane.dao.entity;

import fr.mismo.pennylane.dao.AtheneoGenerator;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "FORUM_LIGNE")
public class ForumLigneEntity {

    @Id
    @Column(name = "NO_FORUM_LIGNE")
    @GenericGenerator(name = "NO_FORUM_LIGNE",
            strategy = "fr.mismo.pennylane.dao.AtheneoGenerator",
            parameters = {@org.hibernate.annotations.Parameter(name = AtheneoGenerator.IDENTIFIER, value = "NO_FORUM_LIGNE")})
    @GeneratedValue(generator = "NO_FORUM_LIGNE", strategy = GenerationType.SEQUENCE)
    private int noForumLigne;

    @Column(name = "NO_FORUM")
    private Integer noForum;

    @Column(name = "CREER_LE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "CREER_PAR")
    private String createdBy;

    @Column(name = "COD_USER")
    private String codUser;

    @Column(name = "TYPE_MESSAGE")
    private String typeMessage;

    @Column(name = "NIVEAU")
    private Integer niveau;

    @Column(name = "CONTENU_MESSAGE")
    private String message;

    @ManyToOne(fetch = FetchType.EAGER, optional=true)
    @JoinColumns({
            @JoinColumn(name="NO_FORUM", referencedColumnName="NO_FORUM" , insertable = false, updatable = false),
    })
    private ForumEntity forum;

}
