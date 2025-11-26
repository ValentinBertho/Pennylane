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

    @Transactional
    public void syncEcriture(final Integer ecritureInt, SiteEntity site, List<Item> comptes) {
        log.info("\n/////// Début synchronisation d'un lot d'écriture N°{} ///////\n", ecritureInt);

        final List<Ecriture> ecritures = ecritureRepository.getEcrituresToExport(ecritureInt);
        final Map<Integer, List<Ecriture>> groupedEcritures = ecritures.stream()
                .collect(Collectors.groupingBy(Ecriture::getNoEcriturePiece));

        int lotSuccess = 0;
        int lotErr = 0;

        log.info("Nombre d'écritures à traiter : {}", ecritures.size());

        for (List<Ecriture> ecrituresList : groupedEcritures.values()) {
            Ecriture first = ecrituresList.getFirst();
            log.debug("Traitement de l'écriture N°{} pour la facture {}", first.getNoEcriturePiece(), first.getNoVFacture());


                Invoice wrapper = null;

                try {

                    List<Product> products = processProducts(ecrituresList, site);

                    log.info("Produits traités avec succès pour la facture {}", first.getNoVFacture());
                } catch (Exception e) {
                    log.error("Erreur lors du traitement des produits pour la facture {}: {}", first.getNoVFacture(), e.getMessage(), e);
                    logRepository.ajouterLigneForum("V_FACTURE", String.valueOf(first.getNoVFacture()), "Erreur dans le processProducts : " + e.getMessage(), 2);
                    lotErr++;
                    continue;
                }


            try {
                wrapper = processInvoice(first, ecrituresList, site, comptes);
                log.info("Facture traitée avec succès pour la facture {}", first.getNoVFacture());
            } catch (Exception e) {
                log.error("Erreur lors du traitement de la facture   {}: {}", first.getNoVFacture(), e.getMessage(), e);
                logRepository.ajouterLigneForum("V_FACTURE", String.valueOf(first.getNoVFacture()), "Erreur dans le processInvoice : " + e.getMessage(), 2);
                lotErr++;
                continue;
            }

                try {
                    processCourrier(first, wrapper, site);
                    log.info("Courrier traité avec succès pour la facture {}", first.getNoVFacture());
                } catch (Exception e) {
                    log.error("Erreur lors du traitement du courrier pour la facture {}: {}", first.getNoVFacture(), e.getMessage(), e);
                    logRepository.ajouterLigneForum("V_FACTURE", String.valueOf(first.getNoVFacture()), "Erreur dans le processCourrier : " + e.getMessage(), 2);
                    lotErr++;
                    continue;
                }

            try {
                String aCustomer = processCustomer(first, site, String.valueOf(first.getNoVFacture()), comptes);
                wrapper.setCustomerId(aCustomer);
                log.info("Client traité avec succès pour la facture {}", first.getNoVFacture());
            } catch (Exception e) {
                log.error("Erreur lors du traitement du client pour la facture {}: {}", first.getNoVFacture(), e.getMessage(), e);
                logRepository.ajouterLigneForum("V_FACTURE", String.valueOf(first.getNoVFacture()), "Erreur dans le processCustomer : " + e.getMessage(), 2);
                lotErr++;
                continue;
            }

            InvoiceResponse response = invoiceApi.createInvoice(wrapper, site, true);
                    if (response != null && (response.getResponseStatus() == null || response.getResponseStatus().isEmpty())) {
                        log.info("Facture créée avec succès pour la facture {}", first.getNoVFacture());
                        logRepository.traiterFacture(first.getNoVFacture(), response.getId().toString(), response.getId().toString(), true);
                        logRepository.ajouterLigneForum("V_FACTURE", String.valueOf(first.getNoVFacture()), "Facture transmise avec succès à Pennylane.", 5);
                    } else if (response != null && response.getResponseStatus() == "ALREADY_EXISTS") {
                        log.warn("Facture déjà existante pour la facture {}", first.getNoVFacture());
                        logRepository.traiterFacture(first.getNoVFacture(), response.getId().toString(), response.getId().toString(), true);
                        logRepository.ajouterLigneForum("V_FACTURE", String.valueOf(first.getNoVFacture()), "La facture existe déjà dans Pennylane.", 4);
                    } else if (response != null && response.getResponseStatus() == "FAILED") {
                        log.error("Échec lors de la création de la facture {}: {}", first.getNoVFacture(), response.getResponseMessage());
                        logRepository.ajouterLigneForum("V_FACTURE", String.valueOf(first.getNoVFacture()), "Échec création facture : " + response.getResponseMessage(), 2);
                        lotErr++;
                        continue;
                    } else {
                        log.error("Erreur inconnue lors de la création de la facture pour la facture {}", first.getNoVFacture());
                        logRepository.ajouterLigneForum("V_FACTURE", String.valueOf(first.getNoVFacture()), "Erreur inconnue dans createInvoice", 2);
                        lotErr++;
                        continue;
                    }


                log.info("Traitement complet réussi pour la facture {}.", first.getNoVFacture());
                lotSuccess++;
        }

        logRepository.traiterLot(ecritureInt, "Traitement lot terminé : " + lotSuccess + " réussis, " + lotErr + " erreurs", lotErr == 0);
        log.info("Traitement finalisé : {} factures réussies, {} erreurs.", lotSuccess, lotErr);
        log.info("\n/////// Fin synchronisation d'un lot d'écriture N° {} ///////\n", ecritureInt);
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
        Tiers tierToImport = societeRepository.getTiers(invoiceToImport.getFirst().getNoSociete(), site.getCode());
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

            if (retrievedProduct == null) {
                log.info("Création du produit dans pennylane");
                Product createdProduct = productApi.createProduct(
                        productMapper.mapToProduct(productToImport, String.valueOf(ecrituresList.get(0).getNoVFacture())),
                        site
                );
                logRepository.majProduit(
                        Integer.parseInt(productToImport.getExternalReference()),
                        createdProduct.getId().toString()
                );
                processedProducts.add(createdProduct);
            } else {
                log.info("Mise à jour du produit dans pennylane");
                Product updatedProduct = productApi.updateProduct(
                        productMapper.mapToProduct(productToImport, String.valueOf(ecrituresList.get(0).getNoVFacture())),
                        site
                );
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
            logRepository.ajouterLigneForum("V_FACTURE", String.valueOf(first.getNoVFacture()), "Aucun courrier trouvé pour cette facture.", 2);
        }
    }


    private Item verifyOrCreateCompte(String compteGeneral, List<Item> comptes, SiteEntity site, String raisonSociale) {
        Optional<Item> existingItem = comptes.stream()
                .filter(item -> Objects.equals(
                        removeTrailingZerosString(item.getNumber()),
                        removeTrailingZerosString(String.valueOf(compteGeneral))
                ))
                .findFirst();

        if (existingItem.isEmpty()) {
            Item newItem = new Item();
            newItem.setNumber(compteGeneral);
            newItem.setLabel(raisonSociale);
            try {
                Item createdItem = accountsApi.createLedgerAccount(newItem, site);
                Thread.sleep(2000);
                log.info("Compte créé dans Pennylane : {}", createdItem);
                existingItem = Optional.of(createdItem);
                comptes.add(createdItem);
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
