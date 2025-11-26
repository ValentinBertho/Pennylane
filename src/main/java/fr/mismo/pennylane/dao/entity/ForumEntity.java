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
@Table(name = "FORUM")
public class ForumEntity {

    @Id
    @Column(name = "NO_FORUM")
    @GenericGenerator(name = "NO_FORUM",
            strategy = "fr.mismo.pennylane.dao.AtheneoGenerator",
            parameters = {@org.hibernate.annotations.Parameter(name = AtheneoGenerator.IDENTIFIER, value = "NO_FORUM")})
    @GeneratedValue(generator = "NO_FORUM", strategy = GenerationType.SEQUENCE)
    private int noForum;

    @Column(name = "NO_INCIDENT")
    private String noIncident;

    @Column(name = "CREER_LE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "CREER_PAR")
    private String createdBy;
}
