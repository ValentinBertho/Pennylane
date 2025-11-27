package fr.mismo.pennylane.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires - StringUtils")
class StringUtilsTest {

    @Test
    @DisplayName("removeTrailingZeros - Doit supprimer les zéros")
    void removeTrailingZeros_shouldRemoveTrailingZeros() {
        assertEquals("411", StringUtils.removeTrailingZeros("41100"));
        assertEquals("411", StringUtils.removeTrailingZeros("411000"));
    }

    @Test
    @DisplayName("maskSensitive - Doit masquer les données")
    void maskSensitive_shouldMaskSensitiveData() {
        assertEquals("***6789", StringUtils.maskSensitive("123456789"));
        assertEquals("****", StringUtils.maskSensitive(null));
    }
}
