package fr.mismo.pennylane.util;

/**
 * Constantes centralisées pour les appels API et la configuration
 * Remplace les magic numbers disséminés dans le code
 */
public final class ApiConstants {

    private ApiConstants() {
        // Classe utilitaire - constructeur privé
    }

    /**
     * Constantes de rate limiting (remplace les Thread.sleep() hardcodés)
     */
    public static final class RateLimit {
        private RateLimit() {}

        // Rate limits Pennylane API
        public static final int PENNYLANE_MAX_CALLS_PER_MINUTE = 100;
        public static final int PENNYLANE_RETRY_DELAY_MS = 600;
        public static final int PENNYLANE_RETRY_DELAY_LONG_MS = 1100;
        public static final int PENNYLANE_RETRY_DELAY_ACCOUNT_MS = 2000;

        // Retry strategy
        public static final int MAX_RETRY_ATTEMPTS = 3;
        public static final long INITIAL_BACKOFF_MS = 500;
        public static final double BACKOFF_MULTIPLIER = 2.0;
    }

    /**
     * Constantes de pagination
     */
    public static final class Pagination {
        private Pagination() {}

        public static final int DEFAULT_PAGE_SIZE = 100;
        public static final int MAX_PAGE_SIZE = 1000;
    }

    /**
     * Endpoints API Pennylane
     */
    public static final class Endpoints {
        private Endpoints() {}

        // Clés pour le rate limiter
        public static final String INVOICE_CREATE = "pennylane_invoice_create";
        public static final String INVOICE_UPDATE = "pennylane_invoice_update";
        public static final String INVOICE_LIST = "pennylane_invoice_list";
        public static final String CUSTOMER_CREATE = "pennylane_customer_create";
        public static final String CUSTOMER_UPDATE = "pennylane_customer_update";
        public static final String PRODUCT_CREATE = "pennylane_product_create";
        public static final String PRODUCT_UPDATE = "pennylane_product_update";
        public static final String ACCOUNT_CREATE = "pennylane_account_create";
        public static final String SUPPLIER_RETRIEVE = "pennylane_supplier_retrieve";
    }

    /**
     * Constantes de validation
     */
    public static final class Validation {
        private Validation() {}

        public static final int MAX_INVOICE_LABEL_LENGTH = 255;
        public static final int MAX_CUSTOMER_NAME_LENGTH = 255;
        public static final int MIN_AMOUNT = 0;
        public static final double AMOUNT_PRECISION = 0.01; // Précision pour les comparaisons de montants
    }

    /**
     * Codes d'erreur métier
     */
    public static final class ErrorCodes {
        private ErrorCodes() {}

        public static final String INVALID_PARAMETER = "INVALID_PARAMETER";
        public static final String ENTITY_NOT_FOUND = "ENTITY_NOT_FOUND";
        public static final String DUPLICATE_ENTITY = "DUPLICATE_ENTITY";
        public static final String API_ERROR = "API_ERROR";
        public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    }

    /**
     * Noms des traitements pour le logging
     */
    public static final class TraitementNames {
        private TraitementNames() {}

        public static final String SYNC_INVOICE = "SYNC_INVOICE";
        public static final String UPDATE_INVOICE = "UPDATE_INVOICE";
        public static final String UPDATE_REGLEMENTS = "UPDATE_REGLEMENTS";
        public static final String UPDATE_REGLEMENTS_V2 = "UPDATE_REGLEMENTS_V2";
        public static final String PROCESS_ERROR = "PROCESS_ERROR";
        public static final String SYNC_ENTRIES = "SYNC_ENTRIES";
        public static final String UPDATE_SALE = "UPDATE_SALE";
    }
}
