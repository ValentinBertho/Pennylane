package fr.mismo.pennylane.api;

import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.util.ApiConstants;
import fr.mismo.pennylane.util.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.Collections;

/**
 * Classe de base pour tous les clients API Pennylane.
 * Centralise la logique commune : construction des headers, gestion des erreurs, rate limiting.
 *
 * Avant cette refactorisation, chaque API client dupliquait :
 * - La méthode headerBuilder() (7 occurrences identiques)
 * - La méthode handleException() (5 occurrences similaires)
 * - Les appels Thread.sleep() (23 occurrences)
 */
@Component
@Slf4j
public abstract class AbstractApi {

    @Autowired
    protected RateLimiter rateLimiter;

    /**
     * Construit les headers HTTP pour les appels API Pennylane
     *
     * @param token Token d'authentification Pennylane
     * @return Headers HTTP configurés avec authentification et content-type
     */
    protected HttpHeaders buildHeaders(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.error("Token d'authentification null ou vide");
            throw new IllegalArgumentException("Le token d'authentification est requis");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Ne pas logger le token complet pour des raisons de sécurité
        log.trace("Headers construits avec token: Bearer ***{}",
                token.length() > 4 ? token.substring(token.length() - 4) : "****");

        return headers;
    }

    /**
     * Extrait le token du site de manière sécurisée
     *
     * @param site Entity contenant le token
     * @return Token d'authentification
     * @throws IllegalArgumentException si le site ou le token est invalide
     */
    protected String extractToken(SiteEntity site) {
        if (site == null) {
            log.error("SiteEntity est null");
            throw new IllegalArgumentException("Le site ne peut pas être null");
        }

        String token = site.getPennylaneToken();
        if (token == null || token.trim().isEmpty()) {
            log.error("Token Pennylane absent pour le site {}", site.getCode());
            throw new IllegalArgumentException("Token Pennylane manquant pour le site " + site.getCode());
        }

        return token;
    }

    /**
     * Gère les exceptions des appels API de manière centralisée
     *
     * @param e Exception capturée
     * @param operation Nom de l'opération (pour le logging)
     * @param entityId Identifiant de l'entité concernée (optionnel)
     * @return ApiException wrappant l'erreur originale
     */
    protected ApiException handleException(Exception e, String operation, String entityId) {
        String context = entityId != null
                ? String.format("%s (ID: %s)", operation, entityId)
                : operation;

        if (e instanceof HttpClientErrorException) {
            HttpClientErrorException clientError = (HttpClientErrorException) e;
            log.error("Erreur client HTTP lors de {} - Status: {}, Body: {}",
                    context, clientError.getStatusCode(), clientError.getResponseBodyAsString(), e);

            return new ApiException(
                    "Erreur client lors de " + operation,
                    e,
                    clientError.getStatusCode().value()
            );
        }

        if (e instanceof HttpServerErrorException) {
            HttpServerErrorException serverError = (HttpServerErrorException) e;
            log.error("Erreur serveur HTTP lors de {} - Status: {}, Body: {}",
                    context, serverError.getStatusCode(), serverError.getResponseBodyAsString(), e);

            return new ApiException(
                    "Erreur serveur lors de " + operation,
                    e,
                    serverError.getStatusCode().value()
            );
        }

        if (e instanceof ResourceAccessException) {
            log.error("Erreur d'accès réseau lors de {} - Message: {}",
                    context, e.getMessage(), e);

            return new ApiException(
                    "Erreur réseau lors de " + operation,
                    e,
                    503 // Service Unavailable
            );
        }

        if (e instanceof RestClientException) {
            log.error("Erreur REST client lors de {} - Message: {}",
                    context, e.getMessage(), e);

            return new ApiException(
                    "Erreur REST lors de " + operation,
                    e,
                    500
            );
        }

        // Erreur générique
        log.error("Erreur inattendue lors de {} - Type: {}, Message: {}",
                context, e.getClass().getSimpleName(), e.getMessage(), e);

        return new ApiException(
                "Erreur inattendue lors de " + operation,
                e,
                500
        );
    }

    /**
     * Applique le rate limiting avant un appel API
     *
     * @param endpointKey Clé identifiant l'endpoint (utiliser ApiConstants.Endpoints)
     */
    protected void applyRateLimit(String endpointKey) {
        rateLimiter.waitIfNeeded(endpointKey, ApiConstants.RateLimit.PENNYLANE_MAX_CALLS_PER_MINUTE);
    }

    /**
     * Applique le rate limiting avec un quota personnalisé
     *
     * @param endpointKey Clé identifiant l'endpoint
     * @param maxCallsPerMinute Nombre maximum d'appels par minute
     */
    protected void applyRateLimit(String endpointKey, int maxCallsPerMinute) {
        rateLimiter.waitIfNeeded(endpointKey, maxCallsPerMinute);
    }

    /**
     * Méthode utilitaire pour construire une URL avec paramètres
     *
     * @param baseUrl URL de base
     * @param params Paramètres à ajouter (varargs alternant clé/valeur)
     * @return URL complète avec paramètres
     */
    protected String buildUrl(String baseUrl, Object... params) {
        if (params.length % 2 != 0) {
            throw new IllegalArgumentException("Les paramètres doivent être fournis par paires clé/valeur");
        }

        StringBuilder url = new StringBuilder(baseUrl);
        boolean firstParam = !baseUrl.contains("?");

        for (int i = 0; i < params.length; i += 2) {
            String key = String.valueOf(params[i]);
            Object value = params[i + 1];

            if (value != null) {
                url.append(firstParam ? "?" : "&");
                url.append(key).append("=").append(value);
                firstParam = false;
            }
        }

        return url.toString();
    }

    /**
     * Valide qu'un objet n'est pas null
     *
     * @param object Objet à valider
     * @param paramName Nom du paramètre (pour le message d'erreur)
     * @throws IllegalArgumentException si l'objet est null
     */
    protected void requireNonNull(Object object, String paramName) {
        if (object == null) {
            String message = String.format("Le paramètre '%s' ne peut pas être null", paramName);
            log.error(message);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Valide une chaîne de caractères
     *
     * @param value Valeur à valider
     * @param paramName Nom du paramètre
     * @throws IllegalArgumentException si la valeur est null ou vide
     */
    protected void requireNonEmpty(String value, String paramName) {
        if (value == null || value.trim().isEmpty()) {
            String message = String.format("Le paramètre '%s' ne peut pas être vide", paramName);
            log.error(message);
            throw new IllegalArgumentException(message);
        }
    }
}
