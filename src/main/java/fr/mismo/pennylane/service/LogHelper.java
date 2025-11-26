package fr.mismo.pennylane.service;


import fr.mismo.pennylane.dao.entity.LogEntity;
import fr.mismo.pennylane.dao.repository.LogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

@Component
public class LogHelper {

    @Autowired
    private LogRepository logRepository;

    @Value("${spring.application.name:GLPI}")
    private String applicationName;

    @Value("${spring.profiles.active:DEV}")
    private String environnement;

    /**
     * Log une information simple
     */
    public void info(String traitement, String message) {
        log("INFO", traitement, message, null, null, null);
    }

    /**
     * Log un avertissement
     */
    public void warn(String traitement, String message) {
        log("WARN", traitement, message, null, null, null);
    }

    /**
     * Log une erreur
     */
    public void error(String traitement, String message, Exception e) {
        String stackTrace = e != null ? getStackTraceAsString(e) : null;
        log("ERROR", traitement, message, null, null, stackTrace);
    }

    /**
     * Log un appel REST avec toutes les informations
     */
    public void logRestCall(String traitement, String methodeHttp, String url,
                            Integer codeRetour, Long dureeMs,
                            String trameRequete, String trameReponse) {
        LogEntity logEntity = new LogEntity();
        logEntity.setDateLog(new Date());
        logEntity.setNiveau(codeRetour >= 400 ? "ERROR" : "INFO");
        logEntity.setTraitement(traitement);
        logEntity.setApplication(applicationName);
        logEntity.setEnvironnement(environnement);
        logEntity.setMethodeHttp(methodeHttp);
        logEntity.setUrlAppellee(url);
        logEntity.setCodeRetourHttp(codeRetour);
        logEntity.setDureeMs(dureeMs);
        logEntity.setTrameRequete(trameRequete);
        logEntity.setTrameReponse(trameReponse);

        // Récupérer les informations de la requête HTTP courante si disponible
        enrichirAvecContexteHttp(logEntity);

        // Récupérer les informations de la stack
        enrichirAvecInfoStack(logEntity);

        logEntity.setMessage(String.format("%s %s - Code: %d - Durée: %dms",
                methodeHttp, url, codeRetour, dureeMs));

        logRepository.save(logEntity);
    }

    /**
     * Log générique avec tous les paramètres
     */
    public void log(String niveau, String traitement, String message,
                    String initiateur, String classe, String stackTrace) {
        LogEntity logEntity = new LogEntity();
        logEntity.setDateLog(new Date());
        logEntity.setNiveau(niveau);
        logEntity.setTraitement(traitement);
        logEntity.setMessage(message);
        logEntity.setInitiateur(initiateur);
        logEntity.setClasse(classe);
        logEntity.setStackTrace(stackTrace);
        logEntity.setApplication(applicationName);
        logEntity.setEnvironnement(environnement);

        // Enrichir avec le contexte HTTP si disponible
        enrichirAvecContexteHttp(logEntity);

        // Enrichir avec les informations de la stack si pas déjà renseigné
        if (classe == null) {
            enrichirAvecInfoStack(logEntity);
        }

        logRepository.save(logEntity);
    }

    /**
     * Enrichit le log avec les informations de la requête HTTP courante
     */
    private void enrichirAvecContexteHttp(LogEntity logEntity) {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                logEntity.setIpSource(getClientIpAddress(request));
                logEntity.setInitiateur(request.getRemoteUser());

                if (logEntity.getUrlAppellee() == null) {
                    logEntity.setUrlAppellee(request.getRequestURI());
                }
                if (logEntity.getMethodeHttp() == null) {
                    logEntity.setMethodeHttp(request.getMethod());
                }
            }
        } catch (Exception e) {
            // Pas de contexte HTTP disponible (tâche planifiée, etc.)
        }
    }

    /**
     * Enrichit le log avec les informations de la stack d'appel
     */
    private void enrichirAvecInfoStack(LogEntity logEntity) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // Trouver le premier élément qui n'est pas cette classe
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (!className.equals(this.getClass().getName()) &&
                    !className.startsWith("java.") &&
                    !className.startsWith("sun.")) {

                logEntity.setClasse(className);
                logEntity.setMethode(element.getMethodName());
                break;
            }
        }
    }

    /**
     * Récupère l'adresse IP réelle du client (prend en compte les proxies)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Prendre la première IP si plusieurs sont présentes
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Convertit une exception en String pour la stocker
     */
    private String getStackTraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Log de démarrage d'un traitement avec retour du timestamp pour calculer la durée
     */
    public long startTraitement(String traitement) {
        info(traitement, "Démarrage du traitement");
        return System.currentTimeMillis();
    }

    /**
     * Log de fin de traitement avec calcul automatique de la durée
     */
    public void endTraitement(String traitement, long startTime) {
        long dureeMs = System.currentTimeMillis() - startTime;

        LogEntity logEntity = new LogEntity();
        logEntity.setDateLog(new Date());
        logEntity.setNiveau("INFO");
        logEntity.setTraitement(traitement);
        logEntity.setMessage("Fin du traitement");
        logEntity.setDureeMs(dureeMs);
        logEntity.setApplication(applicationName);
        logEntity.setEnvironnement(environnement);

        enrichirAvecContexteHttp(logEntity);
        enrichirAvecInfoStack(logEntity);

        logRepository.save(logEntity);
    }
}