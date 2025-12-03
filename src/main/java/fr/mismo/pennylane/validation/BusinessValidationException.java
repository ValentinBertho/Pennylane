package fr.mismo.pennylane.validation;

import java.util.List;

/**
 * Exception levée lors d'une erreur de validation métier
 *
 * <h3>Usage</h3>
 * <p>Cette exception est levée automatiquement par {@link BusinessValidationAspect}
 * lorsque des validations métier échouent.</p>
 *
 * <h3>Informations contenues</h3>
 * <ul>
 *   <li>Message global</li>
 *   <li>Liste détaillée de toutes les erreurs de validation</li>
 * </ul>
 *
 * <h3>Exemple de gestion</h3>
 * <pre>
 * {@code
 * try {
 *     invoiceService.createInvoice(invoice);
 * } catch (BusinessValidationException e) {
 *     log.error("Erreurs de validation: {}", e.getValidationErrors());
 *     // Retourner les erreurs à l'utilisateur
 *     return ResponseEntity.badRequest().body(e.getValidationErrors());
 * }
 * }
 * </pre>
 *
 * @see BusinessValidationAspect
 * @see ValidateBusinessRules
 * @author Interface Pennylane
 * @since 1.10.2
 */
public class BusinessValidationException extends RuntimeException {

    private final List<String> validationErrors;

    /**
     * Constructeur avec message et liste d'erreurs
     *
     * @param message Message global
     * @param validationErrors Liste détaillée des erreurs de validation
     */
    public BusinessValidationException(String message, List<String> validationErrors) {
        super(message + ": " + String.join("; ", validationErrors));
        this.validationErrors = validationErrors;
    }

    /**
     * Retourne la liste des erreurs de validation
     *
     * @return Liste des messages d'erreur
     */
    public List<String> getValidationErrors() {
        return validationErrors;
    }

    /**
     * Retourne une représentation formatée des erreurs
     *
     * @return String avec toutes les erreurs séparées par des sauts de ligne
     */
    public String getFormattedErrors() {
        return String.join("\n", validationErrors);
    }

    /**
     * Retourne le nombre d'erreurs de validation
     *
     * @return Nombre d'erreurs
     */
    public int getErrorCount() {
        return validationErrors.size();
    }
}
