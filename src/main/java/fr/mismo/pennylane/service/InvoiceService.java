package fr.mismo.pennylane.service;

import fr.mismo.pennylane.api.AccountsApi;
import fr.mismo.pennylane.api.ApiException;
import fr.mismo.pennylane.api.InvoiceApi;
import fr.mismo.pennylane.api.SupplierApi;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dao.repository.LogRepository;
import fr.mismo.pennylane.dto.Document;
import fr.mismo.pennylane.dto.accounting.Item;
import fr.mismo.pennylane.dto.invoice.*;
import fr.mismo.pennylane.dto.supplier.Supplier;
import fr.mismo.pennylane.settings.WsDocumentProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InvoiceService {

    @Autowired
    DocumentService documentService;

    @Autowired
    LogRepository logRepository;

    @Autowired
    SupplierApi supplierApi;

    @Autowired
    InvoiceApi invoiceApi;

    @Autowired
    AccountsApi accountsApi;

    @Autowired
    WsDocumentProperties wsDocumentProperties;

    @Autowired
    LogHelper logHelper;

    @Transactional
    public void syncInvoice(final SupplierInvoiceResponse.SupplierInvoiceItem invoice, SiteEntity site,List<Long> categoryIds) {
        String traitement = "SYNC_INVOICE";

        if (invoice == null || site == null) {
            log.error("Impossible de synchroniser la facture: invoice ou site est null");
            logHelper.error(traitement, "[translate:Impossible de synchroniser la facture: invoice ou site est null]", null);
            return;
        }

        long start = logHelper.startTraitement(traitement);

        try {
            String invoiceId = Optional.of(invoice.getId().toString())
                    .orElse("UNKNOWN_ID");

            log.debug("/////// Début synchronisation d'une FACTURE D'ACHAT - ID: {} ///////", invoiceId);
            logHelper.info(traitement, "Début synchronisation d'une facture d'achat - ID: " + invoiceId);

            Integer invoiceExists = logRepository.checkIfSupplierInvoiceExists(invoiceId);
            log.debug("Vérification de l'existence de la facture - ID: {}, Existe déjà: {}", invoiceId, invoiceExists != null && invoiceExists > 0);
            logHelper.info(traitement, "Vérification existence facture ID: " + invoiceId + " - Existe déjà: " + (invoiceExists != null && invoiceExists > 0));

            // Paramétrage des valeurs nécessaires pour la création de la facture
            String invoiceIdV2 = Optional.of(invoice.getId().toString()).orElse("");
            String objet = Optional.ofNullable(invoice.getLabel()).orElse("");
            String dateFacture = Optional.ofNullable(invoice.getDate()).orElse("");
            String codSite = Optional.ofNullable(site.getCode()).map(String::trim).orElse("");

            String codDirection = null;
            String codAgence = null;

            if (invoice.getCategories() != null) {
                try {
                    CategoryResponse category = accountsApi.getCategoryByUrl(invoice.getCategories().getUrl(), site);
                    codDirection = null; // TODO à implémenter si besoin
                    codAgence = category != null ? category.getLabel() : null;
                } catch (Exception e) {
                    log.warn("Erreur lors de la récupération de la catégorie pour la facture ID: {}", invoiceId);
                    logHelper.warn(traitement, "Erreur récupération catégorie pour facture ID: " + invoiceId);
                }
            }

            String codEtat = Optional.ofNullable(invoice.getPaymentStatus()).orElse("");
            String totalHT = Optional.of(invoice.getCurrencyAmountBeforeTax().toString()).orElse("0");
            String totalTTC = Optional.of(invoice.getCurrencyAmount().toString()).orElse("0");
            String totalTVA = Optional.of(invoice.getCurrencyTax().toString()).orElse("0");

            log.debug("Détails de la facture - ID: {}, Objet: {}, Date: {}, Site: {}, Direction: {}, Agence: {}, État: {}, TotalHT: {}, TotalTTC: {}, TotalTVA: {}",
                    invoiceId, objet, dateFacture, codSite, codDirection, codAgence, codEtat, totalHT, totalTTC, totalTVA);
            logHelper.info(traitement, String.format("Détails facture ID: %s, Objet: %s, Date: %s, Site: %s, Direction: %s, Agence: %s, État: %s, TotalHT: %s, TotalTTC: %s, TotalTVA: %s",
                    invoiceId, objet, dateFacture, codSite, codDirection, codAgence, codEtat, totalHT, totalTTC, totalTVA));

            // Informations du fournisseur - Sécurisation contre les nulls
            String invoiceNumber = Optional.ofNullable(invoice.getInvoiceNumber()).orElse("");
            String devise = Optional.ofNullable(invoice.getCurrency()).orElse("");

            if (invoice.getSupplier() == null) {
                String errMsg = "Le fournisseur est null pour la facture ID: " + invoiceId;
                log.error(errMsg);
                logHelper.error(traitement, errMsg, new NullPointerException("Supplier est null"));
                processError(invoice, new NullPointerException("Supplier est null"));
                return;
            }

            Supplier supplier = supplierApi.retrieveSupplier(String.valueOf(invoice.getSupplier().getId()), site);

            if (supplier == null) {
                String errMsg = "Le fournisseur est null pour la facture ID: " + invoiceId;
                log.error(errMsg);
                logHelper.error(traitement, errMsg, new NullPointerException("Supplier est null"));
                processError(invoice, new NullPointerException("Supplier est null"));
                return;
            }

            String nomSociete = Optional.ofNullable(supplier.getName()).orElse("");
            String supplierSourceId = Optional.of(supplier.getId().toString()).orElse("");

            log.debug("Informations fournisseur - InvoiceNumber: {}, Devise: {}, Nom: {}", invoiceNumber, devise, nomSociete);
            logHelper.info(traitement, String.format("Informations fournisseur - InvoiceNumber: %s, Devise: %s, Nom: %s", invoiceNumber, devise, nomSociete));

            Supplier aSupplier;
            try {
                aSupplier = supplierApi.retrieveSupplier(supplierSourceId, site);
                if (aSupplier == null) {
                    String errMsg = "Impossible de récupérer les informations du fournisseur pour la facture ID: " + invoiceId;
                    log.error(errMsg);
                    logHelper.error(traitement, errMsg, new NullPointerException("SupplierWrapper ou Supplier est null"));
                    processError(invoice, new NullPointerException("SupplierWrapper ou Supplier est null"));
                    return;
                }
            } catch (Exception e) {
                log.error("Erreur lors de la récupération du fournisseur pour la facture ID: {}", invoiceId, e);
                logHelper.error(traitement, "Erreur récupération fournisseur facture ID: " + invoiceId, e);
                processError(invoice, e);
                return;
            }

            if (aSupplier.getLedgerAccount() == null) {
                String errMsg = "LedgerAccount est null pour le fournisseur de la facture ID: " + invoiceId;
                log.error(errMsg);
                logHelper.error(traitement, errMsg, new NullPointerException("LedgerAccount est null"));
                processError(invoice, new NullPointerException("LedgerAccount est null"));
                return;
            }

            Item ledger = accountsApi.getLedgerAccountById(aSupplier.getLedgerAccount().getId().toString(),site);

            String noPlanItem = Optional.ofNullable(ledger.getNumber()).orElse("");
            String idPennylaneFourn = Optional.of(aSupplier.getId().toString()).orElse("");
            String idPennylaneFournV2 = Optional.of(aSupplier.getId().toString()).orElse("");

            log.debug("Détails fournisseur - NoPlanItem: {}, ID Pennylane: {}, ID Pennylane V2: {}", noPlanItem, idPennylaneFourn, idPennylaneFournV2);
            logHelper.info(traitement, String.format("Détails fournisseur - NoPlanItem: %s, ID Pennylane: %s, ID Pennylane V2: %s", noPlanItem, idPennylaneFourn, idPennylaneFournV2));

            if (invoiceExists != null && invoiceExists > 0) {
                log.debug("Mise à jour de la facture existante - ID: {}", invoiceId);
                logHelper.info(traitement, "Mise à jour facture existante - ID: " + invoiceId);

                logRepository.majSupplierInvoice(noPlanItem, invoiceId, objet, dateFacture, codSite,
                        totalHT, totalTTC, totalTVA, invoiceNumber, nomSociete);
            } else {
                log.info("Création d'une nouvelle facture - ID: {} ...", invoiceId);
                logHelper.info(traitement, "Création nouvelle facture - ID: " + invoiceId);

                String categoryIdsString = categoryIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", "));

                String categoryDetails = "";
                if (invoice.getCategories() != null) {
                    try {
                        CategoryResponse category = accountsApi.getCategoryByUrl(invoice.getCategories().getUrl(), site);
                        if (category != null) {
                            categoryDetails = String.format("ID: %s, Label: %s",
                                    category.getId() != null ? category.getId().toString() : "N/A",
                                    category.getLabel() != null ? category.getLabel() : "N/A");
                        }
                    } catch (Exception e) {
                        log.warn("Erreur lors de la récupération des détails de catégorie pour la facture {}: {}", invoiceId, e.getMessage());
                        logHelper.warn(traitement, "Erreur récupération catégorie facture ID: " + invoiceId);
                        categoryDetails = "Erreur récupération catégorie";
                    }
                } else {
                    categoryDetails = "Aucune catégorie associée";
                }

                String importMessage = String.format("IMPORT FACTURE - ID: %s, Objet: '%s', Fournisseur: '%s', " +
                                "Montant TTC: %s, Site: %s, CategoryIds filtrés: [%s], Catégorie facture: %s",
                        invoiceId, objet, nomSociete, totalTTC, codSite, categoryIdsString, categoryDetails);

                log.info("Création d'une nouvelle facture - ID: {} ...", invoiceId);
                logHelper.info(traitement, "Création facture - ID: " + invoiceId + ", " + importMessage);

                int retour = logRepository.creerSupplierInvoice(noPlanItem, idPennylaneFourn, idPennylaneFournV2, invoiceId, invoiceIdV2, objet, dateFacture, codSite,
                        codDirection, codAgence, codEtat, totalHT, totalTTC, totalTVA, invoiceNumber, devise, nomSociete, importMessage);

                log.info("Facture créée avec succès - Retour ID: {}", retour);
                logHelper.info(traitement, "Facture créée avec succès - Retour ID: " + retour);

                String fileUrl = Optional.ofNullable(invoice.getPublicFileUrl()).orElse("");
                if (!fileUrl.isEmpty()) {
                    log.info("Importation du PDF pour la facture - ID: {}", invoiceId);
                    logHelper.info(traitement, "Importation PDF facture - ID: " + invoiceId);

                    Document doc = documentService.fetchDocument(fileUrl, "Facture");
                    if (doc != null) {
                        doc.setTitle(objet);
                        doc.setNo(String.valueOf(retour));

                        documentService.creerDocumentFromBase64(doc, wsDocumentProperties.getProprieteDocument().getAuteurDocument());

                        log.info("Importation du PDF terminée - Facture ID: {}", invoiceId);
                        logHelper.info(traitement, "Importation PDF terminée - Facture ID: " + invoiceId);
                    } else {
                        log.warn("Document non récupéré pour la facture - ID: {}", invoiceId);
                        logHelper.warn(traitement, "Document non récupéré pour facture ID: " + invoiceId);
                    }
                } else {
                    log.warn("URL du fichier non définie pour la facture - ID: {}", invoiceId);
                    logHelper.warn(traitement, "URL fichier non définie pour facture ID: " + invoiceId);
                }
            }

            log.debug("/////// Fin synchronisation d'une FACTURE D'ACHAT - ID: {} ///////", invoiceId);
            logHelper.info(traitement, "Fin synchronisation facture ID: " + invoiceId);

        } catch (final Exception e) {
            String invoiceId = invoice != null ? Optional.of(invoice.getId().toString()).orElse("UNKNOWN") : "UNKNOWN";
            log.error("/////// Erreur lors de la synchronisation d'une FACTURE D'ACHAT - ID: {} ///////", invoiceId, e);
            logHelper.error(traitement, "Erreur synchronisation facture ID: " + invoiceId, e);
            processError(invoice, e);
        } finally {
            logHelper.endTraitement(traitement, start);
        }
    }

    @Transactional
    public void updateInvoice(String aFacture, SiteEntity aSite) {
        String traitement = "UPDATE_INVOICE";

        if (aFacture == null || aSite == null) {
            log.error("Impossible de mettre à jour la facture: aFacture ou aSite est null");
            logHelper.error(traitement, "[translate:Impossible de mettre à jour la facture: aFacture ou aSite est null]", null);
            return;
        }

        long start = logHelper.startTraitement(traitement);

        try {
            log.info("/////// Début MAJ d'une FACTURE D'ACHAT ///////");
            logHelper.info(traitement, "Début mise à jour facture - ID: " + aFacture);

            log.info("Tentative de récupération de la facture avec l'ID : {}", aFacture);
            SupplierInvoiceResponse.SupplierInvoiceItem invoice = invoiceApi.getSupplierInvoiceById(aSite, aFacture);

            if (invoice == null) {
                log.warn("Facture non trouvée : {}", aFacture);
                logHelper.warn(traitement, "Facture non trouvée - ID: " + aFacture);
                return;
            }

            log.info("Facture trouvée : {}", aFacture);
            logHelper.info(traitement, "Facture trouvée - ID: " + aFacture);

            boolean updateSuccess = false;
            try {
                updateSuccess = invoiceApi.updateSupplierInvoicePaymentStatus(aSite, aFacture, "to_be_paid");
            } catch (ApiException ex) {
                Throwable cause = ex.getCause();
                String errorBody = null;

                if (cause instanceof HttpClientErrorException) {
                    HttpClientErrorException httpEx = (HttpClientErrorException) cause;
                    errorBody = httpEx.getResponseBodyAsString();
                }

                if (errorBody != null && errorBody.contains("attached payment matching its amount")) {
                    log.warn("Facture déjà réglée ou paiement existant, pas de retry. Facture ID: {}", aFacture);
                    logHelper.warn(traitement, "Facture déjà réglée ou paiement existant - ID: " + aFacture);
                    updateSuccess = true;
                } else {
                    throw ex;
                }
            }

            logRepository.traiterSupplierInvoiceBap(invoice.getId().toString(), "", updateSuccess);

            if (updateSuccess) {
                logRepository.ajouterLigneForum("A_FACTURE", aFacture, "Statut de paiement mis à jour ou déjà à jour.", 5);
                log.info("Statut de paiement traité pour la facture : {}", aFacture);
                logHelper.info(traitement, "Statut de paiement traité - ID: " + aFacture);
            } else {
                logRepository.ajouterLigneForum("A_FACTURE", aFacture, "Échec de la mise à jour du statut de paiement.", 2);
                log.warn("Échec de la mise à jour du statut de paiement pour la facture : {}", aFacture);
                logHelper.warn(traitement, "Échec mise à jour statut de paiement - ID: " + aFacture);
            }

            log.info("/////// Fin MAJ d'une FACTURE D'ACHAT ///////");
            logHelper.info(traitement, "Fin mise à jour facture - ID: " + aFacture);

        } catch (final Exception e) {
            logRepository.ajouterLigneForum("A_FACTURE", aFacture, "Échec de la mise à jour du statut de paiement : " + e.getMessage(), 2);
            log.error("/////// Fin MAJ d'une FACTURE D'ACHAT en erreur ///////", e);
            logHelper.error(traitement, "Erreur mise à jour facture ID: " + aFacture, e);
        } finally {
            logHelper.endTraitement(traitement, start);
        }
    }

    @Transactional
    public void updateReglements(String aFacture, SiteEntity aSite) {
        String traitement = "UPDATE_REGLEMENTS";

        if (aFacture == null || aSite == null) {
            log.error("Impossible de mettre à jour les règlements: ID Facture ou Site null");
            logHelper.error(traitement, "[translate:Impossible de mettre à jour les règlements: ID Facture ou Site null]", null);
            return;
        }

        String siteCode = defaultIfNull(aSite.getCode(), "UNKNOWN");
        log.trace("Début MAJ règlements - ID Facture: {}, Site: {}", aFacture, siteCode);
        logHelper.info(traitement, "Début mise à jour règlements pour facture ID: " + aFacture);

        long start = logHelper.startTraitement(traitement);

        try {
            InvoiceResponse invoiceResponse = invoiceApi.getCustomerInvoiceById(aSite, aFacture);
            if (invoiceResponse == null) {
                log.warn("Facture non trouvée - ID: {}", aFacture);
                logHelper.warn(traitement, "Facture non trouvée - ID: " + aFacture);
                return;
            }

            List<Transaction> transactions = Optional.ofNullable(invoiceResponse.getMatchedTransactions())
                    .map(mt -> invoiceApi.getAllMatchedTransactions(aSite, mt.getUrl()))
                    .orElse(Collections.emptyList());

            log.trace(
                    "Facture récupérée - ID: {}, Statut: {}, Montant restant: {}",
                    defaultIfNull(invoiceResponse.getId(), "UNKNOWN"),
                    defaultIfNull(invoiceResponse.getStatus(), "UNKNOWN"),
                    defaultIfNull(invoiceResponse.getRemainingAmountWithTax(), "0")
            );
            logHelper.info(traitement, String.format("Facture ID: %s, Statut: %s, Montant restant: %s",
                    defaultIfNull(invoiceResponse.getId(), "UNKNOWN"),
                    defaultIfNull(invoiceResponse.getStatus(), "UNKNOWN"),
                    defaultIfNull(invoiceResponse.getRemainingAmountWithTax(), "0")
            ));

            boolean isPaid = Boolean.TRUE.equals(invoiceResponse.getPaid());
            double remainingAmount = parseDoubleSafe(invoiceResponse.getRemainingAmountWithTax(), 0.0);
            double total = parseDoubleSafe(invoiceResponse.getCurrencyAmount(), 0.0);
            String invoiceId = defaultIfNull(invoiceResponse.getId(), "");

            if (invoiceId.isEmpty()) {
                log.error("ID de facture vide, impossible de mettre à jour les règlements");
                logHelper.error(traitement, "ID facture vide, impossible de mettre à jour règlements", null);
                return;
            }

            String fullyPaidAt = "";
            if (isPaid && !transactions.isEmpty()) {
                fullyPaidAt = transactions.stream()
                        .filter(t -> t.getCreatedAt() != null)
                        .max(Comparator.comparing(Transaction::getCreatedAt))
                        .map(Transaction::getCreatedAt)
                        .orElse("");
            }
            if (invoiceResponse.getRemainingAmountWithTax() == null) {
                fullyPaidAt = Instant.now().toString();
            }

            double remaining = (invoiceResponse.getRemainingAmountWithTax() != null) ? remainingAmount : (isPaid ? 0.0 : total);
            String status = computePaymentStatus(isPaid, remaining, total);

            log.trace("Données - Payé: {}, Statut: {}, Restant: {}, Payé le: {}, Total: {}",
                    isPaid, status, remainingAmount, fullyPaidAt, total);
            logHelper.info(traitement, String.format("Données règlements - Payé: %s, Statut: %s, Restant: %s, Payé le: %s, Total: %s",
                    isPaid, status, remainingAmount, fullyPaidAt, total));

            int result = logRepository.majSupplierInvoiceReglement(
                    isPaid, status, remainingAmount, fullyPaidAt, total, invoiceId, siteCode);

            if (result == 1) {
                log.info("MAJ réussie des règlements - Facture ID: {}", invoiceId);
                logHelper.info(traitement, "MAJ réussie des règlements pour facture ID: " + invoiceId);

                String details = String.format(
                        "Mise à jour règlements. Payé: %s, Statut: %s, Restant: %s, Payé le: %s, Total: %s",
                        isPaid, status, remainingAmount, fullyPaidAt, total
                );
                logRepository.ajouterLigneForum("V_FACTURE", aFacture, details, 2);
            } else {
                log.trace("MAJ échouée pour la facture - ID: {}", invoiceId);
                logHelper.warn(traitement, "MAJ échouée des règlements pour facture ID: " + invoiceId);
            }

            log.trace("/////// Fin MAJ des REGLEMENTS - Facture ID: {} ///////", invoiceId);
            logHelper.info(traitement, "Fin mise à jour règlements facture ID: " + invoiceId);

        } catch (Exception e) {
            logRepository.ajouterLigneForum(
                    "A_FACTURE", aFacture, "Erreur MAJ règlements: " + e.getMessage(), 2
            );
            log.error("Erreur lors de la MAJ des règlements - ID: {}", aFacture, e);
            logHelper.error(traitement, "Erreur mise à jour règlements facture ID: " + aFacture, e);
        } finally {
            logHelper.endTraitement(traitement, start);
        }
    }

    @Transactional
    public void updateReglementsV2(String aFacture, SiteEntity aSite) {
        String traitement = "UPDATE_REGLEMENTS_V2";

        if (aFacture == null || aSite == null) {
            log.error("Impossible de mettre à jour les règlements: ID Facture est null");
            logHelper.error(traitement, "[translate:Impossible de mettre à jour les règlements: ID Facture est null]", null);
            return;
        }

        long start = logHelper.startTraitement(traitement);

        try {
            String siteCode = Optional.ofNullable(aSite.getCode()).orElse("UNKNOWN");
            log.debug("Récupération des détails de la facture - ID: {}, Site: {}", aFacture, siteCode);
            logHelper.info(traitement, String.format("Récupération détails facture - ID: %s, Site: %s", aFacture, siteCode));

            InvoiceResponse invoiceResponse = invoiceApi.getCustomerInvoiceById(aSite, aFacture);
            if (invoiceResponse == null) {
                log.warn("Facture non trouvée - ID: {}", aFacture);
                logHelper.warn(traitement, "Facture non trouvée - ID: " + aFacture);
                return;
            }

            String invoiceId = Optional.ofNullable(invoiceResponse.getId()).map(Object::toString).orElse("");
            if (invoiceId.isEmpty()) {
                log.error("ID de facture vide, impossible de mettre à jour les règlements");
                logHelper.error(traitement, "ID facture vide, impossible de mettre à jour règlements", null);
                return;
            }

            List<Transaction> transactions = Collections.emptyList();
            if (invoiceResponse.getMatchedTransactions() != null && invoiceResponse.getMatchedTransactions().getUrl() != null) {
                transactions = invoiceApi.getAllMatchedTransactions(aSite, invoiceResponse.getMatchedTransactions().getUrl());
            }

            if (transactions.isEmpty()) {
                log.debug("Aucune transaction à synchroniser pour la facture {}", invoiceId);
                logHelper.info(traitement, "Aucune transaction à synchroniser pour la facture " + invoiceId);
            }

            List<Long> transactionIdsMetier = transactions.stream()
                    .map(Transaction::getId)
                    .collect(Collectors.toList());

            String validIdsCsv = transactionIdsMetier.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            if (!transactionIdsMetier.isEmpty()) {
                int resultPurge = logRepository.purgeObsoleteTransactions(invoiceId, validIdsCsv);
                if (resultPurge != 1) {
                    String warnMsg = "Purge des transactions obsolètes n'a pas abouti pour la facture " + invoiceId + " - Code retour: " + resultPurge;
                    log.warn(warnMsg);
                    logHelper.warn(traitement, warnMsg);
                }
            }

            for (Transaction transaction : transactions) {
                Double montantTransaction = parseDoubleSafe(transaction.getAmount(), 0);
                String dateTransaction = transaction.getCreatedAt();
                Long transactionId = transaction.getId();

                int result = logRepository.creerReglement(
                        invoiceId,
                        transactionId,
                        montantTransaction,
                        dateTransaction
                );

                switch (result) {
                    case 1:
                        String infoMsg = String.format("Règlement créé pour la facture %s - Montant: %s, Date: %s, Transaction ID: %s",
                                invoiceId, montantTransaction, dateTransaction, transactionId);
                        log.info(infoMsg);
                        logHelper.info(traitement, infoMsg);
                        break;
                    case -3:
                        log.debug("Transaction déjà synchronisée pour la facture {} - Transaction ID: {}", invoiceId, transactionId);
                        logHelper.info(traitement, "Transaction déjà synchronisée facture " + invoiceId + " - Transaction ID: " + transactionId);
                        break;
                    default:
                        String warnMsg = String.format("La mise à jour des règlements n'a pas abouti pour la facture %s - Transaction ID: %s - Code retour: %d",
                                invoiceId, transactionId, result);
                        log.warn(warnMsg);
                        logHelper.warn(traitement, warnMsg);
                        break;
                }
            }

            log.debug("/////// Fin MAJ des REGLEMENTS - Facture ID: {} ///////", invoiceId);
            logHelper.info(traitement, "Fin mise à jour règlements facture ID: " + invoiceId);

        } catch (Exception e) {
            logRepository.ajouterLigneForum(
                    "A_FACTURE", aFacture, "Erreur lors de la MAJ des REGLEMENTS pour la facture: " + e.getMessage(), 2
            );
            log.error("Erreur lors de la MAJ des REGLEMENTS pour la facture - ID: {}", aFacture, e);
            logHelper.error(traitement, "Erreur mise à jour règlements facture ID: " + aFacture, e);
        } finally {
            logHelper.endTraitement(traitement, start);
        }
    }

    private static String defaultIfNull(Object value, String defaultValue) {
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Parse un objet en double de manière sécurisée
     * @param value Valeur à parser
     * @param defaultValue Valeur par défaut en cas d'erreur
     * @return Le double parsé ou la valeur par défaut
     */
    private static double parseDoubleSafe(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        try {
            String strValue = value.toString().trim();
            if (strValue.isEmpty()) {
                return defaultValue;
            }
            return Double.parseDouble(strValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Calcule le statut de paiement d'une facture selon sa situation
     * @param isPaid Indicateur de paiement complet
     * @param remaining Montant restant à payer
     * @param total Montant total de la facture
     * @return Le statut de paiement approprié
     */
    private static String computePaymentStatus(boolean isPaid, double remaining, double total) {
        // Gestion des edge cases
        if (total < 0) {
            return "to_be_processed"; // Montant invalide
        }

        // Facture totalement payée (remaining = 0 ou proche de 0 pour gérer les arrondis)
        if (Math.abs(remaining) < 0.01) {
            return "fully_paid";
        }

        // Facture partiellement payée (0 < remaining < total)
        if (remaining > 0 && remaining < total) {
            return "partially_paid";
        }

        // Cas spécial : total = remaining et marqué comme payé
        if (Math.abs(total - remaining) < 0.01 && isPaid) {
            return "to_be_solded";
        }

        // Par défaut : à traiter
        return "to_be_processed";
    }

    @Transactional
    public void processError(final SupplierInvoiceResponse.SupplierInvoiceItem invoice, final Exception e) {
        String traitement = "PROCESS_ERROR";
        String invoiceId = invoice != null ? Optional.of(invoice.getId().toString()).orElse("UNKNOWN_ID") : "NULL_INVOICE";
        log.error("Erreur lors du traitement de la facture ID: {}", invoiceId, e);
        logHelper.error(traitement, "Erreur traitement facture ID: " + invoiceId, e);
    }
}
