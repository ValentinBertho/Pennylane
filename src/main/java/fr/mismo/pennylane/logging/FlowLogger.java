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
 *
 * Principe : ne loguer QUE quand quelque chose se passe.
 * - Rien à traiter → une seule ligne TRACE (invisible en production)
 * - Éléments traités → bilan structuré complet
 * - Erreurs → toujours loguées
 */
@Component
public class FlowLogger {

    private static final Logger log = LoggerFactory.getLogger(FlowLogger.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SEPARATOR = "══════════════════════════════════════════════════";

    // Stockage des contextes de flux avec statistiques
    private final Map<String, FlowContext> flowContexts = new ConcurrentHashMap<>();

    // ═══════════════════════════════════════════════════════════════════════
    // DÉMARRAGE DE FLUX (silencieux : on attend de savoir s'il y a du travail)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Prépare un flux. Aucun log n'est émis à ce stade.
     * Le log de démarrage n'apparaîtra que si le flux traite des éléments.
     */
    public String startFlow(FlowType flowType) {
        return startFlow(flowType, null);
    }

    /**
     * Prépare un flux avec informations supplémentaires.
     * Le log de démarrage est différé jusqu'au premier élément traité.
     */
    public String startFlow(FlowType flowType, Map<String, Object> additionalInfo) {
        String correlationId = CorrelationIdManager.generate(flowType);
        LocalDateTime now = LocalDateTime.now();

        FlowContext ctx = new FlowContext(flowType, now, additionalInfo);
        flowContexts.put(correlationId, ctx);

        return correlationId;
    }

    // Alias conservés pour compatibilité
    public String startSyncEcritures(int nbLots, int nbSites) {
        String correlationId = startFlow(FlowType.SYNC_ECRITURES,
                Map.of("Sites actifs", nbSites, "Lots à traiter", nbLots));
        return correlationId;
    }

    public String startSyncAchats(int daysBackward, List<String> statusFilters, List<String> categoryFilters) {
        LocalDateTime dateDebut = LocalDateTime.now().minusDays(daysBackward);
        return startFlow(FlowType.SYNC_ACHATS,
                Map.of("Période", daysBackward + " jours (depuis le " + dateDebut.format(DateTimeFormatter.ISO_LOCAL_DATE) + ")",
                        "Statuts", statusFilters.toString(),
                        "Catégories", categoryFilters.toString()));
    }

    public String startSyncReglements(int nbSites) {
        return startFlow(FlowType.SYNC_REGLEMENTS, Map.of("Sites à traiter", nbSites));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FIN DE FLUX (conditionnel : bilan complet seulement si travail effectué)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Clôture un flux. Si rien n'a été traité, une simple ligne TRACE est émise.
     * Sinon, un bilan complet est affiché.
     */
    public void endFlow(String correlationId) {
        endFlow(correlationId, null);
    }

    /**
     * Clôture un flux avec statistiques personnalisées.
     */
    public void endFlow(String correlationId, Map<String, Object> customStats) {
        FlowContext ctx = flowContexts.remove(correlationId);
        if (ctx == null) {
            log.warn("Tentative de fermeture d'un flux inconnu: {}", correlationId);
            return;
        }

        CorrelationIdManager.clear();
        FlowType flowType = ctx.getFlowType();
        String flowCode = flowType.getCode();

        // Si aucun travail n'a été fait, une seule ligne trace suffit
        if (!ctx.hasActivity()) {
            log.trace("[{}] Aucun élément à traiter", flowCode);
            return;
        }

        // Il y a eu du travail : émettre le bilan complet
        Duration duration = Duration.between(ctx.getStartTime(), LocalDateTime.now());
        String durationStr = formatDuration(duration);
        boolean hasErrors = ctx.getErrorCount() > 0;

        // Log de démarrage différé (on sait maintenant que c'était utile)
        logFlowHeader(flowCode, "BILAN", flowType.getDescription(), hasErrors);
        logLine(flowCode, "Correlation ID: " + correlationId, hasErrors);
        logLine(flowCode, "Durée totale: " + durationStr, hasErrors);

        // Statistiques du contexte
        if (ctx.getSuccessCount() > 0 || ctx.getSkippedCount() > 0 || ctx.getErrorCount() > 0) {
            logLine(flowCode, "  Traités: " + ctx.getSuccessCount(), hasErrors);
            if (ctx.getSkippedCount() > 0)
                logLine(flowCode, "  Ignorés: " + ctx.getSkippedCount(), hasErrors);
            if (ctx.getErrorCount() > 0)
                logLine(flowCode, "  Erreurs: " + ctx.getErrorCount(), hasErrors);
        }

        // Statistiques personnalisées
        if (customStats != null) {
            customStats.forEach((key, value) ->
                    logLine(flowCode, "  " + key + ": " + value, hasErrors));
        }

        logLine(flowCode, "Statut: " + (hasErrors ? "PARTIEL" : "SUCCÈS"), hasErrors);
    }

    /**
     * Clôture un flux de synchronisation des écritures avec bilan détaillé.
     */
    public void endSyncEcritures(String correlationId, int lotsTraites, int lotsTotal,
                                 int facturesCrees, int facturesIgnorees,
                                 int clientsCrees, int produitsCrees,
                                 int documentsUploades, List<String> erreurs) {
        FlowContext ctx = flowContexts.remove(correlationId);
        if (ctx == null) return;
        CorrelationIdManager.clear();

        String flowCode = FlowType.SYNC_ECRITURES.getCode();

        // Rien traité, rien à dire
        if (lotsTraites == 0 && (erreurs == null || erreurs.isEmpty())) {
            log.trace("[{}] Aucun lot traité", flowCode);
            return;
        }

        Duration duration = Duration.between(ctx.getStartTime(), LocalDateTime.now());
        boolean hasErrors = erreurs != null && !erreurs.isEmpty();
        int pourcentage = lotsTotal > 0 ? (lotsTraites * 100 / lotsTotal) : 100;

        logFlowHeader(flowCode, "BILAN", "Synchronisation des écritures comptables", hasErrors);
        logLine(flowCode, "Correlation ID: " + correlationId, hasErrors);
        logLine(flowCode, "Durée: " + formatDuration(duration), hasErrors);
        logLine(flowCode, "  Lots traités: " + lotsTraites + "/" + lotsTotal + " (" + pourcentage + "%)", hasErrors);
        if (facturesCrees > 0)
            logLine(flowCode, "  Factures créées: " + facturesCrees, hasErrors);
        if (facturesIgnorees > 0)
            logLine(flowCode, "  Factures ignorées: " + facturesIgnorees, hasErrors);
        if (clientsCrees > 0)
            logLine(flowCode, "  Clients créés: " + clientsCrees, hasErrors);
        if (produitsCrees > 0)
            logLine(flowCode, "  Produits créés: " + produitsCrees, hasErrors);
        if (documentsUploades > 0)
            logLine(flowCode, "  Documents uploadés: " + documentsUploades, hasErrors);
        if (hasErrors) {
            logLine(flowCode, "  Erreurs: " + erreurs.size(), true);
            for (String erreur : erreurs) {
                log.warn("[{}]     - {}", flowCode, erreur);
            }
        }
        logLine(flowCode, "Statut: " + (hasErrors ? "PARTIEL" : "SUCCÈS"), hasErrors);
    }

    /**
     * Clôture un flux d'import des achats avec bilan détaillé.
     */
    public void endSyncAchats(String correlationId, int facturesRecuperees, int facturesRetenues,
                              int facturesImportees, int facturesIgnorees,
                              int fournisseursCrees, int documentsTelechargees) {
        FlowContext ctx = flowContexts.remove(correlationId);
        if (ctx == null) return;
        CorrelationIdManager.clear();

        String flowCode = FlowType.SYNC_ACHATS.getCode();

        // Rien récupéré, rien à dire
        if (facturesRecuperees == 0) {
            log.trace("[{}] Aucune facture fournisseur récupérée", flowCode);
            return;
        }

        Duration duration = Duration.between(ctx.getStartTime(), LocalDateTime.now());
        boolean hasErrors = ctx.getErrorCount() > 0;

        logFlowHeader(flowCode, "BILAN", "Import des factures fournisseurs", hasErrors);
        logLine(flowCode, "Correlation ID: " + correlationId, hasErrors);
        logLine(flowCode, "Durée: " + formatDuration(duration), hasErrors);
        logLine(flowCode, "  Factures récupérées: " + facturesRecuperees, hasErrors);
        if (facturesRetenues != facturesRecuperees)
            logLine(flowCode, "  Factures après filtrage: " + facturesRetenues, hasErrors);
        if (facturesImportees > 0)
            logLine(flowCode, "  Factures importées: " + facturesImportees, hasErrors);
        if (facturesIgnorees > 0)
            logLine(flowCode, "  Factures ignorées: " + facturesIgnorees, hasErrors);
        if (fournisseursCrees > 0)
            logLine(flowCode, "  Fournisseurs créés: " + fournisseursCrees, hasErrors);
        if (documentsTelechargees > 0)
            logLine(flowCode, "  Documents téléchargés: " + documentsTelechargees, hasErrors);
        logLine(flowCode, "Statut: " + (hasErrors ? "PARTIEL" : "SUCCÈS"), hasErrors);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOGS D'ÉTAPES CLÉS (émis seulement quand un événement a lieu)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Log la création réussie d'une facture.
     */
    public void logFactureCreee(FlowType flowType, String reference, String pennylaneId, long dureeMs) {
        log.info("[{}] [{}] Facture créée (Pennylane ID: {}, {}ms)", flowType.getCode(), reference, pennylaneId, dureeMs);
        markActivity();
        incrementSuccess();
    }

    /**
     * Log le statut de paiement d'une facture.
     */
    public void logStatutPaiement(FlowType flowType, String reference, String statut,
                                  double montantPaye, double montantTotal) {
        log.info("[{}] [{}] {} - {}/{}€", flowType.getCode(), reference, statut,
                String.format("%.2f", montantPaye), String.format("%.2f", montantTotal));
        markActivity();
    }

    /**
     * Log une facture ignorée (doublon, existante, etc.).
     */
    public void infoFactureIgnoree(FlowType flowType, String reference, String raison) {
        log.debug("[{}] [{}] Ignorée: {}", flowType.getCode(), reference, raison);
        markActivity();
        incrementSkipped();
    }

    /**
     * Log un nouveau client créé.
     */
    public void logClient(FlowType flowType, String reference, String nomClient, String siret, boolean existe) {
        if (!existe) {
            log.info("[{}] [{}] Nouveau client créé: {} (SIRET: {})", flowType.getCode(), reference, nomClient, siret);
            markActivity();
        }
    }

    /**
     * Log le traitement des produits (seulement si des créations).
     */
    public void logProduits(FlowType flowType, String reference, int nbProduits, int nbCrees) {
        if (nbCrees > 0) {
            log.info("[{}] [{}] {} produit(s) créé(s) sur {}", flowType.getCode(), reference, nbCrees, nbProduits);
            markActivity();
        }
    }

    /**
     * Log l'upload d'un document (succès ou échec).
     */
    public void logDocument(FlowType flowType, String reference, String nomFichier, boolean success) {
        if (success) {
            log.debug("[{}] [{}] Document uploadé: {}", flowType.getCode(), reference, nomFichier);
        } else {
            log.warn("[{}] [{}] Échec upload document: {}", flowType.getCode(), reference, nomFichier);
        }
    }

    /**
     * Log un règlement.
     */
    public void logReglement(FlowType flowType, String reference, String dateReglement,
                             double montant, String moyenPaiement) {
        log.debug("[{}] [{}] Règlement {} : +{}€ ({})", flowType.getCode(), reference,
                dateReglement, String.format("%.2f", montant), moyenPaiement);
    }

    /**
     * Log une étape de validation.
     */
    public void logValidation(FlowType flowType, String reference, String validation, boolean success) {
        if (!success) {
            log.warn("[{}] [{}] Validation KO: {}", flowType.getCode(), reference, validation);
        }
    }

    /**
     * Log les détails de validation des montants (seulement si incohérence).
     */
    public void logValidationMontants(FlowType flowType, String reference,
                                      double montantHT, double montantTVA, double montantTTC) {
        // Seulement en debug, pas de bruit en production
        log.debug("[{}] [{}] Montants: HT={}€, TVA={}€, TTC={}€", flowType.getCode(), reference,
                String.format("%.2f", montantHT), String.format("%.2f", montantTVA), String.format("%.2f", montantTTC));
    }

    /**
     * Log le début du traitement d'une facture (supprimé - trop verbeux).
     * Conservé pour compatibilité mais ne fait rien.
     */
    public void startFacture(FlowType flowType, String numeroFacture) {
        // Intentionnellement vide - les logs par facture sont émis au résultat, pas au début
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOGS D'ANOMALIES (toujours émis - ce sont des événements significatifs)
    // ═══════════════════════════════════════════════════════════════════════

    public void warnIncoherenceMontants(FlowType flowType, String reference,
                                        double montantHT, double montantTVA, double montantTTC, double ecart) {
        log.warn("[{}] [{}] Incohérence montants: HT({})+TVA({})!=TTC({}), écart={}€",
                flowType.getCode(), reference,
                String.format("%.2f", montantHT), String.format("%.2f", montantTVA),
                String.format("%.2f", montantTTC), String.format("%.2f", ecart));
        markActivity();
    }

    public void warnSiretInvalide(FlowType flowType, String reference, String nomClient, String siret) {
        log.warn("[{}] [{}] SIRET invalide pour \"{}\" ({}), client créé sans SIRET",
                flowType.getCode(), reference, nomClient, siret);
        markActivity();
    }

    public void warnSurpaiement(FlowType flowType, String reference,
                                double montantFacture, double montantEncaisse, double excedent) {
        log.warn("[{}] [{}] Surpaiement: facture={}€, encaissé={}€, excédent={}€",
                flowType.getCode(), reference,
                String.format("%.2f", montantFacture), String.format("%.2f", montantEncaisse),
                String.format("%.2f", excedent));
        markActivity();
    }

    public void warnDocumentIndisponible(FlowType flowType, String reference, String refCourrier) {
        log.warn("[{}] [{}] Document PDF indisponible (réf: {})", flowType.getCode(), reference, refCourrier);
        markActivity();
    }

    public void warnCircuitBreakerOpen(FlowType flowType, String nomCircuitBreaker, String raison, int dureeAttente) {
        log.warn("[{}] Circuit Breaker \"{}\" OPEN: {} (attente {}s)",
                flowType.getCode(), nomCircuitBreaker, raison, dureeAttente);
    }

    public void warnDeadlock(FlowType flowType, String procedure, int tentative, int maxTentatives) {
        log.warn("[{}] Deadlock SQL sur {} - tentative {}/{}",
                flowType.getCode(), procedure, tentative, maxTentatives);
    }

    public void infoDeadlockResolu(FlowType flowType, int tentatives, long dureeMs) {
        log.info("[{}] Deadlock résolu après {} tentatives ({}ms)", flowType.getCode(), tentatives, dureeMs);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOGS D'ERREURS (toujours émis)
    // ═══════════════════════════════════════════════════════════════════════

    public void errorMontantInvalide(FlowType flowType, String reference, double montant, String raison) {
        log.error("[{}] [{}] Facture rejetée - montant invalide: {} ({}€)",
                flowType.getCode(), reference, raison, String.format("%.2f", montant));
        markActivity();
        incrementError();
    }

    public void errorClientManquant(FlowType flowType, String reference) {
        log.error("[{}] [{}] Facture non créée - client obligatoire manquant", flowType.getCode(), reference);
        markActivity();
        incrementError();
    }

    public void errorFournisseurNonIdentifiable(FlowType flowType, String reference,
                                                String nomFournisseur, String pennylaneId, String siret) {
        log.error("[{}] [{}] Fournisseur non identifiable: \"{}\" (ID: {}, SIRET: {})",
                flowType.getCode(), reference, nomFournisseur, pennylaneId, siret);
        markActivity();
        incrementError();
    }

    public void errorApiPennylane(FlowType flowType, String reference, int codeHttp, String messageApi) {
        log.error("[{}] [{}] Erreur API Pennylane: HTTP {} - {}",
                flowType.getCode(), reference, codeHttp, messageApi);
        markActivity();
        incrementError();
    }

    public void errorConnexion(FlowType flowType, String typeErreur, int tentative, int maxTentatives) {
        log.error("[{}] Erreur connexion API: {} (tentative {}/{})",
                flowType.getCode(), typeErreur, tentative, maxTentatives);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MÉTHODES UTILITAIRES PRIVÉES
    // ═══════════════════════════════════════════════════════════════════════

    /** Marque le flux courant comme ayant eu de l'activité */
    private void markActivity() {
        String correlationId = CorrelationIdManager.getCurrent();
        if (correlationId != null) {
            FlowContext ctx = flowContexts.get(correlationId);
            if (ctx != null) ctx.markActive();
        }
    }

    private void incrementSuccess() {
        String correlationId = CorrelationIdManager.getCurrent();
        if (correlationId != null) {
            FlowContext ctx = flowContexts.get(correlationId);
            if (ctx != null) ctx.incrementSuccess();
        }
    }

    private void incrementSkipped() {
        String correlationId = CorrelationIdManager.getCurrent();
        if (correlationId != null) {
            FlowContext ctx = flowContexts.get(correlationId);
            if (ctx != null) ctx.incrementSkipped();
        }
    }

    private void incrementError() {
        String correlationId = CorrelationIdManager.getCurrent();
        if (correlationId != null) {
            FlowContext ctx = flowContexts.get(correlationId);
            if (ctx != null) ctx.incrementError();
        }
    }

    private void logFlowHeader(String flowCode, String action, String description, boolean isWarning) {
        if (isWarning) {
            log.warn("[{}] {} {} - {}", flowCode, SEPARATOR, action, description);
        } else {
            log.info("[{}] {} {} - {}", flowCode, SEPARATOR, action, description);
        }
    }

    private void logLine(String flowCode, String message, boolean isWarning) {
        if (isWarning) {
            log.warn("[{}] {}", flowCode, message);
        } else {
            log.info("[{}] {}", flowCode, message);
        }
    }

    private String formatDuration(Duration duration) {
        long totalSeconds = duration.getSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long millis = duration.toMillisPart();

        if (minutes > 0) {
            return String.format("%d min %d sec", minutes, seconds);
        } else if (seconds > 0) {
            return String.format("%.1f sec", seconds + millis / 1000.0);
        } else {
            return String.format("%d ms", millis);
        }
    }

    public FlowStatistics getStatistics(String correlationId) {
        FlowContext ctx = flowContexts.get(correlationId);
        if (ctx == null) return null;
        return new FlowStatistics(ctx);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLASSES INTERNES
    // ═══════════════════════════════════════════════════════════════════════

    /** Contexte interne d'un flux en cours */
    private static class FlowContext {
        private final FlowType flowType;
        private final LocalDateTime startTime;
        private final Map<String, Object> additionalInfo;
        private boolean active = false;
        private int successCount = 0;
        private int errorCount = 0;
        private int skippedCount = 0;

        FlowContext(FlowType flowType, LocalDateTime startTime, Map<String, Object> additionalInfo) {
            this.flowType = flowType;
            this.startTime = startTime;
            this.additionalInfo = additionalInfo;
        }

        FlowType getFlowType() { return flowType; }
        LocalDateTime getStartTime() { return startTime; }
        boolean hasActivity() { return active || successCount > 0 || errorCount > 0 || skippedCount > 0; }
        int getSuccessCount() { return successCount; }
        int getErrorCount() { return errorCount; }
        int getSkippedCount() { return skippedCount; }

        void markActive() { active = true; }
        void incrementSuccess() { successCount++; active = true; }
        void incrementError() { errorCount++; active = true; }
        void incrementSkipped() { skippedCount++; active = true; }
    }

    /** Statistiques publiques exposées (lecture seule) */
    public static class FlowStatistics {
        private final FlowType flowType;
        private final LocalDateTime startTime;
        private final int successCount;
        private final int errorCount;
        private final int skippedCount;

        FlowStatistics(FlowContext ctx) {
            this.flowType = ctx.flowType;
            this.startTime = ctx.startTime;
            this.successCount = ctx.successCount;
            this.errorCount = ctx.errorCount;
            this.skippedCount = ctx.skippedCount;
        }

        public FlowType getFlowType() { return flowType; }
        public LocalDateTime getStartTime() { return startTime; }
        public int getSuccessCount() { return successCount; }
        public int getErrorCount() { return errorCount; }
        public int getSkippedCount() { return skippedCount; }
    }
}
