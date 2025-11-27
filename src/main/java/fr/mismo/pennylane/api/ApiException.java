package fr.mismo.pennylane.api;

import lombok.Getter;

/**
 * Exception personnalisée pour les erreurs d'API Pennylane.
 * Permet de centraliser la gestion des erreurs et d'inclure le code HTTP.
 */
@Getter
public class ApiException extends RuntimeException {

    private final int httpStatusCode;
    private final String errorCode;

    public ApiException(String message, Throwable cause, int httpStatusCode) {
        super(message, cause);
        this.httpStatusCode = httpStatusCode;
        this.errorCode = determineErrorCode(httpStatusCode);
    }

    public ApiException(String message, int httpStatusCode) {
        super(message);
        this.httpStatusCode = httpStatusCode;
        this.errorCode = determineErrorCode(httpStatusCode);
    }

    public ApiException(String message, Throwable cause, int httpStatusCode, String errorCode) {
        super(message, cause);
        this.httpStatusCode = httpStatusCode;
        this.errorCode = errorCode;
    }

    /**
     * Détermine un code d'erreur métier à partir du code HTTP
     */
    private String determineErrorCode(int statusCode) {
        return switch (statusCode) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 429 -> "RATE_LIMIT_EXCEEDED";
            case 500 -> "INTERNAL_SERVER_ERROR";
            case 503 -> "SERVICE_UNAVAILABLE";
            default -> "API_ERROR";
        };
    }

    /**
     * Vérifie si l'erreur est temporaire et peut être retentée
     */
    public boolean isRetryable() {
        return httpStatusCode == 429 // Rate limit
                || httpStatusCode == 503 // Service unavailable
                || httpStatusCode >= 500; // Erreurs serveur
    }

    /**
     * Vérifie si l'erreur est due à une requête invalide
     */
    public boolean isClientError() {
        return httpStatusCode >= 400 && httpStatusCode < 500;
    }

    @Override
    public String toString() {
        return String.format("ApiException{httpStatus=%d, errorCode='%s', message='%s'}",
                httpStatusCode, errorCode, getMessage());
    }
}
