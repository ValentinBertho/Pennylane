package fr.mismo.pennylane.service;

import fr.mismo.pennylane.api.AccountsApi;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dao.repository.LogRepository;
import fr.mismo.pennylane.dto.accounting.Item;
import fr.mismo.pennylane.dto.invoice.*;
import fr.mismo.pennylane.dto.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class InvoiceMapper {

    private static final Logger log = LoggerFactory.getLogger(InvoiceMapper.class);

    @Autowired
    private LogRepository logRepository;

    @Autowired
    AccountsApi accountsApi;

    /**
     * Mappe une liste de FactureDTO en un InvoiceWrapper
     * @param invoices Liste des factures à mapper
     * @return InvoiceWrapper contenant les données mappées
     */
    public Invoice mapToInvoice(List<FactureDTO> invoices, SiteEntity site) {
        try {
            if (invoices == null) {
                throw new IllegalArgumentException("La liste des factures ne peut pas être null");
            }

            Invoice invoice = new Invoice();

            // Assurez-vous qu'il y a des factures dans la liste avant d'accéder au premier élément
            if (invoices.isEmpty()) {
                log.warn("Tentative de mapper une liste vide de factures");
                return invoice;
            }

            FactureDTO firstInvoice = invoices.get(0);
            String invoiceNumber = firstInvoice.getNoVFacture() != null ?
                    String.valueOf(firstInvoice.getNoVFacture()) : "unknown";

            try {

                String dateString = firstInvoice.getDateFacture().toString().substring(0, 10);
                LocalDate date = LocalDate.parse(dateString);

                // invoice.setLabel(firstInvoice.getObjet());

                invoice.setDate(date);

                invoice.setCurrencyAmountBeforeTax(
                        firstInvoice.getMttHt() != null ? firstInvoice.getMttHt().toString() : "0.0"
                );
                invoice.setCurrencyAmount(
                        firstInvoice.getMttTtc() != null ? firstInvoice.getMttTtc().toString() : "0.0"
                );
                invoice.setAmount(
                        firstInvoice.getMttTtc() != null ? firstInvoice.getMttTtc().toString() : "0.0"
                );
                invoice.setCurrencyTax(
                        (firstInvoice.getMttTtc() != null && firstInvoice.getMttHt() != null)
                                ? String.valueOf(firstInvoice.getMttTtc() - firstInvoice.getMttHt())
                                : "0.0"
                );


                invoice.setInvoiceNumber(Optional.ofNullable(firstInvoice.getChronoVFacture())
                        .map(String::trim)
                        .orElse(""));

                invoice.setExternalReference(Optional.ofNullable(firstInvoice.getChronoVFacture())
                        .map(Object::toString)
                        .map(String::trim)
                        .orElse(""));

                // Mappage des lignes de facture avec gestion des erreurs par ligne
                List<LineItem> lineItems = new ArrayList<>();
                for (FactureDTO factureDTO : invoices) {
                    try {
                        LineItem lineItem = mapToLineItem(factureDTO, site);
                        if (lineItem != null) {
                            lineItems.add(lineItem);
                        }
                    } catch (Exception e) {
                        log.error("Erreur lors du mapping d'une ligne pour la facture {}: {}",
                                invoiceNumber, e.getMessage(), e);
                        logRepository.ajouterLigneForum("V_FACTURE", invoiceNumber,
                                "Erreur dans mapToLineItem : " + e.getMessage(), 2);
                        // Continue avec les autres lignes en cas d'erreur sur une ligne
                    }
                }

                invoice.setInvoiceLinesList(lineItems);

                // Mappage du client
                try {
                    invoice.setCustomerId((firstInvoice.getCustomerPennylaneId() != null ?
                            String.valueOf(firstInvoice.getCustomerPennylaneId()) : ""));

                    invoice.setId(firstInvoice.getInvoicePennylaneId() != null ?
                            Long.valueOf(firstInvoice.getInvoicePennylaneId()) : null);

                } catch (Exception e) {
                    log.error("Erreur lors du mapping du client pour la facture {}: {}",
                            invoiceNumber, e.getMessage(), e);
                     logRepository.ajouterLigneForum("V_FACTURE", invoiceNumber,
                            "Erreur dans le mapping du client : " + e.getMessage(), 2);
                    invoice.setCustomerId(null);
                }
            } catch (Exception e) {
                log.error("Erreur lors du mapping des données principales de la facture {}: {}",
                        invoiceNumber, e.getMessage(), e);
                logRepository.ajouterLigneForum("V_FACTURE", invoiceNumber,
                        "Erreur dans mapToInvoice : " + e.getMessage(), 2);
                // Création d'une facture minimale en cas d'erreur
                invoice = new Invoice();
                invoice.setExternalReference(invoiceNumber);
                invoice.setInvoiceLinesList(Collections.emptyList());
            }

            return invoice;
        } catch (Exception e) {
            String invoiceId = "inconnu";
            if (invoices != null && !invoices.isEmpty() && invoices.get(0).getNoVFacture() != null) {
                invoiceId = invoices.get(0).getNoVFacture().toString();
            }
            log.error("Erreur critique lors du mapping de la facture {}: {}",
                    invoiceId, e.getMessage(), e);
            logRepository.ajouterLigneForum("V_FACTURE", invoiceId,
                    "Erreur critique dans mapToInvoice : " + e.getMessage(), 2);

            // En cas d'erreur critique, on renvoie un objet minimal
            Invoice minimalInvoice = new Invoice();
            minimalInvoice.setExternalReference(invoiceId);
            minimalInvoice.setInvoiceLinesList(Collections.emptyList());
            return minimalInvoice;
        }
    }


    /**
     * Mappe un FactureDTO en une ligne de facture (LineItem)
     * @param factureDTO L'objet FactureDTO à mapper
     * @param site Le site pour récupérer les comptes comptables
     * @return LineItem mappé à partir du FactureDTO
     */
    private LineItem mapToLineItem(FactureDTO factureDTO, SiteEntity site) {
        try {
            if (factureDTO == null) {
                log.warn("Tentative de mapper un FactureDTO null en LineItem");
                return null;
            }

            LineItem lineItem = new LineItem();

            // Récupération des valeurs de base
            Double montantHT = Optional.ofNullable(factureDTO.getTotalHT()).orElse(0.0);
            Integer qte = Optional.ofNullable(factureDTO.getQteFac()).orElse(1);

            // Sécurisation de la quantité (évite division par zéro)
            if (qte == null || qte <= 0) {
                log.warn("Quantité invalide ({}) pour la ligne {} (facture {}), remplacement par 1",
                        qte, factureDTO.getNoVLFacture(), factureDTO.getNoVFacture());
                qte = 1;
            }

            String vatRateStr = Optional.ofNullable(factureDTO.getCodTaxe())
                    .map(String::trim)
                    .orElse("FR_200");
            Double tauxTVA = Optional.ofNullable(factureDTO.getTauxTaxe()).orElse(20.0);

            // Utilisation de BigDecimal pour tous les calculs monétaires
            BigDecimal bdMontantHT = BigDecimal.valueOf(montantHT).setScale(2, RoundingMode.HALF_UP);
            BigDecimal bdQuantite = BigDecimal.valueOf(qte);
            BigDecimal bdTauxTVA = BigDecimal.valueOf(tauxTVA)
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

            // Calcul du prix unitaire HT avec 6 décimales
            BigDecimal bdPrixUnitaireHT = bdMontantHT.divide(bdQuantite, 6, RoundingMode.HALF_UP);


            // Vérification : recalcul du montant HT pour détecter les écarts d'arrondi
            BigDecimal bdMontantHTRecalcule = bdPrixUnitaireHT
                    .multiply(bdQuantite)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal ecart = bdMontantHT.subtract(bdMontantHTRecalcule).abs();

            // Si écart > 0.01€, on ajuste le prix unitaire pour garantir la cohérence
            if (ecart.compareTo(new BigDecimal("0.01")) > 0) {
                // Calcul de l'ajustement nécessaire par unité
                BigDecimal ajustement = bdMontantHT.subtract(bdMontantHTRecalcule)
                        .divide(bdQuantite, 6, RoundingMode.HALF_UP);
                bdPrixUnitaireHT = bdPrixUnitaireHT.add(ajustement);

                // Log pour traçabilité
                log.debug("Ajustement prix unitaire - Ligne: {}, Écart: {}, Nouveau prix unitaire: {}",
                        factureDTO.getNoVLFacture(), ecart, bdPrixUnitaireHT);

                // Recalcul final
                bdMontantHTRecalcule = bdPrixUnitaireHT.multiply(bdQuantite)
                        .setScale(2, RoundingMode.HALF_UP);
            }

            // Calcul de la TVA sur le montant HT recalculé
            BigDecimal bdTVA = bdMontantHTRecalcule.multiply(bdTauxTVA)
                    .setScale(2, RoundingMode.HALF_UP);

            // Calcul du TTC
            BigDecimal bdTTC = bdMontantHTRecalcule.add(bdTVA);

            // === ASSIGNATION DES CHAMPS OBLIGATOIRES (selon documentation) ===

            // 1. quantity (required) - number
            lineItem.setQuantity(qte);

            // 2. raw_currency_unit_price (required) - string, jusqu'à 6 décimales
            lineItem.setRawCurrencyUnitPrice(bdPrixUnitaireHT.toPlainString());

            // 3. currency_amount (required) - string, montant TTC en devise de la facture
            lineItem.setCurrencyAmount(bdTTC.toPlainString());

            // 4. currency_tax (required) - string, montant TVA en devise de la facture
            lineItem.setCurrencyTax(bdTVA.toPlainString());

            // 5. vat_rate (required) - string enum
            lineItem.setVatRate(vatRateStr);

            // 6. unit (required) - string
            lineItem.setUnit("unit");

            // === CHAMPS OPTIONNELS ===

            // label (deprecated mais encore utilisable)
            String label = Optional.ofNullable(factureDTO.getDesCom())
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .orElse("Ligne de facture");
            lineItem.setLabel(label);

            // product_id (optionnel)
            if (factureDTO.getIdProduit() != null && factureDTO.getIdProduit() > 0) {
                lineItem.setProductId(factureDTO.getIdProduit());
            }

            // ledger_account_id (optionnel)
            String cpte = factureDTO.getCpte();
            if (cpte != null && !cpte.trim().isEmpty() && !cpte.equals("-1")) {
                try {
                    Item item = accountsApi.getLedgerAccountByNumber(
                            removeTrailingZerosString(cpte), site);
                    if (item != null && item.getId() != null) {
                        lineItem.setLedgerAccountId(String.valueOf(item.getId()));
                    }
                } catch (Exception e) {
                    log.warn("Compte comptable {} non trouvé pour ligne {}: {}",
                            cpte, factureDTO.getNoVLFacture(), e.getMessage());
                }
            }

            // description (deprecated mais encore utilisable si nécessaire)
            // lineItem.setDescription(null);

            // Log de contrôle pour vérification
            if (log.isDebugEnabled()) {
                log.debug("LineItem créé - Ligne: {}, Qté: {}, PU HT: {}, Montant HT recalculé: {}, TVA: {}, TTC: {}",
                        factureDTO.getNoVLFacture(),
                        qte,
                        bdPrixUnitaireHT.toPlainString(),
                        bdMontantHTRecalcule.toPlainString(),
                        bdTVA.toPlainString(),
                        bdTTC.toPlainString());
            }

            return lineItem;

        } catch (Exception e) {
            String invoiceId = factureDTO != null && factureDTO.getNoVFacture() != null ?
                    factureDTO.getNoVFacture().toString() : "inconnu";
            String lineId = factureDTO != null && factureDTO.getNoVLFacture() != null ?
                    factureDTO.getNoVLFacture().toString() : "inconnu";

            log.error("Erreur lors du mapping de la ligne {} (facture {}): {}",
                    lineId, invoiceId, e.getMessage(), e);
            logRepository.ajouterLigneForum("V_FACTURE", invoiceId,
                    "Erreur dans mapToLineItem (ligne " + lineId + "): " + e.getMessage(), 2);
            return null;
        }
    }

    // Méthode pour convertir un taux de TVA en double (ex: "20%" -> 0.2)
    private Double parseTauxTVA(String tauxStr) {
        if (tauxStr == null) return 0.0;
        try {
            return Double.parseDouble(tauxStr.replace("%", "").trim()) / 100.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }



    /**
     * Supprime les zéros à la fin d'une chaîne de caractères
     * @param input La chaîne à traiter
     * @return La chaîne sans les zéros terminaux
     */
    public static String removeTrailingZerosString(String input) {
        try {
            if (input == null || input.isEmpty()) {
                return "";
            }
            return input.replaceAll("0+$", ""); // Supprime les zéros en fin de chaîne
        } catch (Exception e) {
            log.error("Erreur lors de la suppression des zéros en fin de chaîne: {}", e.getMessage(), e);
            return input != null ? input : "";
        }
    }
}