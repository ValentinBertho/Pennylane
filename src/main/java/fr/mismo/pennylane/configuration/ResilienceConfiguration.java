package fr.mismo.pennylane.configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration centralisée pour Resilience4j
 *
 * Cette configuration définit les comportements de résilience pour l'application :
 * - Circuit Breaker : Protection contre les cascades de pannes
 * - Retry : Réessais automatiques des opérations échouées
 * - Rate Limiter : Limitation du débit des appels API
 * - Bulkhead : Isolation des ressources et limitation de concurrence
 * - Time Limiter : Timeout des opérations longues
 *
 * Les configurations spécifiques sont dans application.yml
 *
 * @see application.yml section resilience4j
 * @author Interface Pennylane
 * @since 1.10.2
 */
@Slf4j
@Configuration
public class ResilienceConfiguration {

    /**
     * Configuration par défaut du Circuit Breaker pour les APIs externes
     *
     * Circuit Breaker States:
     * - CLOSED: Fonctionnement normal, appels passent
     * - OPEN: Trop d'échecs détectés, appels bloqués immédiatement
     * - HALF_OPEN: Phase de test, quelques appels autorisés pour vérifier la récupération
     *
     * @return Configuration du Circuit Breaker
     */
    @Bean
    public CircuitBreakerConfig defaultCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
    }

    /**
     * Configuration par défaut du Time Limiter
     *
     * Définit les timeouts par défaut pour les opérations asynchrones
     *
     * @return Configuration du Time Limiter
     */
    @Bean
    public TimeLimiterConfig defaultTimeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(30))
                .cancelRunningFuture(true)
                .build();
    }
}
