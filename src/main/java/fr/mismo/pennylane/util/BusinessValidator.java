package fr.mismo.pennylane.util;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe utilitaire pour les validations métier.
 * Centralise toutes les règles de validation pour assurer la cohérence.
 */
@Slf4j
public final class BusinessValidator {

    private BusinessValidator() {
        // Classe utilitaire
    }

    // Constantes de validation
    public static final BigDecimal MIN_INVOICE_AMOUNT = new BigDecimal("0.01");
    public static final BigDecimal MAX_INVOICE_AMOUNT = new BigDecimal("10000000.00"); // 10M€
    public static final int MAX_FUTURE_YEARS = 10;
    public static final int MAX_INVOICE_AGE_YEARS = 10;

    /**
     * Résultat de validation avec messages d'erreur
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            return String.join(", ", errors);
        }
    }

    /**
     * Valide un montant de facture
     *
     * @param amount Montant à valider
     * @param fieldName Nom du champ (pour le message d'erreur)
     * @return Résultat de la validation
     */
    public static ValidationResult validateAmount(BigDecimal amount, String fieldName) {
        List<String> errors = new ArrayList<>();

        if (amount == null) {
            errors.add(fieldName + " ne peut pas être null");
            return new ValidationResult(false, errors);
        }

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            errors.add(fieldName + " ne peut pas être négatif: " + amount);
        }

        if (amount.compareTo(MIN_INVOICE_AMOUNT) < 0) {
            errors.add(fieldName + " est inférieur au minimum autorisé (" + MIN_INVOICE_AMOUNT + "): " + amount);
        }

        if (amount.compareTo(MAX_INVOICE_AMOUNT) > 0) {
            errors.add(fieldName + " dépasse le maximum autorisé (" + MAX_INVOICE_AMOUNT + "): " + amount);
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Valide une date de facture
     *
     * @param invoiceDate Date de la facture
     * @param allowFuture Autoriser les dates futures
     * @return Résultat de la validation
     */
    public static ValidationResult validateInvoiceDate(LocalDate invoiceDate, boolean allowFuture) {
        List<String> errors = new ArrayList<>();

        if (invoiceDate == null) {
            errors.add("La date de facture ne peut pas être null");
            return new ValidationResult(false, errors);
        }

        LocalDate now = LocalDate.now();
        LocalDate maxPast = now.minusYears(MAX_INVOICE_AGE_YEARS);
        LocalDate maxFuture = now.plusDays(allowFuture ? 1 : 0);

        if (invoiceDate.isBefore(maxPast)) {
            errors.add("La date de facture est trop ancienne (>" + MAX_INVOICE_AGE_YEARS + " ans): " + invoiceDate);
        }

        if (invoiceDate.isAfter(maxFuture)) {
            errors.add("La date de facture est dans le futur: " + invoiceDate);
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Valide une date d'échéance par rapport à la date de facture
     *
     * @param invoiceDate Date de la facture
     * @param deadlineDate Date d'échéance
     * @return Résultat de la validation
     */
    public static ValidationResult validateDeadline(LocalDate invoiceDate, LocalDate deadlineDate) {
        List<String> errors = new ArrayList<>();

        if (invoiceDate == null) {
            errors.add("La date de facture ne peut pas être null");
        }

        if (deadlineDate == null) {
            errors.add("La date d'échéance ne peut pas être null");
        }

        if (!errors.isEmpty()) {
            return new ValidationResult(false, errors);
        }

        if (deadlineDate.isBefore(invoiceDate)) {
            errors.add("La date d'échéance (" + deadlineDate + ") ne peut pas être avant la date de facture (" + invoiceDate + ")");
        }

        LocalDate maxDeadline = LocalDate.now().plusYears(MAX_FUTURE_YEARS);
        if (deadlineDate.isAfter(maxDeadline)) {
            errors.add("La date d'échéance est trop loin dans le futur (>" + MAX_FUTURE_YEARS + " ans): " + deadlineDate);
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Valide la cohérence HT + TVA = TTC
     *
     * @param amountHT Montant hors taxes
     * @param amountTVA Montant de TVA
     * @param amountTTC Montant toutes taxes comprises
     * @return Résultat de la validation
     */
    public static ValidationResult validateAmountCoherence(BigDecimal amountHT, BigDecimal amountTVA, BigDecimal amountTTC) {
        List<String> errors = new ArrayList<>();

        if (amountHT == null || amountTVA == null || amountTTC == null) {
            errors.add("Les montants HT, TVA et TTC ne peuvent pas être null");
            return new ValidationResult(false, errors);
        }

        BigDecimal calculatedTTC = amountHT.add(amountTVA);
        BigDecimal difference = amountTTC.subtract(calculatedTTC).abs();

        // Tolérance de 0.02€ (2 centimes) pour gérer les arrondis
        BigDecimal tolerance = new BigDecimal("0.02");

        if (difference.compareTo(tolerance) > 0) {
            errors.add(String.format(
                    "Incohérence HT+TVA≠TTC: %s + %s = %s, attendu %s (différence: %s)",
                    amountHT, amountTVA, calculatedTTC, amountTTC, difference
            ));
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Valide un numéro SIRET
     *
     * @param siret Numéro SIRET à valider
     * @return Résultat de la validation
     */
    public static ValidationResult validateSIRET(String siret) {
        List<String> errors = new ArrayList<>();

        if (siret == null || siret.isEmpty()) {
            // SIRET optionnel, pas d'erreur si absent
            return new ValidationResult(true, errors);
        }

        // Nettoyer (supprimer espaces et caractères non-numériques)
        String cleaned = siret.replaceAll("[^0-9]", "");

        if (cleaned.length() != 14) {
            errors.add("Le SIRET doit contenir 14 chiffres: " + siret);
            return new ValidationResult(false, errors);
        }

        // Validation Luhn modifiée pour SIRET
        if (!isValidSIRETChecksum(cleaned)) {
            errors.add("Le SIRET a une somme de contrôle invalide: " + siret);
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Vérifie la somme de contrôle d'un SIRET (algorithme Luhn modifié)
     */
    private static boolean isValidSIRETChecksum(String siret) {
        try {
            int sum = 0;
            for (int i = 0; i < 14; i++) {
                int digit = Character.getNumericValue(siret.charAt(i));

                if (i % 2 == 1) {
                    digit *= 2;
                    if (digit > 9) {
                        digit -= 9;
                    }
                }

                sum += digit;
            }

            return sum % 10 == 0;
        } catch (Exception e) {
            log.error("Erreur lors de la validation du SIRET: {}", siret, e);
            return false;
        }
    }

    /**
     * Valide un SIREN (9 premiers chiffres du SIRET)
     *
     * @param siren Numéro SIREN à valider
     * @return Résultat de la validation
     */
    public static ValidationResult validateSIREN(String siren) {
        List<String> errors = new ArrayList<>();

        if (siren == null || siren.isEmpty()) {
            return new ValidationResult(true, errors);
        }

        String cleaned = siren.replaceAll("[^0-9]", "");

        if (cleaned.length() != 9) {
            errors.add("Le SIREN doit contenir 9 chiffres: " + siren);
            return new ValidationResult(false, errors);
        }

        // Utiliser les 9 premiers chiffres pour valider avec SIRET
        // (Ajouter 00001 comme établissement principal)
        String fakeSiret = cleaned + "00001";
        if (!isValidSIRETChecksum(fakeSiret)) {
            errors.add("Le SIREN a une somme de contrôle invalide: " + siren);
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Valide qu'une chaîne n'est pas vide
     *
     * @param value Valeur à valider
     * @param fieldName Nom du champ
     * @param required Si true, le champ est obligatoire
     * @return Résultat de la validation
     */
    public static ValidationResult validateString(String value, String fieldName, boolean required) {
        List<String> errors = new ArrayList<>();

        if (required && (value == null || value.trim().isEmpty())) {
            errors.add(fieldName + " est obligatoire");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Valide un email (format simple)
     *
     * @param email Email à valider
     * @param required Si true, l'email est obligatoire
     * @return Résultat de la validation
     */
    public static ValidationResult validateEmail(String email, boolean required) {
        List<String> errors = new ArrayList<>();

        if (email == null || email.trim().isEmpty()) {
            if (required) {
                errors.add("L'email est obligatoire");
            }
            return new ValidationResult(errors.isEmpty(), errors);
        }

        // Validation basique
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            errors.add("Format d'email invalide: " + email);
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }
}
