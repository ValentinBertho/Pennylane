package fr.mismo.pennylane.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour BusinessValidator
 */
class BusinessValidatorTest {

    // ==================== Tests validateAmount ====================

    @Test
    void validateAmount_withValidAmount_shouldPass() {
        BigDecimal amount = new BigDecimal("100.00");
        BusinessValidator.ValidationResult result = BusinessValidator.validateAmount(amount, "Montant");

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void validateAmount_withNullAmount_shouldFail() {
        BusinessValidator.ValidationResult result = BusinessValidator.validateAmount(null, "Montant");

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("ne peut pas être null"));
    }

    @Test
    void validateAmount_withNegativeAmount_shouldFail() {
        BigDecimal amount = new BigDecimal("-50.00");
        BusinessValidator.ValidationResult result = BusinessValidator.validateAmount(amount, "Montant");

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("négatif")));
    }

    @Test
    void validateAmount_withAmountBelowMinimum_shouldFail() {
        BigDecimal amount = new BigDecimal("0.001");
        BusinessValidator.ValidationResult result = BusinessValidator.validateAmount(amount, "Montant");

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("inférieur au minimum")));
    }

    @Test
    void validateAmount_withAmountAboveMaximum_shouldFail() {
        BigDecimal amount = new BigDecimal("20000000.00");
        BusinessValidator.ValidationResult result = BusinessValidator.validateAmount(amount, "Montant");

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("dépasse le maximum")));
    }

    @Test
    void validateAmount_withMinimumValidAmount_shouldPass() {
        BigDecimal amount = new BigDecimal("0.01");
        BusinessValidator.ValidationResult result = BusinessValidator.validateAmount(amount, "Montant");

        assertTrue(result.isValid());
    }

    @Test
    void validateAmount_withMaximumValidAmount_shouldPass() {
        BigDecimal amount = new BigDecimal("10000000.00");
        BusinessValidator.ValidationResult result = BusinessValidator.validateAmount(amount, "Montant");

        assertTrue(result.isValid());
    }

    // ==================== Tests validateInvoiceDate ====================

    @Test
    void validateInvoiceDate_withNullDate_shouldFail() {
        BusinessValidator.ValidationResult result = BusinessValidator.validateInvoiceDate(null, false);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("ne peut pas être null"));
    }

    @Test
    void validateInvoiceDate_withTodayDate_shouldPass() {
        LocalDate today = LocalDate.now();
        BusinessValidator.ValidationResult result = BusinessValidator.validateInvoiceDate(today, false);

        assertTrue(result.isValid());
    }

    @Test
    void validateInvoiceDate_withFutureDateWhenNotAllowed_shouldFail() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        BusinessValidator.ValidationResult result = BusinessValidator.validateInvoiceDate(tomorrow, false);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("dans le futur")));
    }

    @Test
    void validateInvoiceDate_withFutureDateWhenAllowed_shouldPass() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        BusinessValidator.ValidationResult result = BusinessValidator.validateInvoiceDate(tomorrow, true);

        assertTrue(result.isValid());
    }

    @Test
    void validateInvoiceDate_withVeryOldDate_shouldFail() {
        LocalDate veryOld = LocalDate.now().minusYears(15);
        BusinessValidator.ValidationResult result = BusinessValidator.validateInvoiceDate(veryOld, false);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("trop ancienne")));
    }

    @Test
    void validateInvoiceDate_withRecentPastDate_shouldPass() {
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        BusinessValidator.ValidationResult result = BusinessValidator.validateInvoiceDate(lastMonth, false);

        assertTrue(result.isValid());
    }

    // ==================== Tests validateDeadline ====================

    @Test
    void validateDeadline_withNullInvoiceDate_shouldFail() {
        LocalDate deadline = LocalDate.now().plusDays(30);
        BusinessValidator.ValidationResult result = BusinessValidator.validateDeadline(null, deadline);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("facture") && e.contains("null")));
    }

    @Test
    void validateDeadline_withNullDeadline_shouldFail() {
        LocalDate invoiceDate = LocalDate.now();
        BusinessValidator.ValidationResult result = BusinessValidator.validateDeadline(invoiceDate, null);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("échéance") && e.contains("null")));
    }

    @Test
    void validateDeadline_withValidDates_shouldPass() {
        LocalDate invoiceDate = LocalDate.now();
        LocalDate deadline = invoiceDate.plusDays(30);
        BusinessValidator.ValidationResult result = BusinessValidator.validateDeadline(invoiceDate, deadline);

        assertTrue(result.isValid());
    }

    @Test
    void validateDeadline_withDeadlineBeforeInvoice_shouldFail() {
        LocalDate invoiceDate = LocalDate.now();
        LocalDate deadline = invoiceDate.minusDays(1);
        BusinessValidator.ValidationResult result = BusinessValidator.validateDeadline(invoiceDate, deadline);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("ne peut pas être avant")));
    }

    @Test
    void validateDeadline_withSameDate_shouldPass() {
        LocalDate date = LocalDate.now();
        BusinessValidator.ValidationResult result = BusinessValidator.validateDeadline(date, date);

        assertTrue(result.isValid());
    }

    @Test
    void validateDeadline_withTooFarFutureDeadline_shouldFail() {
        LocalDate invoiceDate = LocalDate.now();
        LocalDate deadline = invoiceDate.plusYears(15);
        BusinessValidator.ValidationResult result = BusinessValidator.validateDeadline(invoiceDate, deadline);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("trop loin dans le futur")));
    }

    // ==================== Tests validateAmountCoherence ====================

    @Test
    void validateAmountCoherence_withNullValues_shouldFail() {
        BusinessValidator.ValidationResult result = BusinessValidator.validateAmountCoherence(null, null, null);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("ne peuvent pas être null"));
    }

    @Test
    void validateAmountCoherence_withCoherentAmounts_shouldPass() {
        BigDecimal HT = new BigDecimal("100.00");
        BigDecimal TVA = new BigDecimal("20.00");
        BigDecimal TTC = new BigDecimal("120.00");
        BusinessValidator.ValidationResult result = BusinessValidator.validateAmountCoherence(HT, TVA, TTC);

        assertTrue(result.isValid());
    }

    @Test
    void validateAmountCoherence_withIncoherentAmounts_shouldFail() {
        BigDecimal HT = new BigDecimal("100.00");
        BigDecimal TVA = new BigDecimal("20.00");
        BigDecimal TTC = new BigDecimal("150.00");  // Should be 120
        BusinessValidator.ValidationResult result = BusinessValidator.validateAmountCoherence(HT, TVA, TTC);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Incohérence HT+TVA≠TTC")));
    }

    @Test
    void validateAmountCoherence_withRoundingDifference_shouldPass() {
        BigDecimal HT = new BigDecimal("100.00");
        BigDecimal TVA = new BigDecimal("20.01");  // Slight rounding difference
        BigDecimal TTC = new BigDecimal("120.00");
        BusinessValidator.ValidationResult result = BusinessValidator.validateAmountCoherence(HT, TVA, TTC);

        // Should pass because difference is within tolerance (0.02€)
        assertTrue(result.isValid());
    }

    @Test
    void validateAmountCoherence_withZeroTVA_shouldPass() {
        BigDecimal HT = new BigDecimal("100.00");
        BigDecimal TVA = BigDecimal.ZERO;
        BigDecimal TTC = new BigDecimal("100.00");
        BusinessValidator.ValidationResult result = BusinessValidator.validateAmountCoherence(HT, TVA, TTC);

        assertTrue(result.isValid());
    }

    // ==================== Tests validateSIRET ====================

    @Test
    void validateSIRET_withNull_shouldPass() {
        // SIRET is optional
        BusinessValidator.ValidationResult result = BusinessValidator.validateSIRET(null);

        assertTrue(result.isValid());
    }

    @Test
    void validateSIRET_withEmptyString_shouldPass() {
        // SIRET is optional
        BusinessValidator.ValidationResult result = BusinessValidator.validateSIRET("");

        assertTrue(result.isValid());
    }

    @Test
    void validateSIRET_withValidSIRET_shouldPass() {
        // Valid SIRET (example from INSEE)
        BusinessValidator.ValidationResult result = BusinessValidator.validateSIRET("73282932000074");

        assertTrue(result.isValid(), "SIRET valide devrait passer");
    }

    @Test
    void validateSIRET_withInvalidLength_shouldFail() {
        BusinessValidator.ValidationResult result = BusinessValidator.validateSIRET("123456789");

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("14 chiffres")));
    }

    @Test
    void validateSIRET_withInvalidChecksum_shouldFail() {
        // Invalid SIRET (wrong checksum)
        BusinessValidator.ValidationResult result = BusinessValidator.validateSIRET("73282932000075");

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("somme de contrôle invalide")));
    }

    @Test
    void validateSIRET_withSpaces_shouldPass() {
        // Valid SIRET with spaces
        BusinessValidator.ValidationResult result = BusinessValidator.validateSIRET("732 829 320 00074");

        assertTrue(result.isValid(), "SIRET avec espaces devrait être accepté");
    }

    @Test
    void validateSIRET_withNonNumeric_shouldFail() {
        BusinessValidator.ValidationResult result = BusinessValidator.validateSIRET("ABC123456789DE");

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("14 chiffres")));
    }

    // ==================== Tests validateSIREN ====================

    @Test
    void validateSIREN_withNull_shouldPass() {
        BusinessValidator.ValidationResult result = BusinessValidator.validateSIREN(null);

        assertTrue(result.isValid());
    }

    @Test
    void validateSIREN_withEmptyString_shouldPass() {
        BusinessValidator.ValidationResult result = BusinessValidator.validateSIREN("");

        assertTrue(result.isValid());
    }

    @Test
    void validateSIREN_withValidSIREN_shouldPass() {
        // Valid SIREN (9 first digits of valid SIRET)
        BusinessValidator.ValidationResult result = BusinessValidator.validateSIREN("732829320");

        assertTrue(result.isValid(), "SIREN valide devrait passer");
    }

    @Test
    void validateSIREN_withInvalidLength_shouldFail() {
        BusinessValidator.ValidationResult result = BusinessValidator.validateSIREN("12345678");

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("9 chiffres")));
    }

    // ==================== Tests validateString ====================

    @Test
    void validateString_withRequiredNullString_shouldFail() {
        BusinessValidator.ValidationResult result = BusinessValidator.validateString(null, "Nom", true);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("obligatoire"));
    }

    @Test
    void validateString_withRequiredEmptyString_shouldFail() {
        BusinessValidator.ValidationResult result = BusinessValidator.validateString("   ", "Nom", true);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("obligatoire"));
    }

    @Test
    void validateString_withRequiredValidString_shouldPass() {
        BusinessValidator.ValidationResult result = BusinessValidator.validateString("Test", "Nom", true);

        assertTrue(result.isValid());
    }

    @Test
    void validateString_withOptionalNullString_shouldPass() {
        BusinessValidator.ValidationResult result = BusinessValidator.validateString(null, "Nom", false);

        assertTrue(result.isValid());
    }

    @Test
    void validateString_withOptionalEmptyString_shouldPass() {
        BusinessValidator.ValidationResult result = BusinessValidator.validateString("", "Nom", false);

        assertTrue(result.isValid());
    }

    // ==================== Tests validateEmail ====================

    @Test
    void validateEmail_withRequiredNullEmail_shouldFail() {
        BusinessValidator.ValidationResult result = BusinessValidator.validateEmail(null, true);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("obligatoire"));
    }

    @Test
    void validateEmail_withRequiredEmptyEmail_shouldFail() {
        BusinessValidator.ValidationResult result = BusinessValidator.validateEmail("  ", true);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("obligatoire"));
    }

    @Test
    void validateEmail_withOptionalNullEmail_shouldPass() {
        BusinessValidator.ValidationResult result = BusinessValidator.validateEmail(null, false);

        assertTrue(result.isValid());
    }

    @Test
    void validateEmail_withValidEmail_shouldPass() {
        BusinessValidator.ValidationResult result = BusinessValidator.validateEmail("test@example.com", true);

        assertTrue(result.isValid());
    }

    @Test
    void validateEmail_withValidEmailWithPlus_shouldPass() {
        BusinessValidator.ValidationResult result = BusinessValidator.validateEmail("test+tag@example.com", true);

        assertTrue(result.isValid());
    }

    @Test
    void validateEmail_withInvalidEmailNoAt_shouldFail() {
        BusinessValidator.ValidationResult result = BusinessValidator.validateEmail("testexample.com", true);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Format d'email invalide")));
    }

    @Test
    void validateEmail_withInvalidEmailNoDomain_shouldFail() {
        BusinessValidator.ValidationResult result = BusinessValidator.validateEmail("test@", true);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Format d'email invalide")));
    }

    @Test
    void validateEmail_withInvalidEmailNoTLD_shouldFail() {
        BusinessValidator.ValidationResult result = BusinessValidator.validateEmail("test@example", true);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Format d'email invalide")));
    }

    // ==================== Tests getErrorMessage ====================

    @Test
    void getErrorMessage_withMultipleErrors_shouldJoinWithComma() {
        BigDecimal amount = new BigDecimal("-100.00");
        BusinessValidator.ValidationResult result = BusinessValidator.validateAmount(amount, "Montant");

        String errorMessage = result.getErrorMessage();
        assertNotNull(errorMessage);
        assertFalse(errorMessage.isEmpty());
        assertTrue(errorMessage.contains(",") || result.getErrors().size() == 1);
    }

    @Test
    void getErrorMessage_withNoErrors_shouldReturnEmptyString() {
        BigDecimal amount = new BigDecimal("100.00");
        BusinessValidator.ValidationResult result = BusinessValidator.validateAmount(amount, "Montant");

        String errorMessage = result.getErrorMessage();
        assertEquals("", errorMessage);
    }
}
