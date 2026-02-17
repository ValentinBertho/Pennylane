package fr.mismo.pennylane.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Set;
import java.util.function.Supplier;

@Slf4j
public final class ApiRetryHelper {

    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 500L;
    private static final Set<HttpStatus> RETRYABLE_STATUSES = Set.of(
            HttpStatus.BAD_GATEWAY,
            HttpStatus.SERVICE_UNAVAILABLE,
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.INTERNAL_SERVER_ERROR
    );

    private ApiRetryHelper() {
    }

    public static <T> T executeWithRetry(String operationName, Supplier<T> operation) {
        long backoff = INITIAL_BACKOFF_MS;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return operation.get();
            } catch (HttpStatusCodeException e) {
                if (!shouldRetry(e, attempt)) {
                    throw e;
                }
                log.warn("{} - tentative {}/{} en échec (HTTP {}). Retry dans {}ms",
                        operationName, attempt, MAX_ATTEMPTS, e.getStatusCode(), backoff);
            } catch (ResourceAccessException e) {
                if (attempt == MAX_ATTEMPTS) {
                    throw e;
                }
                log.warn("{} - tentative {}/{} en échec (timeout/réseau). Retry dans {}ms",
                        operationName, attempt, MAX_ATTEMPTS, backoff);
            }

            sleep(backoff);
            backoff *= 2;
        }

        return null;
    }

    private static boolean shouldRetry(HttpStatusCodeException e, int attempt) {
        return attempt < MAX_ATTEMPTS && RETRYABLE_STATUSES.contains(e.getStatusCode());
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread interrompu pendant le retry API", ie);
        }
    }
}
