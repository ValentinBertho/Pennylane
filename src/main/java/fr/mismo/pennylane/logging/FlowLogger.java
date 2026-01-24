package fr.mismo.pennylane.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logger spécialisé pour les flux de l'interface PENNYLANE.
 * Fournit des méthodes structurées pour :
 * - Les logs de début/fin de flux avec bilan
 * - Les logs d'étapes clés
 * - Les logs d'anomalies (fonctionnelles et techniques)
 *
 * Tous les messages sont formatés selon les recommandations du document
 * d'amélioration des logs PENNYLANE.
 */
@Component
public class FlowLogger {

    private static final Logger log = LoggerFactory.getLogger(FlowLogger.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SEPARATOR = "══════════════════════════════════════════════════";
    private static final String SEPARATOR_LIGHT = "──────────────────────────────────────────────────";

    // Stockage des contextes de flux avec statistiques
    private final Map<String, FlowStatistics> flowStatistics = new ConcurrentHashMap<>();

    // ═══════════════════════════════════════════════════════════════════════
    // LOGS DE DÉBUT DE FLUX
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Log le démarrage d'un flux avec génération du Correlation ID.
     *
     * @param flowType Le type de flux
     * @return Le Correlation ID généré
     */
    public String startFlow(FlowType flowType) {
        return startFlow(flowType, null);
    }

    /**
     * Log le démarrage d'un flux avec informations supplémentaires.
     *
     * @param flowType Le type de flux
     * @param additionalInfo Informations supplémentaires à afficher
     * @return Le Correlation ID généré
     */
    public String startFlow(FlowType flowType, Map<String, Object> additionalInfo) {
        String correlationId = CorrelationIdManager.generate(flowType);
        LocalDateTime now = LocalDateTime.now();

        FlowStatistics stats = new FlowStatistics(flowType, now);
        flowStatistics.put(correlationId, stats);

        String flowCode = flowType.getCode();

        log.info("[{}] {}", flowCode, SEPARATOR);
        log.info("[{}] DÉMARRAGE - {}", flowCode, flowType.getDescription());
        log.info("[{}] {}", flowCode, SEPARATOR);
        log.info("[{}] Correlation ID: {}", flowCode, correlationId);
        log.info("[{}] Heure de démarrage: {}", flowCode, now.format(TIME_FORMATTER));

        if (additionalInfo != null && !additionalInfo.isEmpty()) {
            additionalInfo.forEach((key, value) ->
                log.info("[{}] {}: {}", flowCode, key, value));
        }

        return correlationId;
    }

    /**
     * Log le démarrage d'un flux de synchronisation des écritures.
     *
     * @param nbLots Nombre de lots à traiter
     * @param nbSites Nombre de sites concernés
     * @return Le Correlation ID généré
     */
    public String startSyncEcritures(int nbLots, int nbSites) {
        String correlationId = CorrelationIdManager.generate(FlowType.SYNC_ECRITURES);
        LocalDateTime now = LocalDateTime.now();

        FlowStatistics stats = new FlowStatistics(FlowType.SYNC_ECRITURES, now);
        flowStatistics.put(correlationId, stats);

        String flowCode = FlowType.SYNC_ECRITURES.getCode();

        log.info("[{}] {}", flowCode, SEPARATOR);
        log.info("[{}] DÉMARRAGE - Synchronisation des écritures comptables", flowCode);
        log.info("[{}] {}", flowCode, SEPARATOR);
        log.info("[{}] Correlation ID: {}", flowCode, correlationId);
        log.info("[{}] Heure de démarrage: {}", flowCode, now.format(TIME_FORMATTER));
        log.info("[{}] Sites actifs: {}", flowCode, nbSites);
        log.info("[{}] Lots à traiter: {} lot(s) identifié(s)", flowCode, nbLots);

        return correlationId;
    }

    /**
     * Log le démarrage d'un flux d'import des achats.
     *
     * @param daysBackward Nombre de jours en arrière pour la recherche
     * @param statusFilters Liste des statuts filtrés
     * @param categoryFilters Liste des catégories filtrées
     * @return Le Correlation ID généré
     */
    public String startSyncAchats(int daysBackward, List<String> statusFilters, List<String> categoryFilters) {
        String correlationId = CorrelationIdManager.generate(FlowType.SYNC_ACHATS);
        LocalDateTime now = LocalDateTime.now();

        FlowStatistics stats = new FlowStatistics(FlowType.SYNC_ACHATS, now);
        flowStatistics.put(correlationId, stats);

        String flowCode = FlowType.SYNC_ACHATS.getCode();
        LocalDateTime dateDebut = now.minusDays(daysBackward);

        log.info("[{}] {}", flowCode, SEPARATOR);
        log.info("[{}] DÉMARRAGE - Import des factures fournisseurs", flowCode);
        log.info("[{}] {}", flowCode, SEPARATOR);
        log.info("[{}] Correlation ID: {}", flowCode, correlationId);
        log.info("[{}] Période de recherche: {} jours (depuis le {})", flowCode, daysBackward,
            dateDebut.format(DateTimeFormatter.ISO_LOCAL_DATE));
        log.info("[{}] Filtres actifs:", flowCode);
        log.info("[{}]   • Statuts: {}", flowCode, statusFilters);
        log.info("[{}]   • Catégories: {}", flowCode, categoryFilters);

        return correlationId;
    }

    /**
     * Log le démarrage d'un flux de mise à jour des règlements.
     *
     * @param nbSites Nombre de sites concernés
     * @return Le Correlation ID généré
     */
    public String startSyncReglements(int nbSites) {
        String correlationId = CorrelationIdManager.generate(FlowType.SYNC_REGLEMENTS);
        LocalDateTime now = LocalDateTime.now();

        FlowStatistics stats = new FlowStatistics(FlowType.SYNC_REGLEMENTS, now);
        flowStatistics.put(correlationId, stats);

        String flowCode = FlowType.SYNC_REGLEMENTS.getCode();

        log.info("[{}] {}", flowCode, SEPARATOR);
        log.info("[{}] DÉMARRAGE - Mise à jour des règlements", flowCode);
        log.info("[{}] {}", flowCode, SEPARATOR);
        log.info("[{}] Correlation ID: {}", flowCode, correlationId);
        log.info("[{}] Heure de démarrage: {}", flowCode, now.format(TIME_FORMATTER));
        log.info("[{}] Sites à traiter: {}", flowCode, nbSites);

        return correlationId;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOGS DE FIN DE FLUX
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Log la fin d'un flux avec bilan complet.
     *
     * @param correlationId L'identifiant de corrélation du flux
     */
    public void endFlow(String correlationId) {
        endFlow(correlationId, null);
    }

    /**
     * Log la fin d'un flux avec bilan et statistiques personnalisées.
     *
     * @param correlationId L'identifiant de corrélation du flux
     * @param customStats Statistiques personnalisées à afficher
     */
    public void endFlow(String correlationId, Map<String, Object> customStats) {
        FlowStatistics stats = flowStatistics.get(correlationId);
        if (stats == null) {
            log.warn("Tentative de fermeture d'un flux inconnu: {}", correlationId);
            return;
        }

        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(stats.getStartTime(), endTime);
        String durationStr = formatDuration(duration);

        FlowType flowType = stats.getFlowType();
        String flowCode = flowType.getCode();
        boolean hasErrors = stats.getErrorCount() > 0;

        // Choix du niveau de log selon le résultat
        if (hasErrors) {
            log.warn("[{}] {}", flowCode, SEPARATOR);
            log.warn("[{}] FIN - {}", flowCode, flowType.getDescription());
            log.warn("[{}] {}", flowCode, SEPARATOR);
            log.warn("[{}] Correlation ID: {}", flowCode, correlationId);
            log.warn("[{}] Durée totale: {}", flowCode, durationStr);
            log.warn("[{}] BILAN:", flowCode);
            logStatistics(flowCode, stats, customStats, true);
            log.warn("[{}] Statut final: PARTIEL - Action requise", flowCode);
        } else {
            log.info("[{}] {}", flowCode, SEPARATOR);
            log.info("[{}] FIN - {}", flowCode, flowType.getDescription());
            log.info("[{}] {}", flowCode, SEPARATOR);
            log.info("[{}] Correlation ID: {}", flowCode, correlationId);
            log.info("[{}] Durée totale: {}", flowCode, durationStr);
            log.info("[{}] BILAN:", flowCode);
            logStatistics(flowCode, stats, customStats, false);
            log.info("[{}] Statut final: SUCCÈS", flowCode);
        }

        // Nettoyage
        flowStatistics.remove(correlationId);
        CorrelationIdManager.clear();
    }

    /**
     * Log la fin d'un flux de synchronisation des écritures avec bilan détaillé.
     */
    public void endSyncEcritures(String correlationId, int lotsTraites, int lotsTotal,
                                   int facturesCrees, int facturesIgnorees,
                                   int clientsCrees, int produitsCrees,
                                   int documentsUploades, List<String> erreurs) {
        FlowStatistics stats = flowStatistics.get(correlationId);
        if (stats == null) {
            log.warn("Tentative de fermeture d'un flux inconnu: {}", correlationId);
            return;
        }

        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(stats.getStartTime(), endTime);
        String durationStr = formatDuration(duration);

        String flowCode = FlowType.SYNC_ECRITURES.getCode();
        boolean hasErrors = erreurs != null && !erreurs.isEmpty();
        int pourcentage = lotsTotal > 0 ? (lotsTraites * 100 / lotsTotal) : 100;

        if (hasErrors) {
            log.warn("[{}] {}", flowCode, SEPARATOR);
            log.warn("[{}] FIN - Synchronisation des écritures comptables", flowCode);
            log.warn("[{}] {}", flowCode, SEPARATOR);
            log.warn("[{}] Correlation ID: {}", flowCode, correlationId);
            log.warn("[{}] Durée totale: {}", flowCode, durationStr);
            log.warn("[{}] BILAN:", flowCode);
            log.warn("[{}]   • Lots traités: {}/{} ({}%)", flowCode, lotsTraites, lotsTotal, pourcentage);
            log.warn("[{}]   • Factures créées: {}", flowCode, facturesCrees);
            log.warn("[{}]   • Factures ignorées (doublons): {}", flowCode, facturesIgnorees);
            log.warn("[{}]   • Clients créés: {}", flowCode, clientsCrees);
            log.warn("[{}]   • Produits créés: {}", flowCode, produitsCrees);
            log.warn("[{}]   • Documents uploadés: {}", flowCode, documentsUploades);
            log.warn("[{}]   • Erreurs: {}", flowCode, erreurs.size());
            log.warn("[{}]   • Erreurs détaillées:", flowCode);
            for (String erreur : erreurs) {
                log.warn("[{}]     - {}", flowCode, erreur);
            }
            log.warn("[{}] Statut final: PARTIEL - Action requise", flowCode);
        } else {
            log.info("[{}] {}", flowCode, SEPARATOR);
            log.info("[{}] FIN - Synchronisation des écritures comptables", flowCode);
            log.info("[{}] {}", flowCode, SEPARATOR);
            log.info("[{}] Correlation ID: {}", flowCode, correlationId);
            log.info("[{}] Durée totale: {}", flowCode, durationStr);
            log.info("[{}] BILAN:", flowCode);
            log.info("[{}]   • Lots traités: {}/{} ({}%)", flowCode, lotsTraites, lotsTotal, pourcentage);
            log.info("[{}]   • Factures créées: {}", flowCode, facturesCrees);
            log.info("[{}]   • Factures ignorées (doublons): {}", flowCode, facturesIgnorees);
            log.info("[{}]   • Clients créés: {}", flowCode, clientsCrees);
            log.info("[{}]   • Produits créés: {}", flowCode, produitsCrees);
            log.info("[{}]   • Documents uploadés: {}", flowCode, documentsUploades);
            log.info("[{}]   • Erreurs: 0", flowCode);
            log.info("[{}] Statut final: SUCCÈS", flowCode);
        }

        // Nettoyage
        flowStatistics.remove(correlationId);
        CorrelationIdManager.clear();
    }

    /**
     * Log la fin d'un flux d'import des achats avec bilan détaillé.
     */
    public void endSyncAchats(String correlationId, int facturesRecuperees, int facturesRetenues,
                               int facturesImportees, int facturesIgnorees,
                               int fournisseursCrees, int documentsTelechargees) {
        FlowStatistics stats = flowStatistics.get(correlationId);
        if (stats == null) {
            log.warn("Tentative de fermeture d'un flux inconnu: {}", correlationId);
            return;
        }

        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(stats.getStartTime(), endTime);
        String durationStr = formatDuration(duration);

        String flowCode = FlowType.SYNC_ACHATS.getCode();
        boolean hasErrors = stats.getErrorCount() > 0;

        if (hasErrors) {
            log.warn("[{}] {}", flowCode, SEPARATOR);
            log.warn("[{}] FIN - Import des factures fournisseurs", flowCode);
            log.warn("[{}] {}", flowCode, SEPARATOR);
        } else {
            log.info("[{}] {}", flowCode, SEPARATOR);
            log.info("[{}] FIN - Import des factures fournisseurs", flowCode);
            log.info("[{}] {}", flowCode, SEPARATOR);
        }

        log.info("[{}] Correlation ID: {}", flowCode, correlationId);
        log.info("[{}] Durée totale: {}", flowCode, durationStr);
        log.info("[{}] BILAN:", flowCode);
        log.info("[{}]   • Factures récupérées (brut): {}", flowCode, facturesRecuperees);
        log.info("[{}]   • Factures après filtrage: {}", flowCode, facturesRetenues);
        log.info("[{}]   • Factures importées: {}", flowCode, facturesImportees);
        log.info("[{}]   • Factures ignorées (existantes): {}", flowCode, facturesIgnorees);
        log.info("[{}]   • Fournisseurs créés: {}", flowCode, fournisseursCrees);
        log.info("[{}]   • Documents téléchargés: {}", flowCode, documentsTelechargees);
        log.info("[{}] Statut final: {}", flowCode, hasErrors ? "PARTIEL" : "SUCCÈS");

        // Nettoyage
        flowStatistics.remove(correlationId);
        CorrelationIdManager.clear();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOGS D'ÉTAPES CLÉS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Log le début du traitement d'une facture.
     */
    public void startFacture(FlowType flowType, String numeroFacture) {
        String flowCode = flowType.getCode();
        log.info("[{}] {}", flowCode, SEPARATOR_LIGHT);
        log.info("[{}] Traitement facture: {}", flowCode, numeroFacture);
        log.info("[{}] {}", flowCode, SEPARATOR_LIGHT);
    }

    /**
     * Log une étape de validation réussie.
     */
    public void logValidation(FlowType flowType, String reference, String validation, boolean success) {
        String flowCode = flowType.getCode();
        String status = success ? "OK" : "KO";
        log.debug("[{}] [{}] {}... {}", flowCode, reference, validation, status);
    }

    /**
     * Log les détails de validation des montants.
     */
    public void logValidationMontants(FlowType flowType, String reference,
                                        double montantHT, double montantTVA, double montantTTC) {
        String flowCode = flowType.getCode();
        log.debug("[{}] [{}] Validation montants... OK (HT: {}€, TVA: {}€, TTC: {}€)",
            flowCode, reference,
            String.format("%.2f", montantHT),
            String.format("%.2f", montantTVA),
            String.format("%.2f", montantTTC));
    }

    /**
     * Log une information sur le client.
     */
    public void logClient(FlowType flowType, String reference, String nomClient, String siret, boolean existe) {
        String flowCode = flowType.getCode();
        log.info("[{}] [{}] Client: {} (SIRET: {})", flowCode, reference, nomClient, siret);
        if (existe) {
            log.debug("[{}] [{}] Client existant dans Pennylane", flowCode, reference);
        } else {
            log.info("[{}] [{}] Nouveau client créé dans Pennylane", flowCode, reference);
        }
    }

    /**
     * Log le traitement des produits.
     */
    public void logProduits(FlowType flowType, String reference, int nbProduits, int nbCrees) {
        String flowCode = flowType.getCode();
        log.info("[{}] [{}] Produits: {} article(s) à synchroniser", flowCode, reference, nbProduits);
        if (nbCrees > 0) {
            log.debug("[{}] [{}] {} produit(s) créé(s) dans Pennylane", flowCode, reference, nbCrees);
        }
    }

    /**
     * Log l'upload d'un document.
     */
    public void logDocument(FlowType flowType, String reference, String nomFichier, boolean success) {
        String flowCode = flowType.getCode();
        if (success) {
            log.info("[{}] [{}] Document PDF: {} (téléversé avec succès)", flowCode, reference, nomFichier);
        } else {
            log.warn("[{}] [{}] Document PDF: {} (échec du téléversement)", flowCode, reference, nomFichier);
        }
    }

    /**
     * Log la création réussie d'une facture.
     */
    public void logFactureCreee(FlowType flowType, String reference, String pennylaneId, long dureeMs) {
        String flowCode = flowType.getCode();
        log.info("[{}] [{}] ✓ Facture créée dans Pennylane (ID: {})", flowCode, reference, pennylaneId);
        log.info("[{}] [{}] Durée de traitement: {} sec", flowCode, reference,
            String.format("%.3f", dureeMs / 1000.0));

        // Mise à jour des statistiques
        String correlationId = CorrelationIdManager.getCurrent();
        if (correlationId != null) {
            FlowStatistics stats = flowStatistics.get(correlationId);
            if (stats != null) {
                stats.incrementSuccess();
            }
        }
    }

    /**
     * Log un règlement.
     */
    public void logReglement(FlowType flowType, String reference, String dateReglement,
                              double montant, String moyenPaiement) {
        String flowCode = flowType.getCode();
        log.info("[{}] [{}]   • {}: +{}€ ({})", flowCode, reference, dateReglement,
            String.format("%.2f", montant), moyenPaiement);
    }

    /**
     * Log le statut de paiement final.
     */
    public void logStatutPaiement(FlowType flowType, String reference, String statut,
                                   double montantPaye, double montantTotal) {
        String flowCode = flowType.getCode();
        log.info("[{}] [{}] Montant total: {}€ / {}€", flowCode, reference,
            String.format("%.2f", montantPaye), String.format("%.2f", montantTotal));
        log.info("[{}] [{}] ✓ Statut: {}", flowCode, reference, statut);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOGS D'ANOMALIES FONCTIONNELLES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Log une anomalie d'incohérence de montants.
     */
    public void warnIncoherenceMontants(FlowType flowType, String reference,
                                         double montantHT, double montantTVA, double montantTTC, double ecart) {
        String flowCode = flowType.getCode();
        log.warn("[{}] [{}] ⚠ Incohérence montants détectée", flowCode, reference);
        log.warn("[{}] [{}]   HT ({}€) + TVA ({}€) ≠ TTC ({}€)", flowCode, reference,
            String.format("%.2f", montantHT),
            String.format("%.2f", montantTVA),
            String.format("%.2f", montantTTC));
        log.warn("[{}] [{}]   Écart: {}€ - Traitement poursuivi avec montants d'origine",
            flowCode, reference, String.format("%.2f", ecart));
    }

    /**
     * Log un SIRET invalide.
     */
    public void warnSiretInvalide(FlowType flowType, String reference, String nomClient, String siret) {
        String flowCode = flowType.getCode();
        log.warn("[{}] [{}] ⚠ SIRET invalide pour client \"{}\"", flowCode, reference, nomClient);
        log.warn("[{}] [{}]   SIRET fourni: {} (format incorrect)", flowCode, reference, siret);
        log.warn("[{}] [{}]   Client créé sans SIRET - Correction manuelle requise", flowCode, reference);
    }

    /**
     * Log un surpaiement détecté.
     */
    public void warnSurpaiement(FlowType flowType, String reference,
                                 double montantFacture, double montantEncaisse, double excedent) {
        String flowCode = flowType.getCode();
        log.warn("[{}] [{}] ⚠ Surpaiement détecté", flowCode, reference);
        log.warn("[{}] [{}]   Montant facture: {}€", flowCode, reference, String.format("%.2f", montantFacture));
        log.warn("[{}] [{}]   Montant encaissé: {}€", flowCode, reference, String.format("%.2f", montantEncaisse));
        log.warn("[{}] [{}]   Excédent: {}€ - Avoir à créer", flowCode, reference, String.format("%.2f", excedent));
    }

    /**
     * Log un document indisponible.
     */
    public void warnDocumentIndisponible(FlowType flowType, String reference, String refCourrier) {
        String flowCode = flowType.getCode();
        log.warn("[{}] [{}] ⚠ Document PDF indisponible (réf: {}) - Facture créée sans pièce jointe",
            flowCode, reference, refCourrier);
    }

    /**
     * Log une facture ignorée (doublon).
     */
    public void infoFactureIgnoree(FlowType flowType, String reference, String raison) {
        String flowCode = flowType.getCode();
        log.info("[{}] [{}] Facture ignorée: {}", flowCode, reference, raison);

        // Mise à jour des statistiques
        String correlationId = CorrelationIdManager.getCurrent();
        if (correlationId != null) {
            FlowStatistics stats = flowStatistics.get(correlationId);
            if (stats != null) {
                stats.incrementSkipped();
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOGS D'ERREURS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Log une erreur de montant invalide.
     */
    public void errorMontantInvalide(FlowType flowType, String reference, double montant, String raison) {
        String flowCode = flowType.getCode();
        log.error("[{}] [{}] ✗ Facture rejetée - Montant invalide", flowCode, reference);
        log.error("[{}] [{}]   Cause: {}", flowCode, reference, raison);
        log.error("[{}] [{}]   Valeur actuelle: {}€", flowCode, reference, String.format("%.2f", montant));
        log.error("[{}] [{}]   Action: Corriger le montant dans ATHENEO puis relancer la synchronisation",
            flowCode, reference);

        incrementErrorCount();
    }

    /**
     * Log une erreur de client manquant.
     */
    public void errorClientManquant(FlowType flowType, String reference) {
        String flowCode = flowType.getCode();
        log.error("[{}] [{}] ✗ Facture non créée - Client obligatoire manquant", flowCode, reference);
        log.error("[{}] [{}]   NO_SOCIETE: null", flowCode, reference);
        log.error("[{}] [{}]   Action: Associer un client à la facture", flowCode, reference);

        incrementErrorCount();
    }

    /**
     * Log une erreur de fournisseur non identifiable.
     */
    public void errorFournisseurNonIdentifiable(FlowType flowType, String reference,
                                                  String nomFournisseur, String pennylaneId, String siret) {
        String flowCode = flowType.getCode();
        log.error("[{}] [{}] ✗ Facture non importée - Fournisseur non identifiable", flowCode, reference);
        log.error("[{}] [{}]   Fournisseur Pennylane: \"{}\" (ID: {})", flowCode, reference, nomFournisseur, pennylaneId);
        log.error("[{}] [{}]   SIRET: {}", flowCode, reference, siret);
        log.error("[{}] [{}]   Aucune correspondance trouvée dans ATHENEO", flowCode, reference);
        log.error("[{}] [{}]   Action: Créer le fournisseur dans ATHENEO avec ce SIRET, ou corriger le SIRET dans Pennylane",
            flowCode, reference);

        incrementErrorCount();
    }

    /**
     * Log une erreur API Pennylane.
     */
    public void errorApiPennylane(FlowType flowType, String reference, int codeHttp, String messageApi) {
        String flowCode = flowType.getCode();
        log.error("[{}] [{}] ✗ Erreur API Pennylane", flowCode, reference);
        log.error("[{}] [{}]   Code HTTP: {} ({})", flowCode, reference, codeHttp, getHttpStatusDescription(codeHttp));
        log.error("[{}] [{}]   Message API: \"{}\"", flowCode, reference, messageApi);

        incrementErrorCount();
    }

    /**
     * Log une erreur de connexion.
     */
    public void errorConnexion(FlowType flowType, String typeErreur, int tentative, int maxTentatives) {
        String flowCode = flowType.getCode();
        log.error("[{}] ✗ Erreur de connexion API Pennylane", flowCode);
        log.error("[{}]   Type: {}", flowCode, typeErreur);
        log.error("[{}]   Tentative: {}/{}", flowCode, tentative, maxTentatives);
        if (tentative < maxTentatives) {
            int delai = (int) Math.pow(2, tentative);
            log.error("[{}]   Prochaine tentative dans: {} secondes", flowCode, delai);
        }
    }

    /**
     * Log un circuit breaker ouvert.
     */
    public void warnCircuitBreakerOpen(FlowType flowType, String nomCircuitBreaker, String raison, int dureeAttente) {
        String flowCode = flowType.getCode();
        log.warn("[{}] Circuit Breaker \"{}\" passé en état OPEN", flowCode, nomCircuitBreaker);
        log.warn("[{}]   Raison: {}", flowCode, raison);
        log.warn("[{}]   Durée d'attente: {} secondes", flowCode, dureeAttente);
        log.warn("[{}]   Impact: Appels API temporairement bloqués", flowCode);
    }

    /**
     * Log un deadlock SQL.
     */
    public void warnDeadlock(FlowType flowType, String procedure, int tentative, int maxTentatives) {
        String flowCode = flowType.getCode();
        log.warn("[{}] ⚠ Conflit d'accès base de données - nouvelle tentative automatique ({}/{})",
            flowCode, tentative, maxTentatives);
        log.debug("[{}]   Procédure: {}", flowCode, procedure);
    }

    /**
     * Log la résolution d'un deadlock.
     */
    public void infoDeadlockResolu(FlowType flowType, int tentatives, long dureeMs) {
        String flowCode = flowType.getCode();
        log.info("[{}] Deadlock résolu après {} tentatives", flowCode, tentatives);
        log.info("[{}] Durée de résolution: {}ms", flowCode, dureeMs);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MÉTHODES UTILITAIRES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Incrémente le compteur d'erreurs du flux courant.
     */
    private void incrementErrorCount() {
        String correlationId = CorrelationIdManager.getCurrent();
        if (correlationId != null) {
            FlowStatistics stats = flowStatistics.get(correlationId);
            if (stats != null) {
                stats.incrementError();
            }
        }
    }

    /**
     * Formate une durée de manière lisible.
     */
    private String formatDuration(Duration duration) {
        long totalSeconds = duration.getSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long millis = duration.toMillisPart();

        if (minutes > 0) {
            return String.format("%d min %d sec", minutes, seconds);
        } else if (seconds > 0) {
            return String.format("%.3f secondes", seconds + millis / 1000.0);
        } else {
            return String.format("%d ms", millis);
        }
    }

    /**
     * Log les statistiques avec le bon niveau de log.
     */
    private void logStatistics(String flowCode, FlowStatistics stats,
                                Map<String, Object> customStats, boolean hasErrors) {
        if (hasErrors) {
            log.warn("[{}]   • Traités avec succès: {}", flowCode, stats.getSuccessCount());
            log.warn("[{}]   • Ignorés: {}", flowCode, stats.getSkippedCount());
            log.warn("[{}]   • En erreur: {}", flowCode, stats.getErrorCount());
        } else {
            log.info("[{}]   • Traités avec succès: {}", flowCode, stats.getSuccessCount());
            log.info("[{}]   • Ignorés: {}", flowCode, stats.getSkippedCount());
            log.info("[{}]   • En erreur: {}", flowCode, stats.getErrorCount());
        }

        if (customStats != null) {
            customStats.forEach((key, value) -> {
                if (hasErrors) {
                    log.warn("[{}]   • {}: {}", flowCode, key, value);
                } else {
                    log.info("[{}]   • {}: {}", flowCode, key, value);
                }
            });
        }
    }

    /**
     * Retourne une description du code HTTP.
     */
    private String getHttpStatusDescription(int code) {
        return switch (code) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 422 -> "Unprocessable Entity";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            default -> "Unknown";
        };
    }

    /**
     * Récupère les statistiques d'un flux.
     */
    public FlowStatistics getStatistics(String correlationId) {
        return flowStatistics.get(correlationId);
    }

    /**
     * Classe interne pour les statistiques de flux.
     */
    public static class FlowStatistics {
        private final FlowType flowType;
        private final LocalDateTime startTime;
        private int successCount = 0;
        private int errorCount = 0;
        private int skippedCount = 0;
        private final List<String> errors = new ArrayList<>();

        public FlowStatistics(FlowType flowType, LocalDateTime startTime) {
            this.flowType = flowType;
            this.startTime = startTime;
        }

        public FlowType getFlowType() { return flowType; }
        public LocalDateTime getStartTime() { return startTime; }
        public int getSuccessCount() { return successCount; }
        public int getErrorCount() { return errorCount; }
        public int getSkippedCount() { return skippedCount; }
        public List<String> getErrors() { return errors; }

        public void incrementSuccess() { successCount++; }
        public void incrementError() { errorCount++; }
        public void incrementSkipped() { skippedCount++; }
        public void addError(String error) { errors.add(error); }
    }
}
