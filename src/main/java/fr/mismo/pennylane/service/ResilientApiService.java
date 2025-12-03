package fr.mismo.pennylane.service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Service centralisant l'application des patterns de résilience Resilience4j
 *
 * Ce service fournit des méthodes pour exécuter des opérations avec :
 * - Circuit Breaker : Protection contre les cascades de pannes
 * - Retry : Réessais automatiques avec backoff exponentiel
 * - Rate Limiter : Limitation du débit (100 appels/minute)
 * - Bulkhead : Limitation de la concurrence
 * - Time Limiter : Timeout des opérations longues
 *
 * Utilisation :
 * <pre>
 * {@code
 * // Appel API avec protection complète
 * resilientApiService.executeWithResilience(() -> {
 *     return apiService.callExternalAPI();
 * });
 * }
 * </pre>
 *
 * Configuration : voir application.yml section resilience4j
 * Métriques : Disponibles via /actuator/circuitbreakers, /actuator/retries, etc.
 *
 * @author Interface Pennylane
 * @since 1.10.2
 */
@Slf4j
@Service
public class ResilientApiService {

    private static final String PENNYLANE_API = "pennylaneAPI";
    private static final String DATABASE_OPS = "databaseOperations";

    /**
     * Exécute une opération API avec protection complète Resilience4j
     *
     * Applique dans l'ordre :
     * 1. Rate Limiter (100 appels/minute)
     * 2. Circuit Breaker (protection cascade)
     * 3. Bulkhead (limite concurrence à 10)
     * 4. Retry (3 tentatives avec backoff exponentiel)
     *
     * @param <T> Type de retour de l'opération
     * @param operation L'opération à exécuter
     * @return Le résultat de l'opération
     * @throws Exception Si toutes les tentatives échouent
     */
    @RateLimiter(name = PENNYLANE_API, fallbackMethod = "rateLimitFallback")
    @CircuitBreaker(name = PENNYLANE_API, fallbackMethod = "circuitBreakerFallback")
    @Bulkhead(name = PENNYLANE_API, fallbackMethod = "bulkheadFallback")
    @Retry(name = PENNYLANE_API, fallbackMethod = "retryFallback")
    public <T> T executeWithResilience(Supplier<T> operation) {
        log.debug("Exécution d'une opération API avec résilience Resilience4j");
        return operation.get();
    }

    /**
     * Exécute une opération API de manière asynchrone avec Time Limiter
     *
     * Timeout : 30 secondes (configurable dans application.yml)
     *
     * @param <T> Type de retour de l'opération
     * @param operation L'opération à exécuter
     * @return CompletableFuture contenant le résultat
     */
    @TimeLimiter(name = PENNYLANE_API, fallbackMethod = "timeLimiterFallback")
    @CircuitBreaker(name = PENNYLANE_API)
    @Retry(name = PENNYLANE_API)
    public <T> CompletableFuture<T> executeAsyncWithTimeout(Supplier<T> operation) {
        log.debug("Exécution asynchrone d'une opération API avec timeout");
        return CompletableFuture.supplyAsync(operation);
    }

    /**
     * Exécute une opération base de données avec protection
     *
     * Configuration adaptée aux opérations DB :
     * - Timeout : 60 secondes
     * - Retry : 2 tentatives uniquement
     * - Circuit Breaker : seuil d'échec 60%
     *
     * @param <T> Type de retour de l'opération
     * @param operation L'opération à exécuter
     * @return Le résultat de l'opération
     */
    @CircuitBreaker(name = DATABASE_OPS, fallbackMethod = "databaseFallback")
    @Retry(name = DATABASE_OPS, fallbackMethod = "databaseRetryFallback")
    public <T> T executeDatabaseOperation(Supplier<T> operation) {
        log.debug("Exécution d'une opération base de données avec résilience");
        return operation.get();
    }

    // ==================== FALLBACK METHODS ====================

    /**
     * Fallback appelé lorsque le Rate Limiter est dépassé
     */
    private <T> T rateLimitFallback(Supplier<T> operation, Exception e) {
        log.warn("Rate limit dépassé pour l'API Pennylane. Limite : 100 appels/minute");
        throw new ApiRateLimitException("Trop de requêtes vers l'API Pennylane. Veuillez réessayer dans quelques instants.", e);
    }

    /**
     * Fallback appelé lorsque le Circuit Breaker est ouvert
     */
    private <T> T circuitBreakerFallback(Supplier<T> operation, Exception e) {
        log.error("Circuit breaker ouvert pour l'API Pennylane - Service temporairement indisponible", e);
        throw new ApiCircuitOpenException("L'API Pennylane est temporairement indisponible. Le service tente de se rétablir.", e);
    }

    /**
     * Fallback appelé lorsque le Bulkhead est saturé
     */
    private <T> T bulkheadFallback(Supplier<T> operation, Exception e) {
        log.warn("Bulkhead saturé - Trop d'appels concurrents vers l'API Pennylane");
        throw new ApiBulkheadException("Trop de requêtes simultanées. Veuillez réessayer.", e);
    }

    /**
     * Fallback appelé après épuisement des tentatives de retry
     */
    private <T> T retryFallback(Supplier<T> operation, Exception e) {
        log.error("Échec après {} tentatives de retry pour l'API Pennylane", 3, e);
        throw new ApiRetryExhaustedException("L'opération a échoué après plusieurs tentatives.", e);
    }

    /**
     * Fallback appelé lorsque le timeout est dépassé
     */
    private <T> CompletableFuture<T> timeLimiterFallback(Supplier<T> operation, Exception e) {
        log.error("Timeout dépassé (30s) pour l'opération API", e);
        return CompletableFuture.failedFuture(
            new ApiTimeoutException("L'opération a dépassé le temps limite de 30 secondes.", e)
        );
    }

    /**
     * Fallback pour les opérations base de données
     */
    private <T> T databaseFallback(Supplier<T> operation, Exception e) {
        log.error("Circuit breaker ouvert pour les opérations base de données", e);
        throw new DatabaseCircuitOpenException("La base de données est temporairement indisponible.", e);
    }

    /**
     * Fallback retry pour les opérations base de données
     */
    private <T> T databaseRetryFallback(Supplier<T> operation, Exception e) {
        log.error("Échec après {} tentatives pour l'opération base de données", 2, e);
        throw new DatabaseRetryExhaustedException("L'opération base de données a échoué après plusieurs tentatives.", e);
    }

    // ==================== CUSTOM EXCEPTIONS ====================

    public static class ApiRateLimitException extends RuntimeException {
        public ApiRateLimitException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ApiCircuitOpenException extends RuntimeException {
        public ApiCircuitOpenException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ApiBulkheadException extends RuntimeException {
        public ApiBulkheadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ApiRetryExhaustedException extends RuntimeException {
        public ApiRetryExhaustedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ApiTimeoutException extends RuntimeException {
        public ApiTimeoutException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DatabaseCircuitOpenException extends RuntimeException {
        public DatabaseCircuitOpenException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DatabaseRetryExhaustedException extends RuntimeException {
        public DatabaseRetryExhaustedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
