package fr.mismo.pennylane.configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;

/**
 * Configuration de Resilience4j pour la résilience de l'application.
 *
 * Cette configuration fournit :
 * - Circuit Breaker : Protection contre les cascades de pannes
 * - Retry : Retry automatique avec backoff exponentiel
 * - Rate Limiter : Limitation du nombre de requêtes pour respecter les quotas API
 *
 * Ces patterns améliorent la fiabilité de l'application face aux défaillances temporaires.
 */
@Slf4j
@Configuration
public class ResilienceConfig {

    /**
     * Configure le registre de Circuit Breakers.
     *
     * Circuit Breaker Pattern :
     * - Détecte les défaillances répétées d'un service externe
     * - Ouvre le circuit pour éviter les appels inutiles
     * - Tente périodiquement de fermer le circuit (half-open)
     * - Protège contre les cascades de pannes
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
            .slidingWindowSize(10)                      // Fenêtre d'observation de 10 appels
            .minimumNumberOfCalls(5)                     // Min 5 appels avant calcul du taux d'erreur
            .failureRateThreshold(50)                    // Ouvre si 50% d'erreurs
            .waitDurationInOpenState(Duration.ofSeconds(30))  // Attendre 30s avant half-open
            .permittedNumberOfCallsInHalfOpenState(3)    // 3 appels test en half-open
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build();

        // Configuration spécifique pour l'API Pennylane
        CircuitBreakerConfig pennylaneConfig = CircuitBreakerConfig.custom()
            .slidingWindowSize(20)
            .minimumNumberOfCalls(10)
            .failureRateThreshold(40)                    // Plus tolérant (40%)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(5)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build();

        // Configuration pour WSDocument SOAP
        CircuitBreakerConfig wsdocumentConfig = CircuitBreakerConfig.custom()
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(60))  // Attendre plus longtemps (60s)
            .permittedNumberOfCallsInHalfOpenState(3)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);
        registry.circuitBreaker("pennylane-api", pennylaneConfig);
        registry.circuitBreaker("wsdocument", wsdocumentConfig);

        // Logging des événements de circuit breaker
        registry.circuitBreaker("pennylane-api")
            .getEventPublisher()
            .onStateTransition(event ->
                log.warn("Circuit Breaker Pennylane - État changé: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState())
            );

        registry.circuitBreaker("wsdocument")
            .getEventPublisher()
            .onStateTransition(event ->
                log.warn("Circuit Breaker WSDocument - État changé: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState())
            );

        return registry;
    }

    /**
     * Configure le registre de Retry.
     *
     * Retry Pattern :
     * - Réessaye automatiquement les appels échoués
     * - Backoff exponentiel pour éviter de surcharger le service
     * - Limite le nombre de tentatives
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig defaultConfig = RetryConfig.custom()
            .maxAttempts(3)                             // 3 tentatives max
            .waitDuration(Duration.ofMillis(1000))      // Attendre 1s entre chaque tentative
            .retryExceptions(
                ResourceAccessException.class,          // Erreurs réseau
                HttpServerErrorException.class          // Erreurs serveur 5xx
            )
            .build();

        // Configuration pour Pennylane avec backoff exponentiel
        RetryConfig pennylaneConfig = RetryConfig.custom()
            .maxAttempts(4)                             // 4 tentatives
            .waitDuration(Duration.ofMillis(2000))      // Démarrer à 2s
            .retryExceptions(
                ResourceAccessException.class,
                HttpServerErrorException.class
            )
            .build();

        // Configuration pour WSDocument
        RetryConfig wsdocumentConfig = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(1000))
            .retryExceptions(
                ResourceAccessException.class,
                HttpServerErrorException.class
            )
            .build();

        RetryRegistry registry = RetryRegistry.of(defaultConfig);
        registry.retry("pennylane-api", pennylaneConfig);
        registry.retry("wsdocument", wsdocumentConfig);

        // Logging des événements de retry
        registry.retry("pennylane-api")
            .getEventPublisher()
            .onRetry(event ->
                log.warn("Retry Pennylane - Tentative {} après erreur: {}",
                    event.getNumberOfRetryAttempts(),
                    event.getLastThrowable().getMessage())
            );

        registry.retry("wsdocument")
            .getEventPublisher()
            .onRetry(event ->
                log.warn("Retry WSDocument - Tentative {} après erreur: {}",
                    event.getNumberOfRetryAttempts(),
                    event.getLastThrowable().getMessage())
            );

        return registry;
    }

    /**
     * Configure le registre de Rate Limiters.
     *
     * Rate Limiter Pattern :
     * - Limite le nombre de requêtes par période
     * - Respecte les quotas API des services externes
     * - Remplace les Thread.sleep() par une solution non-bloquante
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        // Configuration par défaut : 10 requêtes par seconde
        RateLimiterConfig defaultConfig = RateLimiterConfig.custom()
            .limitForPeriod(10)                         // 10 appels
            .limitRefreshPeriod(Duration.ofSeconds(1))  // par seconde
            .timeoutDuration(Duration.ofSeconds(5))     // Timeout si limite atteinte
            .build();

        // Configuration pour Pennylane : ~2 requêtes par seconde (rate limit API)
        RateLimiterConfig pennylaneConfig = RateLimiterConfig.custom()
            .limitForPeriod(2)                          // 2 appels
            .limitRefreshPeriod(Duration.ofSeconds(1))  // par seconde
            .timeoutDuration(Duration.ofSeconds(5))     // Attendre max 5s
            .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(defaultConfig);
        registry.rateLimiter("pennylane-api", pennylaneConfig);

        // Logging des événements de rate limiting
        registry.rateLimiter("pennylane-api")
            .getEventPublisher()
            .onFailure(event ->
                log.warn("Rate Limiter Pennylane - Limite atteinte, requête refusée")
            );

        return registry;
    }
}
