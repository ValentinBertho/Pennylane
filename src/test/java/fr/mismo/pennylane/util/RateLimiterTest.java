package fr.mismo.pennylane.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires - RateLimiter")
class RateLimiterTest {

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
        rateLimiter.reset();
    }

    @Test
    @DisplayName("Doit permettre les appels dans la limite du quota")
    void shouldAllowCallsWithinQuota() {
        String endpoint = "test_endpoint";
        int maxCalls = 10;

        long start = System.currentTimeMillis();
        for (int i = 0; i < maxCalls; i++) {
            rateLimiter.waitIfNeeded(endpoint, maxCalls);
        }
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration < 100);
    }

    @Test
    @DisplayName("Doit retourner le nombre d'appels restants")
    void shouldReturnRemainingCalls() {
        String endpoint = "test_endpoint";
        int maxCalls = 10;

        assertEquals(maxCalls, rateLimiter.getRemainingCalls(endpoint));

        rateLimiter.waitIfNeeded(endpoint, maxCalls);
        assertEquals(maxCalls - 1, rateLimiter.getRemainingCalls(endpoint));
    }
}
