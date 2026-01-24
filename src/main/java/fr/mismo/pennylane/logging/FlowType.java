package fr.mismo.pennylane.logging;

/**
 * Enumeration des types de flux pour l'interface PENNYLANE.
 * Chaque flux a un code unique utilisé dans les logs pour faciliter le filtrage.
 */
public enum FlowType {

    SYNC_ECRITURES("SYNC-ECRITURES", "Synchronisation des écritures comptables"),
    SYNC_ACHATS("SYNC-ACHATS", "Import des factures fournisseurs"),
    SYNC_ACHATS_V2("SYNC-ACHATS-V2", "Import des factures fournisseurs (changelog)"),
    SYNC_REGLEMENTS("SYNC-REGLEMENTS", "Mise à jour des règlements"),
    SYNC_REGLEMENTS_V2("SYNC-REGLEMENTS-V2", "Mise à jour des règlements V2"),
    SYNC_BAP("SYNC-BAP", "Mise à jour du statut Bon À Payer"),
    PURGE_LOGS("PURGE-LOGS", "Purge des logs");

    private final String code;
    private final String description;

    FlowType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return code;
    }
}
