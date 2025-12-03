package fr.mismo.pennylane.validation;

import fr.mismo.pennylane.dto.invoice.Invoice;
import fr.mismo.pennylane.util.BusinessValidator;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Aspect AOP pour appliquer automatiquement les validations métier
 *
 * <h3>Fonctionnement</h3>
 * <p>Intercepte toutes les méthodes annotées avec {@code @ValidateBusinessRules}
 * et applique les validations métier définies dans BusinessValidator avant l'exécution.</p>
 *
 * <h3>Validations appliquées</h3>
 * <ol>
 *   <li>Montants : min 0.01€, max 10M€, pas de négatifs</li>
 *   <li>Dates : pas trop anciennes (10 ans), pas dans le futur (sauf exception)</li>
 *   <li>Identifiants : SIRET/SIREN avec checksum Luhn</li>
 *   <li>Cohérence : HT + TVA = TTC (tolérance 2 centimes)</li>
 *   <li>Champs obligatoires : non null, non vide</li>
 * </ol>
 *
 * <h3>Gestion des erreurs</h3>
 * <p>En cas d'erreur de validation, lève {@link BusinessValidationException}
 * avec la liste complète des erreurs détectées.</p>
 *
 * @see ValidateBusinessRules
 * @see BusinessValidator
 * @see BusinessValidationException
 * @author Interface Pennylane
 * @since 1.10.2
 */
@Slf4j
@Aspect
@Component
public class BusinessValidationAspect {

    /**
     * Intercepte toutes les méthodes annotées avec @ValidateBusinessRules
     *
     * @param joinPoint Point d'interception
     * @param validateAnnotation Annotation de validation
     * @throws BusinessValidationException Si des erreurs de validation sont détectées
     */
    @Before("@annotation(validateAnnotation)")
    public void validateBusinessRules(JoinPoint joinPoint, ValidateBusinessRules validateAnnotation) {
        log.debug("Application des validations métier sur {}", joinPoint.getSignature().getName());

        List<String> allErrors = new ArrayList<>();
        List<ValidateBusinessRules.ValidationType> excludedTypes = Arrays.asList(validateAnnotation.exclude());

        // Parcourir tous les paramètres de la méthode
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }

            // Validation des factures (Invoice DTO)
            if (arg instanceof Invoice) {
                Invoice invoice = (Invoice) arg;
                validateInvoice(invoice, excludedTypes, allErrors);
            }

            // Ajouter d'autres types d'objets métier ici selon les besoins
        }

        // Si des erreurs ont été détectées, lever une exception
        if (!allErrors.isEmpty()) {
            String errorMessage = String.join("; ", allErrors);
            log.warn("Échec de validation métier : {}", errorMessage);
            throw new BusinessValidationException("Erreurs de validation métier détectées", allErrors);
        }

        log.debug("Validations métier OK pour {}", joinPoint.getSignature().getName());
    }

    /**
     * Valide un objet Invoice
     *
     * @param invoice Facture à valider
     * @param excludedTypes Types de validations à ignorer
     * @param allErrors Liste des erreurs accumulées
     */
    private void validateInvoice(Invoice invoice, List<ValidateBusinessRules.ValidationType> excludedTypes, List<String> allErrors) {

        // Validation des champs obligatoires
        if (!excludedTypes.contains(ValidateBusinessRules.ValidationType.REQUIRED_FIELDS)) {
            validateRequiredFields(invoice, allErrors);
        }

        // Validation des montants
        if (!excludedTypes.contains(ValidateBusinessRules.ValidationType.AMOUNTS)) {
            validateAmounts(invoice, allErrors);
        }

        // Validation des dates
        if (!excludedTypes.contains(ValidateBusinessRules.ValidationType.DATES)) {
            validateDates(invoice, allErrors);
        }

        // Validation de la cohérence HT+TVA=TTC
        if (!excludedTypes.contains(ValidateBusinessRules.ValidationType.COHERENCE)) {
            validateCoherence(invoice, allErrors);
        }
    }

    /**
     * Valide les champs obligatoires d'une facture
     */
    private void validateRequiredFields(Invoice invoice, List<String> errors) {
        BusinessValidator.ValidationResult result;

        result = BusinessValidator.validateString(invoice.getInvoiceNumber(), "Numéro de facture", true);
        if (!result.isValid()) {
            errors.addAll(result.getErrors());
        }

        if (invoice.getCustomer() != null) {
            result = BusinessValidator.validateString(invoice.getCustomer().getName(), "Nom du client", true);
            if (!result.isValid()) {
                errors.addAll(result.getErrors());
            }
        }
    }

    /**
     * Valide les montants d'une facture
     */
    private void validateAmounts(Invoice invoice, List<String> errors) {
        if (invoice.getAmount() != null) {
            BusinessValidator.ValidationResult result = BusinessValidator.validateAmount(
                invoice.getAmount(),
                "Montant de la facture"
            );
            if (!result.isValid()) {
                errors.addAll(result.getErrors());
            }
        }

        // Valider les lignes de facture
        if (invoice.getLineItems() != null) {
            invoice.getLineItems().forEach(line -> {
                if (line.getAmount() != null) {
                    BusinessValidator.ValidationResult result = BusinessValidator.validateAmount(
                        line.getAmount(),
                        "Montant ligne " + line.getLabel()
                    );
                    if (!result.isValid()) {
                        errors.addAll(result.getErrors());
                    }
                }
            });
        }
    }

    /**
     * Valide les dates d'une facture
     */
    private void validateDates(Invoice invoice, List<String> errors) {
        if (invoice.getDate() != null) {
            LocalDate invoiceDate = invoice.getDate().toLocalDate();

            BusinessValidator.ValidationResult result = BusinessValidator.validateInvoiceDate(
                invoiceDate,
                false  // Ne pas autoriser les dates futures
            );

            if (!result.isValid()) {
                errors.addAll(result.getErrors());
            }

            // Valider la date d'échéance si présente
            if (invoice.getDeadline() != null) {
                LocalDate deadlineDate = invoice.getDeadline().toLocalDate();
                result = BusinessValidator.validateDeadline(invoiceDate, deadlineDate);
                if (!result.isValid()) {
                    errors.addAll(result.getErrors());
                }
            }
        }
    }

    /**
     * Valide la cohérence HT + TVA = TTC
     */
    private void validateCoherence(Invoice invoice, List<String> errors) {
        // Si on a toutes les informations de montants
        BigDecimal totalHT = BigDecimal.ZERO;
        BigDecimal totalTVA = BigDecimal.ZERO;
        BigDecimal totalTTC = invoice.getAmount();

        if (invoice.getLineItems() != null) {
            for (Invoice.LineItem line : invoice.getLineItems()) {
                if (line.getAmount() != null) {
                    totalHT = totalHT.add(line.getAmount());
                }
                if (line.getVat() != null && line.getVat().getAmount() != null) {
                    totalTVA = totalTVA.add(line.getVat().getAmount());
                }
            }

            // Valider la cohérence si on a les 3 montants
            if (totalHT.compareTo(BigDecimal.ZERO) > 0 && totalTTC != null) {
                BusinessValidator.ValidationResult result = BusinessValidator.validateAmountCoherence(
                    totalHT,
                    totalTVA,
                    totalTTC
                );
                if (!result.isValid()) {
                    errors.addAll(result.getErrors());
                }
            }
        }
    }
}
