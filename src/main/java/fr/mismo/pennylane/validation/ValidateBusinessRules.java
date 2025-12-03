package fr.mismo.pennylane.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour déclencher les validations métier automatiques sur une méthode
 *
 * <h3>Usage</h3>
 * <pre>
 * {@code
 * @ValidateBusinessRules
 * public InvoiceResponse createInvoice(Invoice invoice) {
 *     // La validation sera appliquée avant l'exécution
 * }
 * }
 * </pre>
 *
 * <h3>Validations appliquées</h3>
 * <ul>
 *   <li>Validation des montants (HT, TVA, TTC)</li>
 *   <li>Validation des dates (facture, échéance)</li>
 *   <li>Validation des identifiants (SIRET, SIREN)</li>
 *   <li>Validation des champs obligatoires</li>
 * </ul>
 *
 * @see BusinessValidationAspect
 * @see BusinessValidator
 * @author Interface Pennylane
 * @since 1.10.2
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateBusinessRules {

    /**
     * Types de validations à ignorer
     * @return Tableau des types de validations à ne pas appliquer
     */
    ValidationType[] exclude() default {};

    /**
     * Type de validation métier
     */
    enum ValidationType {
        AMOUNTS,      // Validation des montants
        DATES,        // Validation des dates
        IDENTIFIERS,  // SIRET, SIREN
        REQUIRED_FIELDS,  // Champs obligatoires
        COHERENCE     // Cohérence des données (HT+TVA=TTC)
    }
}
