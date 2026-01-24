package fr.mismo.pennylane.logging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire des identifiants de corrélation pour le suivi des flux.
 * Permet de tracer un flux de bout en bout via un identifiant unique.
 *
 * Format: {FLOW_CODE}-{YYYYMMDD}-{HHMMSS}-{UUID_SHORT}
 * Exemple: SYNC-ECR-20260123-143052-A7B2
 */
public class CorrelationIdManager {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

    // Stockage thread-local pour l'ID de corrélation courant
    private static final ThreadLocal<String> currentCorrelationId = new ThreadLocal<>();

    // Cache des IDs de corrélation actifs avec leur contexte
    private static final ConcurrentHashMap<String, FlowContext> activeFlows = new ConcurrentHashMap<>();

    /**
     * Génère un nouvel identifiant de corrélation pour un flux.
     *
     * @param flowType Le type de flux
     * @return L'identifiant de corrélation généré
     */
    public static String generate(FlowType flowType) {
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DATE_FORMATTER);
        String time = now.format(TIME_FORMATTER);
        String uuid = UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        String correlationId = String.format("%s-%s-%s-%s",
            getShortCode(flowType), date, time, uuid);

        currentCorrelationId.set(correlationId);
        activeFlows.put(correlationId, new FlowContext(flowType, now));

        return correlationId;
    }

    /**
     * Récupère l'identifiant de corrélation courant du thread.
     *
     * @return L'identifiant de corrélation ou null si aucun flux actif
     */
    public static String getCurrent() {
        return currentCorrelationId.get();
    }

    /**
     * Définit l'identifiant de corrélation courant.
     *
     * @param correlationId L'identifiant à définir
     */
    public static void setCurrent(String correlationId) {
        currentCorrelationId.set(correlationId);
    }

    /**
     * Efface l'identifiant de corrélation courant et le retire des flux actifs.
     */
    public static void clear() {
        String id = currentCorrelationId.get();
        if (id != null) {
            activeFlows.remove(id);
        }
        currentCorrelationId.remove();
    }

    /**
     * Récupère le contexte d'un flux actif.
     *
     * @param correlationId L'identifiant de corrélation
     * @return Le contexte du flux ou null si non trouvé
     */
    public static FlowContext getFlowContext(String correlationId) {
        return activeFlows.get(correlationId);
    }

    /**
     * Génère un code court pour le type de flux.
     */
    private static String getShortCode(FlowType flowType) {
        return switch (flowType) {
            case SYNC_ECRITURES -> "SYNC-ECR";
            case SYNC_ACHATS -> "SYNC-ACH";
            case SYNC_ACHATS_V2 -> "SYNC-ACH2";
            case SYNC_REGLEMENTS -> "SYNC-REG";
            case SYNC_REGLEMENTS_V2 -> "SYNC-REG2";
            case SYNC_BAP -> "SYNC-BAP";
            case PURGE_LOGS -> "PURGE";
        };
    }

    /**
     * Classe interne représentant le contexte d'un flux.
     */
    public static class FlowContext {
        private final FlowType flowType;
        private final LocalDateTime startTime;
        private int successCount = 0;
        private int errorCount = 0;
        private int skippedCount = 0;

        public FlowContext(FlowType flowType, LocalDateTime startTime) {
            this.flowType = flowType;
            this.startTime = startTime;
        }

        public FlowType getFlowType() {
            return flowType;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public void incrementSuccess() {
            this.successCount++;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public void incrementError() {
            this.errorCount++;
        }

        public int getSkippedCount() {
            return skippedCount;
        }

        public void incrementSkipped() {
            this.skippedCount++;
        }

        public int getTotalProcessed() {
            return successCount + errorCount + skippedCount;
        }
    }
}
