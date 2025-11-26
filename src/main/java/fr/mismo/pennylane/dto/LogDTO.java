package fr.mismo.pennylane.dto;

import lombok.Data;

import java.util.Date;

@Data
public class LogDTO {

    private Long id;
    private Date dateLog;
    private String niveau;
    private String traitement;
    private String initiateur;
    private String message;
    private String classe;
    private String methode;
    private String stackTrace;
    private String ipSource;
    private String urlAppellee;
    private String methodeHttp;
    private Integer codeRetourHttp;
    private Integer idSessionSql;
    private Long dureeMs;
    private String trameRequete;
    private String trameReponse;
    private String environnement;
    private String application;

    // Constructeurs
    public LogDTO() {
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

    public void setIdSessionSql(Integer idSessionSql) {
        this.idSessionSql = idSessionSql;
    }

    public Integer getIdSessionSql() {
        return idSessionSql;
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

    // Méthode utilitaire pour obtenir la classe CSS selon le niveau
    public String getNiveauCssClass() {
        if (niveau == null) return "secondary";
        switch (niveau.toUpperCase()) {
            case "ERROR":
            case "FATAL":
                return "danger";
            case "WARN":
                return "warning";
            case "INFO":
                return "info";
            case "DEBUG":
                return "secondary";
            default:
                return "secondary";
        }
    }

    // Méthode utilitaire pour obtenir l'icône selon le niveau
    public String getNiveauIcon() {
        if (niveau == null) return "bi-info-circle";
        switch (niveau.toUpperCase()) {
            case "ERROR":
            case "FATAL":
                return "bi-x-circle-fill";
            case "WARN":
                return "bi-exclamation-triangle-fill";
            case "INFO":
                return "bi-info-circle-fill";
            case "DEBUG":
                return "bi-bug-fill";
            default:
                return "bi-info-circle";
        }
    }
}
