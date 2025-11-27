package fr.mismo.pennylane.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires - ApiException")
class ApiExceptionTest {

    @Test
    @DisplayName("Doit créer une exception avec tous les paramètres")
    void shouldCreateExceptionWithAllParameters() {
        Exception cause = new RuntimeException("Root cause");
        ApiException exception = new ApiException("API error", cause, 500);

        assertEquals("API error", exception.getMessage());
        assertEquals(500, exception.getHttpStatusCode());
        assertEquals("INTERNAL_SERVER_ERROR", exception.getErrorCode());
    }

    @Test
    @DisplayName("isRetryable - Doit identifier les erreurs retriables")
    void isRetryable_shouldIdentifyRetryableErrors() {
        assertTrue(new ApiException("", 429).isRetryable());
        assertTrue(new ApiException("", 503).isRetryable());
        assertFalse(new ApiException("", 404).isRetryable());
    }
}
