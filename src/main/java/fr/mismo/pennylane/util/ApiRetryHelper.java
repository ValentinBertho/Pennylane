package fr.mismo.pennylane.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Helper pour rejouer les appels API Pennylane sur erreurs transitoires.
 */
@Slf4j
public final class ApiRetryHelper {

    private static final int MAX_ATTEMPTS = 4;
    private static final long INITIAL_BACKOFF_MS = 750L;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final Set<Integer> RETRYABLE_STATUS = Set.of(429, 500, 502, 503, 504);

    private ApiRetryHelper() {
        // Utility class
    }

    public static <T> T executeWithRetry(Supplier<T> operation, String operationName) {
        long backoffMs = INITIAL_BACKOFF_MS;
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return operation.get();
            } catch (RuntimeException e) {
                if (!isRetryable(e) || attempt == MAX_ATTEMPTS) {
                    throw e;
                }

                lastException = e;
                log.warn("Appel API '{}' en échec (tentative {}/{}), retry dans {}ms: {}",
                        operationName, attempt, MAX_ATTEMPTS, backoffMs, buildErrorSummary(e));

                sleep(backoffMs, operationName);
                backoffMs = (long) (backoffMs * BACKOFF_MULTIPLIER);
            }
        }

        throw lastException != null ? lastException :
                new IllegalStateException("Échec inattendu du retry API pour " + operationName);
    }

    private static boolean isRetryable(RuntimeException e) {
        if (e instanceof ResourceAccessException) {
            return true;
        }

        if (e instanceof RestClientResponseException responseException) {
            HttpStatusCode statusCode = responseException.getStatusCode();
            return RETRYABLE_STATUS.contains(statusCode.value());
        }

        return false;
    }

    private static String buildErrorSummary(RuntimeException e) {
        if (e instanceof RestClientResponseException responseException) {
            return responseException.getStatusCode() + " - " + responseException.getStatusText();
        }
        return e.getMessage();
    }

    private static void sleep(long delayMs, String operationName) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread interrompu pendant le retry de " + operationName, ie);
        }
    }
}
