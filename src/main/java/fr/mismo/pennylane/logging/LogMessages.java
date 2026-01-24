package fr.mismo.pennylane.logging;

/**
 * Centralisation des messages de log pour l'interface PENNYLANE.
 * Tous les messages sont rédigés en français et formatés de manière cohérente.
 *
 * Convention de nommage:
 * - Préfixe INFO_ : Messages d'information
 * - Préfixe WARN_ : Messages d'avertissement
 * - Préfixe ERROR_ : Messages d'erreur
 * - Préfixe DEBUG_ : Messages de debug
 */
public final class LogMessages {

    private LogMessages() {
        // Classe utilitaire, pas d'instanciation
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MESSAGES GÉNÉRAUX
    // ═══════════════════════════════════════════════════════════════════════

    public static final String INFO_DEMARRAGE = "DÉMARRAGE - %s";
    public static final String INFO_FIN = "FIN - %s";
    public static final String INFO_CORRELATION_ID = "Correlation ID: %s";
    public static final String INFO_DUREE = "Durée totale: %s";
    public static final String INFO_HEURE_DEMARRAGE = "Heure de démarrage: %s";

    // ═══════════════════════════════════════════════════════════════════════
    // MESSAGES SYNCHRONISATION ÉCRITURES
    // ═══════════════════════════════════════════════════════════════════════

    public static final String INFO_SYNC_ECRITURES_DEBUT = "Synchronisation des écritures comptables";
    public static final String INFO_SYNC_ECRITURES_LOTS = "Lots à traiter: %d lot(s) identifié(s)";
    public static final String INFO_SYNC_ECRITURES_SITES = "Sites actifs: %d";

    public static final String INFO_LOT_DEBUT = "Début synchronisation d'un lot d'écriture N°%d";
    public static final String INFO_LOT_FIN = "Traitement lot terminé: %d factures réussies, %d erreurs";
    public static final String INFO_LOT_ECRITURES = "Nombre d'écritures à traiter: %d";

    public static final String INFO_FACTURE_DEBUT = "Traitement facture: %s";
    public static final String INFO_FACTURE_CREEE = "✓ Facture créée dans Pennylane (ID: %s)";
    public static final String INFO_FACTURE_DUREE = "Durée de traitement: %.3f sec";
    public static final String INFO_FACTURE_IGNOREE = "Facture ignorée: %s";

    public static final String INFO_CLIENT = "Client: %s (SIRET: %s)";
    public static final String INFO_CLIENT_EXISTANT = "Client existant dans Pennylane (ID: %s)";
    public static final String INFO_CLIENT_CREE = "Nouveau client créé dans Pennylane";

    public static final String INFO_PRODUITS = "Produits: %d article(s) à synchroniser";
    public static final String INFO_PRODUIT_CREE = "Produit \"%s\" créé (ID: %s)";

    public static final String INFO_DOCUMENT_UPLOADE = "Document PDF: %s (téléversé avec succès)";

    // ═══════════════════════════════════════════════════════════════════════
    // MESSAGES SYNCHRONISATION ACHATS
    // ═══════════════════════════════════════════════════════════════════════

    public static final String INFO_SYNC_ACHATS_DEBUT = "Import des factures fournisseurs";
    public static final String INFO_SYNC_ACHATS_PERIODE = "Période de recherche: %d jours (depuis le %s)";
    public static final String INFO_SYNC_ACHATS_FILTRES = "Filtres actifs:";
    public static final String INFO_SYNC_ACHATS_FILTRE_STATUTS = "  • Statuts: %s";
    public static final String INFO_SYNC_ACHATS_FILTRE_CATEGORIES = "  • Catégories: %s";

    public static final String INFO_FACTURES_RECUPEREES = "Factures récupérées (brut): %d";
    public static final String INFO_FACTURES_FILTREES = "Factures après filtrage: %d";
    public static final String INFO_FACTURES_IMPORTEES = "Factures importées: %d";
    public static final String INFO_FOURNISSEURS_CREES = "Fournisseurs créés: %d";
    public static final String INFO_DOCUMENTS_TELECHARGES = "Documents téléchargés: %d";

    // ═══════════════════════════════════════════════════════════════════════
    // MESSAGES RÈGLEMENTS
    // ═══════════════════════════════════════════════════════════════════════

    public static final String INFO_SYNC_REGLEMENTS_DEBUT = "Mise à jour des règlements";
    public static final String INFO_REGLEMENT_TRANSACTION = "  • %s: +%.2f€ (%s)";
    public static final String INFO_REGLEMENT_MONTANT_TOTAL = "Montant total: %.2f€ / %.2f€";
    public static final String INFO_REGLEMENT_STATUT = "✓ Statut: %s";

    public static final String INFO_STATUT_ENTIEREMENT_PAYEE = "ENTIÈREMENT PAYÉE";
    public static final String INFO_STATUT_PARTIELLEMENT_PAYEE = "PARTIELLEMENT PAYÉE";
    public static final String INFO_STATUT_A_PAYER = "À PAYER";
    public static final String INFO_STATUT_SURPAYEE = "SURPAYÉE";

    // ═══════════════════════════════════════════════════════════════════════
    // MESSAGES BON À PAYER
    // ═══════════════════════════════════════════════════════════════════════

    public static final String INFO_SYNC_BAP_DEBUT = "Mise à jour du statut Bon À Payer";
    public static final String INFO_BAP_FACTURE = "Mise à jour BAP facture %s";
    public static final String INFO_BAP_SUCCESS = "✓ Facture %s passée en Bon À Payer";

    // ═══════════════════════════════════════════════════════════════════════
    // MESSAGES D'AVERTISSEMENT
    // ═══════════════════════════════════════════════════════════════════════

    public static final String WARN_INCOHERENCE_MONTANTS = "⚠ Incohérence montants détectée";
    public static final String WARN_INCOHERENCE_DETAIL = "  HT (%.2f€) + TVA (%.2f€) ≠ TTC (%.2f€)";
    public static final String WARN_INCOHERENCE_ECART = "  Écart: %.2f€ - Traitement poursuivi avec montants d'origine";

    public static final String WARN_SIRET_INVALIDE = "⚠ SIRET invalide pour client \"%s\"";
    public static final String WARN_SIRET_DETAIL = "  SIRET fourni: %s (format incorrect)";
    public static final String WARN_SIRET_ACTION = "  Client créé sans SIRET - Correction manuelle requise";

    public static final String WARN_SURPAIEMENT = "⚠ Surpaiement détecté";
    public static final String WARN_SURPAIEMENT_MONTANT_FACTURE = "  Montant facture: %.2f€";
    public static final String WARN_SURPAIEMENT_MONTANT_ENCAISSE = "  Montant encaissé: %.2f€";
    public static final String WARN_SURPAIEMENT_EXCEDENT = "  Excédent: %.2f€ - Avoir à créer";

    public static final String WARN_DOCUMENT_INDISPONIBLE = "⚠ Document PDF indisponible (réf: %s) - Facture créée sans pièce jointe";

    public static final String WARN_CATEGORIES_INCOHERENTES = "⚠ Les catégories configurées et les catégories trouvées ne correspondent pas";
    public static final String WARN_CATEGORIES_DETAIL = "  categoriesAFiltrer=%s (%d), categoryIds=%s (%d)";
    public static final String WARN_CATEGORIES_ARRET = "Tâche arrêtée pour éviter une incohérence";

    public static final String WARN_CIRCUIT_BREAKER_OPEN = "Circuit Breaker \"%s\" passé en état OPEN";
    public static final String WARN_CIRCUIT_BREAKER_RAISON = "  Raison: %s";
    public static final String WARN_CIRCUIT_BREAKER_ATTENTE = "  Durée d'attente: %d secondes";
    public static final String WARN_CIRCUIT_BREAKER_IMPACT = "  Impact: Appels API temporairement bloqués";

    public static final String WARN_DEADLOCK = "⚠ Conflit d'accès base de données - nouvelle tentative automatique (%d/%d)";
    public static final String INFO_DEADLOCK_RESOLU = "Deadlock résolu après %d tentatives";

    public static final String WARN_RATE_LIMIT = "Limite d'appels API proche (%d/%d par minute) - Ralentissement préventif activé";

    // ═══════════════════════════════════════════════════════════════════════
    // MESSAGES D'ERREUR
    // ═══════════════════════════════════════════════════════════════════════

    public static final String ERROR_FACTURE_REJETEE = "✗ Facture rejetée - %s";
    public static final String ERROR_CAUSE = "  Cause: %s";
    public static final String ERROR_VALEUR = "  Valeur actuelle: %s";
    public static final String ERROR_ACTION = "  Action: %s";

    public static final String ERROR_MONTANT_INVALIDE = "Montant invalide";
    public static final String ERROR_MONTANT_MIN = "Montant HT doit être supérieur à 0.01€";
    public static final String ERROR_MONTANT_MAX = "Montant HT doit être inférieur à 10 000 000€";

    public static final String ERROR_CLIENT_MANQUANT = "✗ Facture non créée - Client obligatoire manquant";
    public static final String ERROR_CLIENT_NO_SOCIETE = "  NO_SOCIETE: null";
    public static final String ERROR_CLIENT_ACTION = "  Action: Associer un client à la facture";

    public static final String ERROR_FOURNISSEUR_NON_IDENTIFIABLE = "✗ Facture non importée - Fournisseur non identifiable";
    public static final String ERROR_FOURNISSEUR_DETAIL = "  Fournisseur Pennylane: \"%s\" (ID: %s)";
    public static final String ERROR_FOURNISSEUR_SIRET = "  SIRET: %s";
    public static final String ERROR_FOURNISSEUR_NO_MATCH = "  Aucune correspondance trouvée dans ATHENEO";
    public static final String ERROR_FOURNISSEUR_ACTION = "  Action: Créer le fournisseur dans ATHENEO avec ce SIRET, ou corriger le SIRET dans Pennylane";

    public static final String ERROR_API_PENNYLANE = "✗ Erreur API Pennylane";
    public static final String ERROR_API_CODE = "  Code HTTP: %d (%s)";
    public static final String ERROR_API_MESSAGE = "  Message API: \"%s\"";

    public static final String ERROR_CONNEXION = "✗ Erreur de connexion API Pennylane";
    public static final String ERROR_CONNEXION_TYPE = "  Type: %s";
    public static final String ERROR_CONNEXION_TENTATIVE = "  Tentative: %d/%d";
    public static final String ERROR_CONNEXION_RETRY = "  Prochaine tentative dans: %d secondes";

    public static final String ERROR_CONNEXION_FINAL = "✗ Connexion API Pennylane impossible après %d tentatives";
    public static final String ERROR_CONNEXION_IMPACT = "  Impact: %d factures en attente de synchronisation";
    public static final String ERROR_CONNEXION_ACTION = "  Action: Vérifier la connectivité réseau et le statut de l'API Pennylane";

    public static final String ERROR_SQL_DEADLOCK = "✗ Erreur SQL - Deadlock détecté";
    public static final String ERROR_SQL_PROCEDURE = "  Procédure: %s";
    public static final String ERROR_SQL_CODE = "  Erreur SQL: %d - %s";

    public static final String ERROR_DOCUMENT_UPLOAD = "✗ Échec téléchargement PDF";
    public static final String ERROR_DOCUMENT_URL = "  URL: %s";
    public static final String ERROR_DOCUMENT_ERREUR = "  Erreur: HTTP %d (%s)";
    public static final String ERROR_DOCUMENT_ACTION = "  Facture importée sans document";

    // ═══════════════════════════════════════════════════════════════════════
    // STATUTS FINAUX
    // ═══════════════════════════════════════════════════════════════════════

    public static final String STATUT_SUCCES = "Statut final: SUCCÈS";
    public static final String STATUT_PARTIEL = "Statut final: PARTIEL - Action requise";
    public static final String STATUT_ECHEC = "Statut final: ÉCHEC";

    // ═══════════════════════════════════════════════════════════════════════
    // BILAN
    // ═══════════════════════════════════════════════════════════════════════

    public static final String BILAN_HEADER = "BILAN:";
    public static final String BILAN_LOTS = "  • Lots traités: %d/%d (%d%%)";
    public static final String BILAN_FACTURES_CREEES = "  • Factures créées: %d";
    public static final String BILAN_FACTURES_IGNOREES = "  • Factures ignorées (doublons): %d";
    public static final String BILAN_FACTURES_ERREUR = "  • Factures en erreur: %d";
    public static final String BILAN_CLIENTS_CREES = "  • Clients créés: %d";
    public static final String BILAN_PRODUITS_CREES = "  • Produits créés: %d";
    public static final String BILAN_DOCUMENTS = "  • Documents uploadés: %d";
    public static final String BILAN_ERREURS = "  • Erreurs: %d";
    public static final String BILAN_ERREURS_DETAIL = "  • Erreurs détaillées:";
    public static final String BILAN_ERREUR_ITEM = "    - %s";

    // ═══════════════════════════════════════════════════════════════════════
    // VALIDATION
    // ═══════════════════════════════════════════════════════════════════════

    public static final String DEBUG_VALIDATION = "Vérification %s... %s";
    public static final String DEBUG_VALIDATION_OK = "OK";
    public static final String DEBUG_VALIDATION_KO = "KO";

    public static final String DEBUG_VALIDATION_DOUBLON = "doublon";
    public static final String DEBUG_VALIDATION_MONTANTS = "montants";
    public static final String DEBUG_VALIDATION_CLIENT = "client";
    public static final String DEBUG_VALIDATION_PRODUITS = "produits";
    public static final String DEBUG_VALIDATION_DATE = "date";

    // ═══════════════════════════════════════════════════════════════════════
    // MÉTHODES UTILITAIRES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Formate un message avec les paramètres fournis.
     */
    public static String format(String template, Object... args) {
        return String.format(template, args);
    }

    /**
     * Ajoute le préfixe du flux au message.
     */
    public static String withFlowPrefix(FlowType flowType, String message) {
        return String.format("[%s] %s", flowType.getCode(), message);
    }

    /**
     * Ajoute le préfixe du flux et la référence au message.
     */
    public static String withReference(FlowType flowType, String reference, String message) {
        return String.format("[%s] [%s] %s", flowType.getCode(), reference, message);
    }
}
