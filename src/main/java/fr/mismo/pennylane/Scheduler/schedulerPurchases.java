package fr.mismo.pennylane.Scheduler;

import fr.mismo.pennylane.api.AccountsApi;
import fr.mismo.pennylane.api.InvoiceApi;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dao.repository.EcritureRepository;
import fr.mismo.pennylane.dao.repository.SiteRepository;
import fr.mismo.pennylane.dto.Category;
import fr.mismo.pennylane.dto.invoice.*;
import fr.mismo.pennylane.logging.FlowLogger;
import fr.mismo.pennylane.logging.FlowType;
import fr.mismo.pennylane.service.CategoryCacheService;
import fr.mismo.pennylane.service.InvoiceService;
import fr.mismo.pennylane.settings.Config;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduler pour la synchronisation des factures fournisseurs entre Pennylane et ATHENEO.
 * Principe : aucun log si rien à traiter, bilan complet sinon.
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

    @Autowired
    FlowLogger flowLogger;

    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * Synchronise les factures fournisseurs depuis Pennylane vers ATHENEO (V1).
     */
    @Scheduled(cron = "${cron.Purchases}")
    public void SyncPurchases() {
        List<SiteEntity> sites = siteRepository.findAllByPennylaneAchatTrue();
        if (CollectionUtils.isEmpty(sites)) return;

        int daysBackward = Integer.parseInt(config.getDaysBackward());
        List<String> statusAFiltrer = config.getStatusAFiltrer();
        List<String> categoriesAFiltrer = config.getCategoriesAFiltrer();

        String correlationId = flowLogger.startSyncAchats(daysBackward, statusAFiltrer, categoriesAFiltrer);

        OffsetDateTime syncDateTime = LocalDate.now()
                .minusDays(daysBackward)
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC);

        AtomicInteger totalFacturesRecuperees = new AtomicInteger(0);
        AtomicInteger totalFacturesRetenues = new AtomicInteger(0);
        AtomicInteger facturesImportees = new AtomicInteger(0);
        AtomicInteger facturesIgnorees = new AtomicInteger(0);
        AtomicInteger fournisseursCrees = new AtomicInteger(0);
        AtomicInteger documentsTelechargees = new AtomicInteger(0);

        boolean hasProcessedInvoices = false;

        for (SiteEntity site : sites) {
            // Récupération catégories
            List<Category> categories = categoryCacheService.getCategories(site);

            List<Long> categoryIds = categories.stream()
                    .filter(c -> categoriesAFiltrer.contains(c.getLabel()))
                    .map(Category::getId)
                    .filter(Objects::nonNull)
                    .toList();

            if (categoriesAFiltrer.size() != categoryIds.size()) {
                List<String> categoriesTrouvees = categories.stream()
                        .filter(c -> categoryIds.contains(c.getId()))
                        .map(Category::getLabel)
                        .toList();
                List<String> categoriesManquantes = categoriesAFiltrer.stream()
                        .filter(configured -> !categoriesTrouvees.contains(configured))
                        .toList();

                log.warn("[SYNC-ACHATS] Incohérence catégories pour le site {} (configurées: {}, trouvées: {}, manquantes: {}), site ignoré",
                        site.getCode(), categoriesAFiltrer.size(), categoryIds.size(), categoriesManquantes);
                continue;
            }

            // Récupération factures
            List<SupplierInvoiceResponse.SupplierInvoiceItem> items = invoiceApi.listAllSupplierInvoices(site, categoryIds, syncDateTime);
            if (items == null) {
                log.warn("[SYNC-ACHATS] Site {} : récupération des factures impossible (retour API null)", site.getCode());
                continue;
            }
            totalFacturesRecuperees.addAndGet(items.size());

            // Filtrage factures
            List<SupplierInvoiceResponse.SupplierInvoiceItem> invoices = items.stream()
                    .filter(invoice -> statusAFiltrer == null || statusAFiltrer.isEmpty()
                            || statusAFiltrer.contains(invoice.getPaymentStatus()))
                    .toList();

            totalFacturesRetenues.addAndGet(invoices.size());

            if (CollectionUtils.isEmpty(invoices)) continue;

            log.debug("[SYNC-ACHATS] Site {} : {} factures retenues sur {} récupérées",
                    site.getCode(), invoices.size(), items.size());

            for (SupplierInvoiceResponse.SupplierInvoiceItem invoice : invoices) {
                try {
                    InvoiceService.SyncResult result = invoiceService.syncInvoice(invoice, site, categoryIds);
                    hasProcessedInvoices = true;

                    if (result.isCreated()) {
                        facturesImportees.incrementAndGet();
                        if (result.isFournisseurCree()) fournisseursCrees.incrementAndGet();
                        if (result.isDocumentTelecharge()) documentsTelechargees.incrementAndGet();
                    } else {
                        facturesIgnorees.incrementAndGet();
                        flowLogger.infoFactureIgnoree(FlowType.SYNC_ACHATS, String.valueOf(invoice.getId()), "Facture déjà existante");
                    }

                } catch (final RestClientException e) {
                    flowLogger.errorApiPennylane(FlowType.SYNC_ACHATS, String.valueOf(invoice.getId()), 0, e.getMessage());
                } catch (final ServiceException e) {
                    log.error("[SYNC-ACHATS] [{}] Erreur service: {}", invoice.getId(), e.getMessage());
                } catch (final Exception e) {
                    log.error("[SYNC-ACHATS] [{}] Erreur: {}", invoice.getId(), e.getMessage(), e);
                }
            }
        }

        if (hasProcessedInvoices) {
            LocalDateTime now = LocalDateTime.now();
            config.setLastInsertPurchases(now);
        }

        flowLogger.endSyncAchats(correlationId,
            totalFacturesRecuperees.get(), totalFacturesRetenues.get(),
            facturesImportees.get(), facturesIgnorees.get(),
            fournisseursCrees.get(), documentsTelechargees.get());
    }

    @Scheduled(cron = "${cron.PurchasesV2}")
    public void SyncPurchasesV2() {
        List<SiteEntity> sites = siteRepository.findAllByPennylaneAchatTrue();
        if (CollectionUtils.isEmpty(sites)) return;

        int daysBackward = Integer.parseInt(config.getDaysBackward());
        List<String> statusAFiltrer = config.getStatusAFiltrer();
        List<String> categoriesAFiltrer = config.getCategoriesAFiltrer();

        String correlationId = flowLogger.startFlow(FlowType.SYNC_ACHATS_V2,
            Map.of("Période", daysBackward + " jours",
                   "Statuts", statusAFiltrer.toString(),
                   "Catégories", categoriesAFiltrer.toString()));

        OffsetDateTime syncDateTime = LocalDate.now()
                .minusDays(daysBackward)
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC);

        AtomicInteger changelogsTraites = new AtomicInteger(0);
        AtomicInteger facturesImportees = new AtomicInteger(0);
        AtomicInteger facturesIgnorees = new AtomicInteger(0);
        AtomicInteger erreurs = new AtomicInteger(0);

        for (SiteEntity site : sites) {
            try {
                List<Category> categories = invoiceApi.listAllCategories(site);

                List<Long> categoryIds = categories.stream()
                        .filter(c -> categoriesAFiltrer.contains(c.getLabel()))
                        .map(Category::getId)
                        .filter(Objects::nonNull)
                        .toList();

                if (categoriesAFiltrer.size() != categoryIds.size()) {
                    List<String> categoriesTrouvees = categories.stream()
                            .filter(c -> categoryIds.contains(c.getId()))
                            .map(Category::getLabel)
                            .toList();
                    List<String> categoriesManquantes = categoriesAFiltrer.stream()
                            .filter(configured -> !categoriesTrouvees.contains(configured))
                            .toList();
                    log.warn("[SYNC-ACHATS-V2] Incohérence catégories pour le site {} (manquantes: {}), site ignoré",
                            site.getCode(), categoriesManquantes);
                    continue;
                }

                List<ChangelogResponse.ChangelogItem> changelogs = invoiceApi.listAllSupplierInvoiceChangelogs(site, syncDateTime);

                if (CollectionUtils.isEmpty(changelogs)) continue;

                log.debug("[SYNC-ACHATS-V2] Site {} : {} entrées changelog", site.getCode(), changelogs.size());

                for (ChangelogResponse.ChangelogItem changelogItem : changelogs) {
                    try {
                        changelogsTraites.incrementAndGet();

                        SupplierInvoiceResponse.SupplierInvoiceItem invoice =
                                invoiceApi.getSupplierInvoiceById(site, String.valueOf(changelogItem.getId()));

                        if (invoice == null) {
                            facturesIgnorees.incrementAndGet();
                            continue;
                        }

                        CategoryResponse category =
                                accountsApi.getCategoryByUrl(invoice.getCategories().getUrl(), site);

                        if (!categoryIds.contains(category.getId() != null ? category.getId().longValue() : null)) {
                            facturesIgnorees.incrementAndGet();
                            continue;
                        }

                        if (!CollectionUtils.isEmpty(statusAFiltrer)
                                && !statusAFiltrer.contains(invoice.getPaymentStatus())) {
                            facturesIgnorees.incrementAndGet();
                            continue;
                        }

                        invoiceService.syncInvoice(invoice, site, categoryIds);
                        facturesImportees.incrementAndGet();

                    } catch (ServiceException e) {
                        erreurs.incrementAndGet();
                        log.error("[SYNC-ACHATS-V2] [{}] Erreur service: {}", changelogItem.getId(), e.getMessage());
                    } catch (RestClientException e) {
                        erreurs.incrementAndGet();
                        log.error("[SYNC-ACHATS-V2] [{}] Erreur API: {}", changelogItem.getId(), e.getMessage());
                    } catch (Exception e) {
                        erreurs.incrementAndGet();
                        log.error("[SYNC-ACHATS-V2] [{}] Erreur: {}", changelogItem.getId(), e.getMessage(), e);
                    }
                }

            } catch (Exception e) {
                log.error("[SYNC-ACHATS-V2] Erreur site {}: {}", site.getCode(), e.getMessage(), e);
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("Changelogs traités", changelogsTraites.get());
        stats.put("Factures importées", facturesImportees.get());
        if (facturesIgnorees.get() > 0)
            stats.put("Factures ignorées", facturesIgnorees.get());
        if (erreurs.get() > 0)
            stats.put("Erreurs", erreurs.get());
        flowLogger.endFlow(correlationId, stats);
    }

    @Scheduled(cron = "${cron.UpdatePurchaseReglement}")
    public void UpdatePurchaseReglement() {
        List<SiteEntity> sites = siteRepository.findAllByPennylaneAchatTrue();
        if (CollectionUtils.isEmpty(sites)) return;

        String correlationId = flowLogger.startSyncReglements(sites.size());

        AtomicInteger reglementsTraites = new AtomicInteger(0);
        AtomicInteger reglementsEnErreur = new AtomicInteger(0);
        AtomicInteger facturesPayees = new AtomicInteger(0);
        AtomicInteger facturesPartielles = new AtomicInteger(0);
        AtomicInteger surpaiements = new AtomicInteger(0);

        sites.forEach(site -> {
            List<String> aFactureList = ecritureRepository.getMajReglement(site.getCode());
            if (CollectionUtils.isEmpty(aFactureList)) return;

            log.debug("[SYNC-REGLEMENTS] Site {} : {} règlement(s)", site.getCode(), aFactureList.size());

            aFactureList.forEach(aFacture -> {
                try {
                    InvoiceService.ReglementResult result = invoiceService.updateReglements(aFacture, site);
                    reglementsTraites.incrementAndGet();

                    switch (result.getStatut()) {
                        case "FULLY_PAID" -> {
                            facturesPayees.incrementAndGet();
                            flowLogger.logStatutPaiement(FlowType.SYNC_REGLEMENTS, aFacture,
                                "ENTIÈREMENT PAYÉE", result.getMontantPaye(), result.getMontantTotal());
                        }
                        case "PARTIALLY_PAID" -> {
                            facturesPartielles.incrementAndGet();
                            flowLogger.logStatutPaiement(FlowType.SYNC_REGLEMENTS, aFacture,
                                "PARTIELLEMENT PAYÉE", result.getMontantPaye(), result.getMontantTotal());
                        }
                        case "OVERPAID" -> {
                            surpaiements.incrementAndGet();
                            flowLogger.warnSurpaiement(FlowType.SYNC_REGLEMENTS, aFacture,
                                result.getMontantTotal(), result.getMontantPaye(),
                                result.getMontantPaye() - result.getMontantTotal());
                        }
                        default -> log.debug("[SYNC-REGLEMENTS] [{}] Statut: {}", aFacture, result.getStatut());
                    }

                } catch (final RestClientException e) {
                    reglementsEnErreur.incrementAndGet();
                    flowLogger.errorApiPennylane(FlowType.SYNC_REGLEMENTS, aFacture, 0, e.getMessage());
                } catch (final ServiceException e) {
                    reglementsEnErreur.incrementAndGet();
                    log.error("[SYNC-REGLEMENTS] [{}] Erreur service: {}", aFacture, e.getMessage());
                } catch (final Exception e) {
                    reglementsEnErreur.incrementAndGet();
                    log.error("[SYNC-REGLEMENTS] [{}] Erreur: {}", aFacture, e.getMessage(), e);
                }
            });
        });

        Map<String, Object> stats = new HashMap<>();
        stats.put("Règlements traités", reglementsTraites.get());
        if (facturesPayees.get() > 0)
            stats.put("Factures payées", facturesPayees.get());
        if (facturesPartielles.get() > 0)
            stats.put("Factures partielles", facturesPartielles.get());
        if (surpaiements.get() > 0)
            stats.put("Surpaiements", surpaiements.get());
        if (reglementsEnErreur.get() > 0)
            stats.put("Erreurs", reglementsEnErreur.get());
        flowLogger.endFlow(correlationId, stats);
    }

    @Scheduled(cron = "${cron.UpdatePurchaseReglementV2}")
    public void UpdatePurchaseReglementV2() {
        List<SiteEntity> sites = siteRepository.findAllByPennylaneAchatTrue();
        if (CollectionUtils.isEmpty(sites)) return;

        String correlationId = flowLogger.startFlow(FlowType.SYNC_REGLEMENTS_V2,
            Map.of("Sites", sites.size()));

        AtomicInteger reglementsTraites = new AtomicInteger(0);
        AtomicInteger reglementsEnErreur = new AtomicInteger(0);

        sites.forEach(site -> {
            List<String> aFactureList = ecritureRepository.getMajReglement(site.getCode());
            if (CollectionUtils.isEmpty(aFactureList)) return;

            log.debug("[SYNC-REGLEMENTS-V2] Site {} : {} règlement(s)", site.getCode(), aFactureList.size());

            aFactureList.forEach(aFacture -> {
                try {
                    invoiceService.updateReglementsV2(aFacture, site);
                    reglementsTraites.incrementAndGet();
                } catch (final RestClientException e) {
                    reglementsEnErreur.incrementAndGet();
                    log.error("[SYNC-REGLEMENTS-V2] [{}] Erreur API: {}", aFacture, e.getMessage());
                } catch (final ServiceException e) {
                    reglementsEnErreur.incrementAndGet();
                    log.error("[SYNC-REGLEMENTS-V2] [{}] Erreur service: {}", aFacture, e.getMessage());
                } catch (final Exception e) {
                    reglementsEnErreur.incrementAndGet();
                    log.error("[SYNC-REGLEMENTS-V2] [{}] Erreur: {}", aFacture, e.getMessage(), e);
                }
            });
        });

        Map<String, Object> stats = new HashMap<>();
        stats.put("Règlements traités", reglementsTraites.get());
        if (reglementsEnErreur.get() > 0)
            stats.put("Erreurs", reglementsEnErreur.get());
        flowLogger.endFlow(correlationId, stats);
    }
}
