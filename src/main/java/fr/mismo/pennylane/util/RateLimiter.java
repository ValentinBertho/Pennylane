package fr.mismo.pennylane.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiter pour gérer les appels API externes avec gestion des quotas.
 * Implémentation thread-safe basée sur l'algorithme Token Bucket.
 *
 * Remplace les Thread.sleep() disséminés dans le code par une gestion centralisée.
 */
@Component
@Slf4j
public class RateLimiter {

    private static final int DEFAULT_MAX_CALLS_PER_MINUTE = 100;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    // Stockage thread-safe des quotas par endpoint
    private final ConcurrentHashMap<String, EndpointQuota> quotas = new ConcurrentHashMap<>();

    /**
     * Classe interne pour gérer les quotas par endpoint
     */
    private static class EndpointQuota {
        private final int maxCallsPerMinute;
        private final AtomicLong callCount = new AtomicLong(0);
        private volatile Instant windowStart = Instant.now();

        EndpointQuota(int maxCallsPerMinute) {
            this.maxCallsPerMinute = maxCallsPerMinute;
        }

        synchronized boolean tryAcquire() {
            Instant now = Instant.now();

            // Réinitialiser la fenêtre si elle est expirée
            if (Duration.between(windowStart, now).compareTo(WINDOW_DURATION) > 0) {
                windowStart = now;
                callCount.set(0);
            }

            // Vérifier si on peut faire l'appel
            if (callCount.get() < maxCallsPerMinute) {
                callCount.incrementAndGet();
                return true;
            }

            return false;
        }

        long getWaitTimeMillis() {
            Instant now = Instant.now();
            Instant windowEnd = windowStart.plus(WINDOW_DURATION);
            return Duration.between(now, windowEnd).toMillis();
        }
    }

    /**
     * Attendre si nécessaire avant de faire un appel API
     *
     * @param endpointKey Identifiant unique de l'endpoint (ex: "pennylane_create_invoice")
     */
    public void waitIfNeeded(String endpointKey) {
        waitIfNeeded(endpointKey, DEFAULT_MAX_CALLS_PER_MINUTE);
    }

    /**
     * Attendre si nécessaire avant de faire un appel API avec un quota personnalisé
     *
     * @param endpointKey Identifiant unique de l'endpoint
     * @param maxCallsPerMinute Nombre maximum d'appels par minute
     */
    public void waitIfNeeded(String endpointKey, int maxCallsPerMinute) {
        EndpointQuota quota = quotas.computeIfAbsent(
            endpointKey,
            k -> new EndpointQuota(maxCallsPerMinute)
        );

        while (!quota.tryAcquire()) {
            long waitTime = Math.min(quota.getWaitTimeMillis(), 5000); // Max 5 secondes
            log.debug("Rate limit atteint pour {}, attente de {}ms", endpointKey, waitTime);

            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Rate limiter interrompu pour {}", endpointKey);
                break;
            }
        }
    }

    /**
     * Réinitialiser les quotas (utile pour les tests)
     */
    public void reset() {
        quotas.clear();
    }

    /**
     * Obtenir le nombre d'appels restants pour un endpoint
     */
    public long getRemainingCalls(String endpointKey) {
        EndpointQuota quota = quotas.get(endpointKey);
        if (quota == null) {
            return DEFAULT_MAX_CALLS_PER_MINUTE;
        }
        return Math.max(0, quota.maxCallsPerMinute - quota.callCount.get());
    }
}
