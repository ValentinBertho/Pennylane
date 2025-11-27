package fr.mismo.pennylane.util;

/**
 * Classe utilitaire pour les opérations sur les chaînes de caractères.
 * Centralise les méthodes utilitaires dupliquées dans le code.
 */
public final class StringUtils {

    private StringUtils() {
        // Classe utilitaire - constructeur privé
    }

    /**
     * Supprime les zéros de fin d'une chaîne de caractères.
     * Utilisé pour normaliser les numéros de comptes comptables.
     *
     * Exemples :
     * - "41100" -> "411"
     * - "411000" -> "411"
     * - "411" -> "411"
     * - "0000" -> ""
     * - null -> null
     * - "" -> ""
     *
     * @param input Chaîne à traiter
     * @return Chaîne sans zéros de fin, ou null/vide si l'entrée est null/vide
     */
    public static String removeTrailingZeros(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.replaceAll("0+$", "");
    }

    /**
     * Vérifie si une chaîne est null ou vide (après trim)
     *
     * @param str Chaîne à vérifier
     * @return true si la chaîne est null ou vide
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Retourne une valeur par défaut si la chaîne est null ou vide
     *
     * @param str Chaîne à vérifier
     * @param defaultValue Valeur par défaut
     * @return La chaîne ou la valeur par défaut
     */
    public static String defaultIfEmpty(String str, String defaultValue) {
        return isNullOrEmpty(str) ? defaultValue : str;
    }

    /**
     * Masque une chaîne sensible pour le logging (ex: token, password)
     * Affiche seulement les 4 derniers caractères
     *
     * Exemples :
     * - "abc123def456" -> "***f456"
     * - "12" -> "****"
     * - null -> "****"
     *
     * @param sensitive Chaîne sensible à masquer
     * @return Chaîne masquée
     */
    public static String maskSensitive(String sensitive) {
        if (sensitive == null || sensitive.length() <= 4) {
            return "****";
        }
        return "***" + sensitive.substring(sensitive.length() - 4);
    }

    /**
     * Tronque une chaîne à une longueur maximale
     *
     * @param str Chaîne à tronquer
     * @param maxLength Longueur maximale
     * @return Chaîne tronquée avec "..." si nécessaire
     */
    public static String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
}
