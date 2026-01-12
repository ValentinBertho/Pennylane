package fr.mismo.pennylane.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.TransientDataAccessException;

import java.util.function.Supplier;

/**
 * Classe utilitaire pour gérer les retry avec backoff exponentiel
 * Utilisée principalement pour gérer les deadlocks SQL
 */
@Slf4j
public class RetryHelper {

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 100;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    /**
     * Exécute une opération avec retry en cas de deadlock
     * @param operation L'opération à exécuter
     * @param operationName Le nom de l'opération (pour les logs)
     * @param <T> Le type de retour
     * @return Le résultat de l'opération
     * @throws RuntimeException Si toutes les tentatives échouent
     */
    public static <T> T executeWithRetry(Supplier<T> operation, String operationName) {
        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;

        while (attempt < MAX_RETRIES) {
            try {
                return operation.get();
            } catch (CannotAcquireLockException | TransientDataAccessException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    log.error("Échec de l'opération '{}' après {} tentatives", operationName, MAX_RETRIES, e);
                    throw new RuntimeException("Échec après " + MAX_RETRIES + " tentatives: " + operationName, e);
                }

                log.warn("Deadlock détecté pour l'opération '{}' (tentative {}/{}). Attente de {}ms avant retry...",
                        operationName, attempt, MAX_RETRIES, backoffMs);

                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrompu pendant le retry", ie);
                }

                backoffMs = (long) (backoffMs * BACKOFF_MULTIPLIER);
            }
        }

        throw new RuntimeException("Échec inattendu de l'opération: " + operationName);
    }

    /**
     * Exécute une opération void avec retry en cas de deadlock
     * @param operation L'opération à exécuter
     * @param operationName Le nom de l'opération (pour les logs)
     */
    public static void executeWithRetryVoid(Runnable operation, String operationName) {
        executeWithRetry(() -> {
            operation.run();
            return null;
        }, operationName);
    }
}
