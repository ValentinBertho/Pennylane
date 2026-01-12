package fr.mismo.pennylane.service;

import fr.mismo.pennylane.dao.repository.LogRepository;
import fr.mismo.pennylane.util.RetryHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service dédié pour la gestion des logs avec transactions séparées
 * pour éviter les deadlocks
 */
@Service
@Slf4j
public class LogService {

    @Autowired
    private LogRepository logRepository;

    /**
     * Ajoute une ligne au forum avec retry automatique en cas de deadlock
     * Utilise une transaction séparée (REQUIRES_NEW) pour éviter d'impacter
     * la transaction parente en cas d'échec
     *
     * @param entite Type d'entité (V_FACTURE, A_FACTURE, etc.)
     * @param noEntite Numéro de l'entité
     * @param message Message à logger
     * @param niveau Niveau de log (1=Error, 2=Warning, 3=Info, etc.)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ajouterLigneForumSafe(String entite, String noEntite, String message, Integer niveau) {
        RetryHelper.executeWithRetryVoid(
                () -> logRepository.ajouterLigneForum(entite, noEntite, message, niveau),
                "ajouterLigneForum[" + entite + ":" + noEntite + "]"
        );
    }

    /**
     * Traite un lot avec retry automatique
     *
     * @param noEcritureLot Numéro du lot
     * @param message Message de traitement
     * @param succes Indicateur de succès
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void traiterLotSafe(Integer noEcritureLot, String message, Boolean succes) {
        RetryHelper.executeWithRetryVoid(
                () -> logRepository.traiterLot(noEcritureLot, message, succes),
                "traiterLot[" + noEcritureLot + "]"
        );
    }

    /**
     * Traite une facture avec retry automatique
     *
     * @param noVFacture Numéro de facture
     * @param idPennylane ID Pennylane
     * @param idPennylaneV2 ID Pennylane V2
     * @param succes Indicateur de succès
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void traiterFactureSafe(Integer noVFacture, String idPennylane, String idPennylaneV2, Boolean succes) {
        RetryHelper.executeWithRetryVoid(
                () -> logRepository.traiterFacture(noVFacture, idPennylane, idPennylaneV2, succes),
                "traiterFacture[" + noVFacture + "]"
        );
    }

    /**
     * Met à jour les règlements d'une facture fournisseur avec retry automatique
     * Utilise une transaction séparée (REQUIRES_NEW) pour éviter les problèmes
     * de transaction corrompue
     *
     * @param paid Indicateur de paiement
     * @param paymentStatus Statut de paiement
     * @param remainingAmount Montant restant
     * @param fullyPaidAt Date de paiement complet
     * @param currencyAmount Montant total
     * @param invoiceId ID de la facture
     * @param codSite Code du site
     * @return Code retour de la procédure stockée
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int majSupplierInvoiceReglementSafe(Boolean paid, String paymentStatus,
                                                Double remainingAmount, String fullyPaidAt,
                                                Double currencyAmount, String invoiceId, String codSite) {
        return RetryHelper.executeWithRetry(
                () -> logRepository.majSupplierInvoiceReglement(
                        paid, paymentStatus, remainingAmount, fullyPaidAt,
                        currencyAmount, invoiceId, codSite),
                "majSupplierInvoiceReglement[" + invoiceId + "]"
        );
    }
}
