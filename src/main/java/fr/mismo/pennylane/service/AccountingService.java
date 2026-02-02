package fr.mismo.pennylane.service;

import fr.mismo.pennylane.api.*;
import fr.mismo.pennylane.dao.entity.CourrierEntity;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dao.repository.*;
import fr.mismo.pennylane.dto.accounting.Item;
import fr.mismo.pennylane.dto.ath.Ecriture;
import fr.mismo.pennylane.dto.ath.Tiers;
import fr.mismo.pennylane.dto.customer.Customer;
import fr.mismo.pennylane.dto.customer.ResponseCustomer;
import fr.mismo.pennylane.dto.invoice.FactureDTO;
import fr.mismo.pennylane.dto.invoice.FileAttachmentResponse;
import fr.mismo.pennylane.dto.invoice.Invoice;
import fr.mismo.pennylane.dto.invoice.InvoiceResponse;
import fr.mismo.pennylane.dto.product.Product;
import fr.mismo.pennylane.logging.FlowLogger;
import fr.mismo.pennylane.logging.FlowType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AccountingService {

    @Autowired
    private EcritureRepository ecritureRepository;

    @Autowired
    WsDocumentService wsDocumentService;

    @Autowired
    CourrierRepository courrierRepository;

    @Autowired
    LogRepository logRepository;

    @Autowired
    LogService logService;

    @Autowired
    FactureRepository factureRepository;

    @Autowired
    InvoiceMapper invoiceMapper;

    @Autowired
    ProductMapper productMapper;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    InvoiceApi invoiceApi;

    @Autowired
    ProductApi productApi;

    @Autowired
    SocieteRepository societeRepository;

    @Autowired
    CustomerApi customerApi;

    @Autowired
    TiersMapper tiersMapper;

    @Autowired
    AccountsApi accountsApi;

    @Autowired
    FlowLogger flowLogger;

    /**
     * Résultat de la synchronisation d'un lot d'écritures.
     * Contient les statistiques pour le bilan du flux.
     */
    @Getter
    public static class SyncResult {
        private int facturesCrees = 0;
        private int facturesIgnorees = 0;
        private int clientsCrees = 0;
        private int produitsCrees = 0;
        private int documentsUploades = 0;
        private final List<String> erreurs = new ArrayList<>();

        public void incrementFacturesCrees() { facturesCrees++; }
        public void incrementFacturesIgnorees() { facturesIgnorees++; }
        public void incrementClientsCrees() { clientsCrees++; }
        public void incrementProduitsCrees() { produitsCrees++; }
        public void incrementDocumentsUploades() { documentsUploades++; }
        public void addErreur(String erreur) { erreurs.add(erreur); }
        public boolean hasErrors() { return !erreurs.isEmpty(); }
    }

    @Transactional
    public SyncResult syncEcriture(final Integer ecritureInt, SiteEntity site, List<Item> comptes) {
        SyncResult result = new SyncResult();
        // Validation des paramètres d'entrée
        if (ecritureInt == null) {
            log.error("[SYNC-ECRITURES] [LOT-null] ✗ Le numéro de lot d'écriture est null");
            throw new IllegalArgumentException("Le numéro de lot d'écriture ne peut pas être null");
        }
        if (site == null) {
            log.error("[SYNC-ECRITURES] [LOT-{}] ✗ Le site est null", ecritureInt);
            throw new IllegalArgumentException("Le site ne peut pas être null");
        }
        if (comptes == null) {
            log.error("[SYNC-ECRITURES] [LOT-{}] ✗ La liste des comptes est null", ecritureInt);
            throw new IllegalArgumentException("La liste des comptes ne peut pas être null");
        }

        log.info("[SYNC-ECRITURES] [LOT-{}] Début synchronisation du lot d'écriture", ecritureInt);

        final List<Ecriture> ecritures = ecritureRepository.getEcrituresToExport(ecritureInt);

        // Vérification si la liste est vide
        if (ecritures == null || ecritures.isEmpty()) {
            log.warn("[SYNC-ECRITURES] [LOT-{}] Aucune écriture à exporter", ecritureInt);
            logService.traiterLotSafe(ecritureInt, "Aucune écriture à traiter", true);
            return result;
        }

        final Map<Integer, List<Ecriture>> groupedEcritures = ecritures.stream()
                .collect(Collectors.groupingBy(Ecriture::getNoEcriturePiece));

        int lotSuccess = 0;
        int lotErr = 0;

        log.info("[SYNC-ECRITURES] [LOT-{}] {} écriture(s) à traiter", ecritureInt, ecritures.size());

        for (List<Ecriture> ecrituresList : groupedEcritures.values()) {
            // Vérification de sécurité : la liste ne doit pas être vide
            if (ecrituresList == null || ecrituresList.isEmpty()) {
                log.warn("[SYNC-ECRITURES] [LOT-{}] Liste d'écritures vide dans le groupe, ignorée", ecritureInt);
                continue;
            }

            Ecriture first = ecrituresList.get(0);
            if (first == null) {
                log.error("[SYNC-ECRITURES] [LOT-{}] ✗ La première écriture du groupe est null", ecritureInt);
                result.addErreur("LOT-" + ecritureInt + ": Écriture null");
                lotErr++;
                continue;
            }

            String factureRef = "FAC-" + first.getNoVFacture();
            flowLogger.startFacture(FlowType.SYNC_ECRITURES, factureRef);

            Invoice wrapper = null;
            boolean clientCree = false;
            int produitsCreesCount = 0;
            boolean documentUploade = false;

            // Traitement des produits
            try {
                List<Product> products = processProducts(ecrituresList, site);
                produitsCreesCount = products.size();
                flowLogger.logProduits(FlowType.SYNC_ECRITURES, factureRef, products.size(), produitsCreesCount);
            } catch (Exception e) {
                log.error("[SYNC-ECRITURES] [{}] ✗ Erreur traitement produits: {}", factureRef, e.getMessage());
                logService.ajouterLigneForumSafe("V_FACTURE", String.valueOf(first.getNoVFacture()), "Erreur dans le processProducts : " + e.getMessage(), 2);
                result.addErreur(factureRef + ": Erreur produits - " + e.getMessage());
                lotErr++;
                continue;
            }

            // Traitement de la facture
            try {
                wrapper = processInvoice(first, ecrituresList, site, comptes);
                flowLogger.logValidation(FlowType.SYNC_ECRITURES, factureRef, "Préparation facture", true);
            } catch (Exception e) {
                log.error("[SYNC-ECRITURES] [{}] ✗ Erreur traitement facture: {}", factureRef, e.getMessage());
                logService.ajouterLigneForumSafe("V_FACTURE", String.valueOf(first.getNoVFacture()), "Erreur dans le processInvoice : " + e.getMessage(), 2);
                result.addErreur(factureRef + ": Erreur facture - " + e.getMessage());
                lotErr++;
                continue;
            }

            // Traitement du courrier (document PDF)
            try {
                processCourrier(first, wrapper, site);
                if (wrapper.getFileId() != null) {
                    documentUploade = true;
                    flowLogger.logDocument(FlowType.SYNC_ECRITURES, factureRef, "facture_" + first.getNoVFacture() + ".pdf", true);
                }
            } catch (Exception e) {
                log.warn("[SYNC-ECRITURES] [{}] ⚠ Document PDF non disponible: {}", factureRef, e.getMessage());
                flowLogger.warnDocumentIndisponible(FlowType.SYNC_ECRITURES, factureRef, "COU-" + first.getNoVFacture());
                // Continue sans document
            }

            // Traitement du client
            try {
                String aCustomer = processCustomer(first, site, String.valueOf(first.getNoVFacture()), comptes);
                wrapper.setCustomerId(aCustomer);
                // Déterminer si le client a été créé ou existait déjà
                // (On considère créé si le processCustomer a retourné un ID)
                if (aCustomer != null) {
                    flowLogger.logValidation(FlowType.SYNC_ECRITURES, factureRef, "Client", true);
                }
            } catch (Exception e) {
                log.error("[SYNC-ECRITURES] [{}] ✗ Erreur traitement client: {}", factureRef, e.getMessage());
                logService.ajouterLigneForumSafe("V_FACTURE", String.valueOf(first.getNoVFacture()), "Erreur dans le processCustomer : " + e.getMessage(), 2);
                result.addErreur(factureRef + ": Erreur client - " + e.getMessage());
                lotErr++;
                continue;
            }

            // Création de la facture dans Pennylane
            InvoiceResponse response = invoiceApi.createInvoice(wrapper, site, true);
            if (response != null && (response.getResponseStatus() == null || response.getResponseStatus().isEmpty())) {
                flowLogger.logFactureCreee(FlowType.SYNC_ECRITURES, factureRef, response.getId().toString(), 0);
                logService.traiterFactureSafe(first.getNoVFacture(), response.getId().toString(), response.getId().toString(), true);
                logService.ajouterLigneForumSafe("V_FACTURE", String.valueOf(first.getNoVFacture()), "Facture transmise avec succès à Pennylane.", 5);
                result.incrementFacturesCrees();
                if (documentUploade) result.incrementDocumentsUploades();
                result.produitsCrees += produitsCreesCount;
            } else if (response != null && "ALREADY_EXISTS".equals(response.getResponseStatus())) {
                flowLogger.infoFactureIgnoree(FlowType.SYNC_ECRITURES, factureRef, "Facture déjà existante dans Pennylane");
                logService.traiterFactureSafe(first.getNoVFacture(), response.getId().toString(), response.getId().toString(), true);
                logService.ajouterLigneForumSafe("V_FACTURE", String.valueOf(first.getNoVFacture()), "La facture existe déjà dans Pennylane.", 4);
                result.incrementFacturesIgnorees();
            } else if (response != null && "FAILED".equals(response.getResponseStatus())) {
                flowLogger.errorApiPennylane(FlowType.SYNC_ECRITURES, factureRef, 422, response.getResponseMessage());
                logService.ajouterLigneForumSafe("V_FACTURE", String.valueOf(first.getNoVFacture()), "Échec création facture : " + response.getResponseMessage(), 2);
                result.addErreur(factureRef + ": " + response.getResponseMessage());
                lotErr++;
                continue;
            } else {
                log.error("[SYNC-ECRITURES] [{}] ✗ Erreur inconnue lors de la création", factureRef);
                logService.ajouterLigneForumSafe("V_FACTURE", String.valueOf(first.getNoVFacture()), "Erreur inconnue dans createInvoice", 2);
                result.addErreur(factureRef + ": Erreur inconnue");
                lotErr++;
                continue;
            }

            log.info("[SYNC-ECRITURES] [{}] ✓ Traitement complet réussi", factureRef);
            lotSuccess++;
        }

        logService.traiterLotSafe(ecritureInt, "Traitement lot terminé : " + lotSuccess + " réussis, " + lotErr + " erreurs", lotErr == 0);
        log.info("[SYNC-ECRITURES] [LOT-{}] Fin - {} factures réussies, {} erreurs", ecritureInt, lotSuccess, lotErr);

        return result;
    }

    private Invoice processInvoice(Ecriture first, List<Ecriture> ecrituresList, SiteEntity site, List<Item> comptes) {
        List<FactureDTO> invoiceToImport = factureRepository.getFacture(first.getNoVFacture());

        log.info("Traitement de la facture {}.", invoiceToImport.getFirst().getChronoVFacture());

        Invoice wrapper = invoiceMapper.mapToInvoice(invoiceToImport, site);

        // Sécurisation du parsing de la date
        Object dateEcheanceObj = ecrituresList.get(0).getDateEcheance();
        if (dateEcheanceObj != null) {
            String dateStr = String.valueOf(dateEcheanceObj);
            if (!dateStr.equalsIgnoreCase("null") && !dateStr.isBlank()) {
                try {
                    wrapper.setDeadline(LocalDate.parse(dateStr));
                } catch (DateTimeParseException e) {
                    log.warn("Format de date invalide pour la facture {} : {}", first.getNoVFacture(), dateStr);
                }
            } else {
                log.warn("Aucune date d'échéance valide pour la facture {}.", first.getNoVFacture());
            }
        } else {
            log.warn("La date d'échéance est null pour la facture {}.", first.getNoVFacture());
        }

        for (FactureDTO factureLigne : invoiceToImport) {
            String compte = factureLigne.getCpte();
            Item item = verifyOrCreateCompte(compte, comptes, site, "Auto interface Pennylane " + compte);
        }

        return wrapper;
    }


    private String processCustomer(Ecriture first, SiteEntity site, String noFacture, List<Item> comptes) {
        List<FactureDTO> invoiceToImport = factureRepository.getFacture(first.getNoVFacture());

        // Validation : la liste de factures ne doit pas être vide
        if (invoiceToImport == null || invoiceToImport.isEmpty()) {
            log.error("Aucune facture trouvée pour la facture N°{}", first.getNoVFacture());
            throw new IllegalStateException("Impossible de traiter le client : aucune facture trouvée");
        }

        Tiers tierToImport = societeRepository.getTiers(invoiceToImport.get(0).getNoSociete(), site.getCode());

        // Validation : le tiers doit exister
        if (tierToImport == null) {
            log.error("Aucun tiers trouvé pour la société N°{}", invoiceToImport.get(0).getNoSociete());
            throw new IllegalStateException("Impossible de traiter le client : tiers introuvable");
        }

        Customer customer = null;
        String customerId = null;

        if (tierToImport.getIdUnique() == null) {

            // Vérifier ou créer le compte comptable localement ou via l'API
            Item item = verifyOrCreateCompte(tierToImport.getCompteComptable(), comptes, site, tierToImport.getRaisonSociale());

            if (item == null) {
                log.error("Impossible de trouver ou créer le compte comptable {} pour la société {}",
                        tierToImport.getCompteComptable(), tierToImport.getRaisonSociale());
                return null;
            }

            // Récupérer le compte dans l'API Pennylane via son numéro
            Item ledger = accountsApi.getLedgerAccountByNumber(item.getNumber(), site);
            if (ledger == null) {
                log.error("Le compte comptable {} n'existe pas dans l'API Pennylane.",
                        item.getNumber());
                return null;
            }

            ResponseCustomer requestedCustomer = customerApi.findCustomerByLedgerAccount(site, Long.valueOf(ledger.getId()));

            if (requestedCustomer != null && !requestedCustomer.getItems().isEmpty()) {
                Customer foundCustomer = requestedCustomer.getItems().getFirst();
                logRepository.majSociete(tierToImport.getCompteComptable(), foundCustomer.getId().toString(), foundCustomer.getId().toString(), site.getCode());
                customer = customerApi.retrieveCustomer(String.valueOf(foundCustomer.getId()), site);
                customerId = foundCustomer.getId().toString();
            }
        } else {
            customer = customerApi.retrieveCustomer(tierToImport.getIdUnique(), site);
        }

        log.info("Traitement de la société {} - {}.", tierToImport.getRaisonSociale(), tierToImport.getCompteComptable());

        if (customer == null) {
            log.info("Création de la société dans Pennylane");

            Customer create = customerApi.createCustomer(tiersMapper.mapToCustomer(tierToImport, site, noFacture), site);
            customerId = String.valueOf(create.getId());
            logRepository.majSociete(tierToImport.getCompteComptable(), customerId, customerId, site.getCode());
        } else {
            log.info("Mise à jour de la société dans Pennylane");
            if (tierToImport.getIdUnique() != null) {
                customerId = tierToImport.getIdUnique();
                customerApi.updateCustomer(tiersMapper.mapToCustomer(tierToImport, site, noFacture), site);
            }
        }
        return customerId;
    }


    private List<Product> processProducts(List<Ecriture> ecrituresList, SiteEntity site) {
        List<Product> processedProducts = new ArrayList<>();

        // Récupération des lignes de facture
        List<FactureDTO> factureLines = factureRepository.getFacture(ecrituresList.getFirst().getNoVFacture());

        // Filtrer les lignes avec produit valide et éviter les doublons par noProduit
        Map<Integer, FactureDTO> uniqueProducts = factureLines.stream()
                .filter(l -> l.getNoProduit() != null)
                .collect(Collectors.toMap(
                        FactureDTO::getNoProduit,
                        Function.identity(),
                        (existing, duplicate) -> existing // en cas de doublons, garder le premier
                ));

        // Charger tous les produits existants du site une seule fois
        List<Product> siteProducts = productApi.listAllProducts(site);

        for (FactureDTO line : uniqueProducts.values()) {
            Product productToImport = productRepository.getProduct(line.getNoProduit());
            if (productToImport == null) {
                log.warn("Aucun produit trouvé avec le numéro de produit : {}", line.getNoProduit());
                continue;
            }

            Product retrievedProduct = null;

            if (productToImport.getId() == null) {
                retrievedProduct = siteProducts.stream()
                        .filter(p -> p.getExternalReference() != null)
                        .filter(p -> p.getExternalReference().equals(productToImport.getExternalReference()))
                        .findFirst()
                        .orElse(null);

                if (retrievedProduct != null) {
                    productToImport.setId(retrievedProduct.getId());
                    logRepository.majProduit(
                            Integer.parseInt(productToImport.getExternalReference()),
                            retrievedProduct.getId().toString()
                    );
                }
            } else {
                retrievedProduct = productApi.retrieveProduct(String.valueOf(productToImport.getId()), site);
            }

            log.info("Traitement du produit {} - {}.", productToImport.getReference(), productToImport.getLabel());

            Product mappedProduct = productMapper.mapToProduct(productToImport, String.valueOf(ecrituresList.get(0).getNoVFacture()));

            // Validation des champs obligatoires avant envoi à l'API Pennylane
            String validationError = productMapper.validateProduct(mappedProduct);
            if (validationError != null) {
                log.error("[SYNC-ECRITURES] ✗ Produit invalide (ref: {}): {}", productToImport.getExternalReference(), validationError);
                logService.ajouterLigneForumSafe("V_FACTURE", String.valueOf(ecrituresList.get(0).getNoVFacture()),
                        "Produit invalide (ref: " + productToImport.getExternalReference() + "): " + validationError, 2);
                continue;
            }

            if (retrievedProduct == null) {
                log.info("Création du produit dans pennylane");
                Product createdProduct = productApi.createProduct(mappedProduct, site);
                if (createdProduct == null) {
                    log.error("[SYNC-ECRITURES] ✗ Échec création produit (ref: {})", productToImport.getExternalReference());
                    continue;
                }
                logRepository.majProduit(
                        Integer.parseInt(productToImport.getExternalReference()),
                        createdProduct.getId().toString()
                );
                processedProducts.add(createdProduct);
            } else {
                log.info("Mise à jour du produit dans pennylane");
                Product updatedProduct = productApi.updateProduct(mappedProduct, site);
                processedProducts.add(updatedProduct);
            }
        }

        return processedProducts;
    }



    private void processCourrier(Ecriture first, Invoice wrapper, SiteEntity site) {
        Optional<CourrierEntity> optionalCourrier = courrierRepository.callExportFactureCourrier(first.getNoVFacture()).findFirst();
        if (optionalCourrier.isPresent()) {
            CourrierEntity courrier = optionalCourrier.get();
            log.info("Traitement du courrier {}.", courrier.getTitreC());
            MultipartFile multipartFile = wsDocumentService.getDocumentContentMultipart(courrier.getLastVersion());
            try {
                String base64File = convertFileToBase64(multipartFile);

                // Convertir MultipartFile en Resource pour l'envoi
                org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(multipartFile.getBytes()) {
                    @Override
                    public String getFilename() {
                        return multipartFile.getOriginalFilename();
                    }
                };

                // Envoi du fichier en pièce jointe via l'API Pennylane
                FileAttachmentResponse response = accountsApi.uploadFileAttachment(resource, site);

                // Tu peux stocker l'id ou l'url retournée dans wrapper
                if (response != null) {
                    wrapper.setFileId(response.getId().toString());
                } else {
                    log.error("Echec de l'upload du fichier pour courrier {}", courrier.getTitreC());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            log.info("Aucun courrier pour la facture de vente N° {}", first.getNoVFacture());
            logService.ajouterLigneForumSafe("V_FACTURE", String.valueOf(first.getNoVFacture()), "Aucun courrier trouvé pour cette facture.", 2);
        }
    }


    private Item verifyOrCreateCompte(String compteGeneral, List<Item> comptes, SiteEntity site, String raisonSociale) {
        // Validation des paramètres
        if (compteGeneral == null || compteGeneral.trim().isEmpty()) {
            log.error("Le numéro de compte général est null ou vide");
            return null;
        }
        if (comptes == null) {
            log.error("La liste des comptes est null");
            return null;
        }

        Optional<Item> existingItem = comptes.stream()
                .filter(item -> item != null && item.getNumber() != null)
                .filter(item -> Objects.equals(
                        removeTrailingZerosString(item.getNumber()),
                        removeTrailingZerosString(String.valueOf(compteGeneral))
                ))
                .findFirst();

        if (existingItem.isEmpty()) {
            Item newItem = new Item();
            newItem.setNumber(compteGeneral);
            newItem.setLabel(raisonSociale != null ? raisonSociale : "Auto interface Pennylane");
            try {
                Item createdItem = accountsApi.createLedgerAccount(newItem, site);
                // FIXME: Remplacer ce sleep par un mécanisme de retry/backoff approprié
                // Le sleep bloque le thread et n'est pas une bonne pratique
                Thread.sleep(2000);
                log.info("Compte créé dans Pennylane : {}", createdItem);
                existingItem = Optional.of(createdItem);
                comptes.add(createdItem);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interruption lors de l'attente après création du compte", e);
            } catch (Exception e) {
                log.error("Erreur lors de la création du compte dans Pennylane", e);
            }
        }

        return existingItem.orElse(null);
    }

    public static String removeTrailingZerosString(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.replaceAll("0+$", "");
    }

    @Transactional
    public void processError(final Integer ecritureInt, final Exception e) {
        log.error("Erreur lors du traitement du lot d'écriture {}", ecritureInt, e);
    }

    @Transactional
    public void processErrorAccount(final Integer account, final Exception e) {
        log.error("Erreur lors du traitement du COMPTE NO : {}", account, e);
    }

    @Transactional
    public void processErrorJournal(final Integer journal, final Exception e) {
        log.error("Erreur lors du traitement du JOURNAL NO : {}", journal, e);
    }

    public String convertFileToBase64(MultipartFile multipartFile) throws IOException {
        byte[] fileBytes = multipartFile.getBytes();
        return Base64.getEncoder().encodeToString(fileBytes);
    }
}
