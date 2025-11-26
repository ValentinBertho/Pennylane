package fr.mismo.pennylane.dao.entity;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "LOG", indexes = {
        @Index(name = "idx_date_log", columnList = "DATE_LOG"),
        @Index(name = "idx_niveau", columnList = "NIVEAU"),
        @Index(name = "idx_traitement", columnList = "TRAITEMENT")
})
public class LogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "DATE_LOG", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateLog;

    @Column(name = "NIVEAU", length = 50)
    private String niveau; // INFO, DEBUG, WARN, ERROR, FATAL

    @Column(name = "TRAITEMENT", length = 100)
    private String traitement;

    @Column(name = "INITIATEUR", length = 100)
    private String initiateur; // Utilisateur ou système initiateur

    @Column(name = "ID_SESSION_SQL")
    private Short idSessionSql;

    @Column(name = "MESSAGE", columnDefinition = "TEXT")
    private String message;

    @Column(name = "CLASSE", length = 200)
    private String classe; // Classe Java source du log

    @Column(name = "METHODE", length = 100)
    private String methode; // Méthode source du log

    @Column(name = "STACK_TRACE", columnDefinition = "TEXT")
    private String stackTrace; // Stack trace en cas d'erreur

    @Column(name = "IP_SOURCE", length = 45)
    private String ipSource; // IPv4 ou IPv6

    @Column(name = "URL_APPELLEE", length = 500)
    private String urlAppellee; // URL REST appelée

    @Column(name = "METHODE_HTTP", length = 10)
    private String methodeHttp; // GET, POST, PUT, DELETE, etc.

    @Column(name = "CODE_RETOUR_HTTP")
    private Integer codeRetourHttp; // 200, 404, 500, etc.

    @Column(name = "DUREE_MS")
    private Long dureeMs; // Durée d'exécution en millisecondes

    @Column(name = "TRAME_REQUETE", columnDefinition = "TEXT")
    private String trameRequete; // Corps de la requête REST

    @Column(name = "TRAME_REPONSE", columnDefinition = "TEXT")
    private String trameReponse; // Corps de la réponse REST

    @Column(name = "ENVIRONNEMENT", length = 50)
    private String environnement; // DEV, INT, PROD

    @Column(name = "APPLICATION", length = 100)
    private String application; // Nom de l'application

    // Constructeurs
    public LogEntity() {
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDateLog() {
        return dateLog;
    }

    public void setDateLog(Date dateLog) {
        this.dateLog = dateLog;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public String getTraitement() {
        return traitement;
    }

    public void setTraitement(String traitement) {
        this.traitement = traitement;
    }

    public String getInitiateur() {
        return initiateur;
    }

    public void setInitiateur(String initiateur) {
        this.initiateur = initiateur;
    }

    public Short getIdSessionSql() {
        return idSessionSql;
    }

    public void setIdSessionSql(Short idSessionSql) {
        this.idSessionSql = idSessionSql;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getClasse() {
        return classe;
    }

    public void setClasse(String classe) {
        this.classe = classe;
    }

    public String getMethode() {
        return methode;
    }

    public void setMethode(String methode) {
        this.methode = methode;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getIpSource() {
        return ipSource;
    }

    public void setIpSource(String ipSource) {
        this.ipSource = ipSource;
    }

    public String getUrlAppellee() {
        return urlAppellee;
    }

    public void setUrlAppellee(String urlAppellee) {
        this.urlAppellee = urlAppellee;
    }

    public String getMethodeHttp() {
        return methodeHttp;
    }

    public void setMethodeHttp(String methodeHttp) {
        this.methodeHttp = methodeHttp;
    }

    public Integer getCodeRetourHttp() {
        return codeRetourHttp;
    }

    public void setCodeRetourHttp(Integer codeRetourHttp) {
        this.codeRetourHttp = codeRetourHttp;
    }

    public Long getDureeMs() {
        return dureeMs;
    }

    public void setDureeMs(Long dureeMs) {
        this.dureeMs = dureeMs;
    }

    public String getTrameRequete() {
        return trameRequete;
    }

    public void setTrameRequete(String trameRequete) {
        this.trameRequete = trameRequete;
    }

    public String getTrameReponse() {
        return trameReponse;
    }

    public void setTrameReponse(String trameReponse) {
        this.trameReponse = trameReponse;
    }

    public String getEnvironnement() {
        return environnement;
    }

    public void setEnvironnement(String environnement) {
        this.environnement = environnement;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }
}