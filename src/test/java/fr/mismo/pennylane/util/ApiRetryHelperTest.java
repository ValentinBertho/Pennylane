package fr.mismo.pennylane.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiRetryHelperTest {

    @Test
    @DisplayName("executeWithRetry relance les erreurs 503 puis réussit")
    void executeWithRetry_shouldRetryOn503AndSucceed() {
        AtomicInteger attempts = new AtomicInteger();

        String result = ApiRetryHelper.executeWithRetry("test", () -> {
            if (attempts.incrementAndGet() < 3) {
                throw HttpStatusCodeException.create(HttpStatus.SERVICE_UNAVAILABLE, "503", null, null, null);
            }
            return "ok";
        });

        assertEquals("ok", result);
        assertEquals(3, attempts.get());
    }

    @Test
    @DisplayName("executeWithRetry ne relance pas les erreurs 404")
    void executeWithRetry_shouldNotRetryOn404() {
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(HttpStatusCodeException.class, () ->
                ApiRetryHelper.executeWithRetry("test", () -> {
                    attempts.incrementAndGet();
                    throw HttpStatusCodeException.create(HttpStatus.NOT_FOUND, "404", null, null, null);
                })
        );

        assertEquals(1, attempts.get());
    }
}
