package fr.mismo.pennylane.Scheduler;

import fr.mismo.pennylane.api.AccountsApi;
import fr.mismo.pennylane.api.InvoiceApi;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dao.repository.EcritureRepository;
import fr.mismo.pennylane.dao.repository.SiteRepository;
import fr.mismo.pennylane.dto.Category;
import fr.mismo.pennylane.dto.invoice.*;
import fr.mismo.pennylane.service.CategoryCacheService;
import fr.mismo.pennylane.service.InvoiceService;
import fr.mismo.pennylane.settings.Config;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Scheduler pour la synchronisation des factures fournisseurs entre Pennylane et ATHENEO
 *
 * <h2>Responsabilités</h2>
 * <ul>
 *   <li>Importer les factures fournisseurs Pennylane → ATHENEO (V1 et V2)</li>
 *   <li>Mettre à jour les règlements/paiements Pennylane → ATHENEO</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>Les CRON d'exécution sont définis dans application.yml :</p>
 * <pre>
 * cron:
 *   Purchases: "-"                      # V1 désactivée (récupère toutes les factures)
 *   PurchasesV2: "-"                    # V2 désactivée (utilise changelog)
 *   UpdatePurchaseReglement: "-"        # V1 désactivée
 *   UpdatePurchaseReglementV2: "-"      # V2 désactivée
 * </pre>
 *
 * <h2>Paramètres de configuration</h2>
 * <pre>
 * facture:
 *   statusAFiltrer: 'to_be_processed'   # Statuts à importer
 *   daysBackward: 360                    # Remonter jusqu'à N jours
 *   categoriesAFiltrer: [ACH]           # Catégories à synchroniser
 *   lastInsertPurchases: 2024-01-01T00:00:00
 * </pre>
 *
 * <h2>Procédures stockées utilisées</h2>
 * <ul>
 *   <li>{@code SP_PENNYLANE_SUPPLIER_INVOICE_REGLEMENT} - Récupère règlements à maj</li>
 *   <li>{@code SP_PENNYLANE_GET_FACTURE} - Détails d'une facture</li>
 * </ul>
 *
 * @see InvoiceService
 * @see CategoryCacheService
 * @see DOCUMENTATION_SCHEDULERS.md
 * @author Interface Pennylane
 * @since 1.10.2
 */
@Component
@Slf4j
public class schedulerPurchases {

    @Autowired
    SiteRepository siteRepository;

    @Autowired
    InvoiceService invoiceService;

    @Autowired
    InvoiceApi invoiceApi;

    @Autowired
    Config config;

    @Autowired
    EcritureRepository ecritureRepository;

    @Autowired
    AccountsApi accountsApi;

    @Autowired
    CategoryCacheService categoryCacheService;

    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * Synchronise les factures fournisseurs depuis Pennylane vers ATHENEO (Version 1)
     *
     * <h3>Direction</h3>
     * <p>Pennylane → ATHENEO</p>
     *
     * <h3>État</h3>
     * <p>⚠️ DÉSACTIVÉ par défaut (cron: "-")</p>
     *
     * <h3>Flux de traitement</h3>
     * <ol>
     *   <li>Calcule la date de synchronisation (aujourd'hui - daysBackward)</li>
     *   <li>Récupère les catégories via cache (optimisation performance)</li>
     *   <li>Filtre les catégories autorisées (ex: ACH)</li>
     *   <li>Récupère TOUTES les factures depuis Pennylane via API</li>
     *   <li>Filtre par statut (ex: to_be_processed)</li>
     *   <li>Synchronise chaque facture vers ATHENEO</li>
     *   <li>Met à jour lastInsertPurchases</li>
     * </ol>
     *
     * <h3>Différence avec V2</h3>
     * <p>V1 récupère TOUTES les factures puis filtre (lourd), V2 utilise changelog (léger).</p>
     *
     * <h3>Configuration YAML</h3>
     * <pre>
     * cron:
     *   Purchases: "0 * /30 * * * *"  # Toutes les 30 minutes
     *
     * facture:
     *   statusAFiltrer: 'to_be_processed'
     *   daysBackward: 360
     *   categoriesAFiltrer: [ACH]
     * </pre>
     *
     * <h3>API Pennylane utilisée</h3>
     * <ul>
     *   <li>GET /categories - Liste des catégories</li>
     *   <li>GET /supplier_invoices - Liste des factures fournisseurs</li>
     * </ul>
     *
     * <h3>Métriques loggées</h3>
     * <ul>
     *   <li>Durée récupération catégories</li>
     *   <li>Durée récupération factures API</li>
     *   <li>Durée filtrage</li>
     *   <li>Nombre factures brutes vs filtrées</li>
     * </ul>
     *
     * @see InvoiceService#syncInvoice(SupplierInvoiceResponse.SupplierInvoiceItem, SiteEntity, List)
     * @see CategoryCacheService#getCategories(SiteEntity)
     */
    @Scheduled(cron = "${cron.Purchases}")
    public void SyncPurchases() {
        long startGlobal = System.currentTimeMillis();
        log.info("== Démarrage de la synchronisation des FACTURES ACHATS (Pennylane -> Athénéo) ==");

        OffsetDateTime syncDateTime = LocalDate.now()
                .minusDays(Long.parseLong(config.getDaysBackward()))
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC);

        String statusAFiltrer = config.getStatusAFiltrer();

        List<SiteEntity> sites = siteRepository.findAllByPennylaneAchatTrue();
        boolean hasProcessedInvoices = false;

        // Variables pour logging regroupé
        List<String> allCategoriesAFiltrer = new ArrayList<>();
        List<Long> allCategoryIds = new ArrayList<>();
        int totalItems = 0;
        int totalFilteredInvoices = 0;

        for (SiteEntity site : sites) {
            long startSite = System.currentTimeMillis();
            log.debug("== Début du traitement des factures pour le site {} ==", site.getCode());

            // Chrono récupération catégories
            long startCategories = System.currentTimeMillis();
            List<Category> categories = categoryCacheService.getCategories(site);

            // OLD VERSION LENTE.
            // List<Category> categories = invoiceApi.listAllCategories(site);
            long durationCategories = System.currentTimeMillis() - startCategories;
            log.debug("Site {} - Récupération des catégories effectuée en {} ms", site.getCode(), durationCategories);

            List<String> categoriesAFiltrer = config.getCategoriesAFiltrer();

            List<Long> categoryIds = categories.stream()
                    .filter(c -> categoriesAFiltrer.contains(c.getLabel()))
                    .map(Category::getId)
                    .filter(Objects::nonNull)
                    .toList();

            // Vérification : correspondance entre les deux listes
            if (categoriesAFiltrer.size() != categoryIds.size()) {
                log.warn("⚠️ Les catégories configurées et les catégories trouvées ne correspondent pas : "
                                + "categoriesAFiltrer={} ({}), categoryIds={} ({})",
                        categoriesAFiltrer, categoriesAFiltrer.size(),
                        categoryIds, categoryIds.size());
                log.warn("Tâche SyncPurchases arrêtée pour éviter une incohérence.");
                return; // quitte la méthode, donc stoppe le cron ici
            }

            // Chrono récupération factures
            long startInvoicesApi = System.currentTimeMillis();
            List<SupplierInvoiceResponse.SupplierInvoiceItem> items = invoiceApi.listAllSupplierInvoices(site, categoryIds, syncDateTime);

            long durationInvoicesApi = System.currentTimeMillis() - startInvoicesApi;
            log.debug("Site {} - Récupération des factures effectuée en {} ms ({} factures brutes)",
                    site.getCode(), durationInvoicesApi, items.size());

            log.debug("🔎 Début du filtrage: statusAFiltrer={}", statusAFiltrer);

            // Filtrage factures
            long startFilter = System.currentTimeMillis();
            List<SupplierInvoiceResponse.SupplierInvoiceItem> invoices = items.stream()
                    .filter(invoice -> statusAFiltrer == null || statusAFiltrer.isEmpty()
                            || statusAFiltrer.equals(invoice.getPaymentStatus()))
                    .toList();



            long durationFilter = System.currentTimeMillis() - startFilter;
            log.debug("Site {} - Filtrage factures effectué en {} ms ({} retenues sur {})",
                    site.getCode(), durationFilter, invoices.size(), items.size());

            if (CollectionUtils.isEmpty(invoices)) {
                log.debug("Aucune facture à synchroniser pour le site : {}", site.getCode());
                continue;
            }

            for (SupplierInvoiceResponse.SupplierInvoiceItem invoice : invoices) {
                long startInvoice = System.currentTimeMillis();
                try {
                    invoiceService.syncInvoice(invoice, site, categoryIds);
                    hasProcessedInvoices = true;
                } catch (final RestClientException e) {
                    log.error("Erreur API Pennylane pour facture ID {}", invoice.getId(), e);
                } catch (final ServiceException e) {
                    log.error("Erreur spécifique au service pour facture ID {}", invoice.getId(), e);
                } catch (final Exception e) {
                    log.error("Erreur non gérée pour facture ID {}", invoice.getId(), e);
                } finally {
                    long durationInvoice = System.currentTimeMillis() - startInvoice;
                    log.debug("Facture {} traitée en {} ms", invoice.getId(), durationInvoice);
                }
            }

            // Regroupement infos fpour logs globaux
            allCategoriesAFiltrer.addAll(categoriesAFiltrer);
            allCategoryIds.addAll(categoryIds);
            totalItems += items.size();
            totalFilteredInvoices += invoices.size();

            long durationSite = System.currentTimeMillis() - startSite;
            log.debug("== Fin du traitement du site {} ({} factures retenues, {} ms) ==",
                    site.getCode(), invoices.size(), durationSite);
        }

        if (hasProcessedInvoices) {
            log.debug("Catégories à filtrer : {}", allCategoriesAFiltrer.stream().distinct().toList());
            log.debug("IDs des catégories retenues : {}", allCategoryIds.stream().distinct().toList());
            log.debug("Nombre total de factures récupérées sur l'API : {}", totalItems);
            log.debug("Factures après filtrage du statut : {}", totalFilteredInvoices);

            LocalDateTime now = LocalDateTime.now();
            config.setLastInsertPurchases(now);
            log.debug("Date de dernière synchronisation mise à jour : {}", now);
        }

        long durationGlobal = System.currentTimeMillis() - startGlobal;
        log.info("== Fin de la synchronisation globale des factures achats ({} ms) ==", durationGlobal);
    }



    @Scheduled(cron = "${cron.PurchasesV2}")
    public void SyncPurchasesV2() {
        log.info("== Démarrage de la synchronisation des FACTURES ACHATS V2 (Pennylane -> Athénéo) ==");

        OffsetDateTime syncDateTime = LocalDate.now()
                .minusDays(Long.parseLong(config.getDaysBackward()))
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC);

        String statusAFiltrer = config.getStatusAFiltrer();

        List<SiteEntity> sites = siteRepository.findAllByPennylaneAchatTrue();
        boolean hasProcessedInvoices = false;

        for (SiteEntity site : sites) {
            try {
                List<Category> categories = invoiceApi.listAllCategories(site);
                List<String> categoriesAFiltrer = config.getCategoriesAFiltrer();

                List<Long> categoryIds = categories.stream()
                        .filter(c -> categoriesAFiltrer.contains(c.getLabel()))
                        .map(Category::getId)
                        .filter(Objects::nonNull)
                        .toList();

                List<ChangelogResponse.ChangelogItem> changelogs = invoiceApi.listAllSupplierInvoiceChangelogs(site, syncDateTime);

                if (CollectionUtils.isEmpty(changelogs)) {
                    log.trace("Aucune entrée dans le changelog pour le site : {}", site.getCode());
                    continue;
                }

                for (ChangelogResponse.ChangelogItem changelogItem : changelogs) {
                    try {
                        // On récupère la facture complète
                        SupplierInvoiceResponse.SupplierInvoiceItem invoice =
                                invoiceApi.getSupplierInvoiceById(site, String.valueOf(changelogItem.getId()));

                        if (invoice == null) {
                            log.warn("Impossible de récupérer la facture {}", changelogItem.getId());
                            continue;
                        }

                        CategoryResponse category =
                                accountsApi.getCategoryByUrl(invoice.getCategories().getUrl(), site);

                        if (!categoryIds.contains(category.getId() != null ? category.getId().longValue() : null)) {
                            log.debug("Facture {} ignorée car catégorie {} non autorisée", invoice.getId(), category.getId());
                            continue;
                        }

                        if (StringUtils.hasText(statusAFiltrer)
                                && !statusAFiltrer.equals(invoice.getPaymentStatus())) {
                            log.debug("Facture {} ignorée car statut {} != {}", invoice.getId(), invoice.getPaymentStatus(), statusAFiltrer);
                            continue;
                        }

                        invoiceService.syncInvoice(invoice, site,categoryIds);

                    } catch (ServiceException e) {
                        log.error("Erreur spécifique au service pour facture {}: {}", changelogItem.getId(), e.getMessage(), e);
                    } catch (RestClientException e) {
                        log.error("Erreur RestClient lors de la récupération de la facture {}: {}", changelogItem.getId(), e.getMessage(), e);
                    } catch (Exception e) {
                        log.error("Erreur inattendue sur la facture {}: {}", changelogItem.getId(), e.getMessage(), e);
                    }
                }

            } catch (Exception e) {
                log.error("Erreur inattendue lors du traitement du site {}: {}", site.getCode(), e.getMessage(), e);
            }
        }
    }


    @Scheduled(cron = "${cron.UpdatePurchaseReglement}")
    public void UpdatePurchaseReglement() {
        long startGlobal = System.currentTimeMillis();
        log.info("== Démarrage de la mise à jour des REGLEMENTS : (Pennylane -> Athénéo) ==");

        List<SiteEntity> sites = siteRepository.findAllByPennylaneAchatTrue();
        log.debug("Mise à jour des règlements pour {} sites ...", sites.size());

        sites.forEach(site -> {
            long startSite = System.currentTimeMillis();
            log.debug("== Début du traitement des règlements pour le site {} ==", site.getCode());

            List<String> aFactureList = ecritureRepository.getMajReglement(site.getCode());

            if (CollectionUtils.isEmpty(aFactureList)) {
                log.debug("Aucune V_FACTURE à synchroniser pour le site {}", site.getCode());
                return;
            }

            log.debug("Nombre de règlements à synchroniser pour le site {} : {}", site.getCode(), aFactureList.size());

            aFactureList.forEach(aFacture -> {
                long startInvoice = System.currentTimeMillis();
                log.trace("== Démarrage de la synchronisation du règlement {} (Pennylane -> Athénéo) ==", aFacture);

                try {
                    invoiceService.updateReglements(aFacture, site);
                } catch (final RestClientException e) {
                    log.error("Erreur lors de la communication avec Pennylane", e);
                } catch (final ServiceException e) {
                    log.error("Erreur spécifique au service lors de la synchronisation", e);
                } catch (final Exception e) {
                    log.error("Erreur non gérée", e);
                } finally {
                    long durationInvoice = System.currentTimeMillis() - startInvoice;
                    log.debug("== Fin de la synchronisation du règlement {} (durée : {} ms) ==", aFacture, durationInvoice);
                }
            });

            long durationSite = System.currentTimeMillis() - startSite;
            log.trace("== Fin du traitement des règlements pour le site {} ({} règlements, {} ms) ==",
                    site.getCode(), aFactureList.size(), durationSite);
        });

        long durationGlobal = System.currentTimeMillis() - startGlobal;
        log.info("== Fin globale de la mise à jour des règlements ({} ms) ==", durationGlobal);
    }


    @Scheduled(cron = "${cron.UpdatePurchaseReglementV2}")
    public void UpdatePurchaseReglementV2() {
        log.trace("== Démarrage de la mise à jour des REGLEMENTS V2 : (Pennylane -> Athénéo) ==");

        List<SiteEntity> sites = siteRepository.findAllByPennylaneAchatTrue();

        log.debug("Mise à jour des règlements ...");

        sites.forEach(site -> {

            List<String> aFactureList = ecritureRepository.getMajReglement(site.getCode());

            if (CollectionUtils.isEmpty(aFactureList)) {
                log.trace("Aucune V_FACTURE à synchroniser");
                return;
            }

            log.debug("== Traitement des REGLEMENTS pour le site : {} ==", site.getCode());
            aFactureList.forEach(aFacture -> {
                log.debug("== Démarrage de la synchronisation des REGLEMENTS (Pennylane -> Athénéo) ==");

                try {
                    invoiceService.updateReglementsV2(aFacture, site);
                } catch (final RestClientException e) {
                    log.error("Erreur lors de la communication avec Pennylane", e);
                } catch (final ServiceException e) {
                    log.error("Erreur spécifique au service lors de la synchronisation", e);
                } catch (final Exception e) {
                    log.error("Erreur non gérée", e);
                } finally {
                    log.debug("== Fin de la synchronisation des reglements (Pennylane -> Athénéo) ==");
                }
            });
        });
    }
}
