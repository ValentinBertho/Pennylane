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
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Scheduler pour la synchronisation des factures fournisseurs entre Pennylane et ATHENEO
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
     * Synchronise les factures fournisseurs depuis Pennylane vers ATHENEO (Version 1)
     */
    @Scheduled(cron = "${cron.Purchases}")
    public void SyncPurchases() {
        List<SiteEntity> sites = siteRepository.findAllByPennylaneAchatTrue();
        if (CollectionUtils.isEmpty(sites)) {
            log.trace("[SYNC-ACHATS] Aucun site actif pour la synchronisation des achats");
            return;
        }

        int daysBackward = Integer.parseInt(config.getDaysBackward());
        List<String> statusAFiltrer = config.getStatusAFiltrer();
        List<String> categoriesAFiltrer = config.getCategoriesAFiltrer();

        // Démarrage du flux avec logging amélioré
        String correlationId = flowLogger.startSyncAchats(daysBackward, statusAFiltrer, categoriesAFiltrer);

        OffsetDateTime syncDateTime = LocalDate.now()
                .minusDays(daysBackward)
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC);

        // Compteurs pour le bilan
        AtomicInteger totalFacturesRecuperees = new AtomicInteger(0);
        AtomicInteger totalFacturesRetenues = new AtomicInteger(0);
        AtomicInteger facturesImportees = new AtomicInteger(0);
        AtomicInteger facturesIgnorees = new AtomicInteger(0);
        AtomicInteger fournisseursCrees = new AtomicInteger(0);
        AtomicInteger documentsTelechargees = new AtomicInteger(0);

        boolean hasProcessedInvoices = false;

        for (SiteEntity site : sites) {
            long startSite = System.currentTimeMillis();
            log.info("[SYNC-ACHATS] Traitement du site {} ...", site.getCode());

            // Récupération catégories
            long startCategories = System.currentTimeMillis();
            List<Category> categories = categoryCacheService.getCategories(site);
            long durationCategories = System.currentTimeMillis() - startCategories;
            log.debug("[SYNC-ACHATS] Site {} - Récupération des catégories en {} ms", site.getCode(), durationCategories);

            List<Long> categoryIds = categories.stream()
                    .filter(c -> categoriesAFiltrer.contains(c.getLabel()))
                    .map(Category::getId)
                    .filter(Objects::nonNull)
                    .toList();

            // Vérification : correspondance entre les deux listes
            if (categoriesAFiltrer.size() != categoryIds.size()) {
                log.warn("[SYNC-ACHATS] ⚠ Les catégories configurées et les catégories trouvées ne correspondent pas");
                log.warn("[SYNC-ACHATS]   categoriesAFiltrer={} ({}), categoryIds={} ({})",
                        categoriesAFiltrer, categoriesAFiltrer.size(),
                        categoryIds, categoryIds.size());
                log.warn("[SYNC-ACHATS] Tâche arrêtée pour éviter une incohérence");

                flowLogger.endFlow(correlationId, Map.of(
                    "Statut", "INTERROMPU",
                    "Raison", "Incohérence des catégories"
                ));
                return;
            }

            // Récupération factures
            long startInvoicesApi = System.currentTimeMillis();
            List<SupplierInvoiceResponse.SupplierInvoiceItem> items = invoiceApi.listAllSupplierInvoices(site, categoryIds, syncDateTime);
            long durationInvoicesApi = System.currentTimeMillis() - startInvoicesApi;

            totalFacturesRecuperees.addAndGet(items.size());
            log.info("[SYNC-ACHATS] Site {} - {} factures récupérées en {} ms",
                    site.getCode(), items.size(), durationInvoicesApi);

            // Filtrage factures
            List<SupplierInvoiceResponse.SupplierInvoiceItem> invoices = items.stream()
                    .filter(invoice -> statusAFiltrer == null || statusAFiltrer.isEmpty()
                            || statusAFiltrer.contains(invoice.getPaymentStatus()))
                    .toList();

            totalFacturesRetenues.addAndGet(invoices.size());
            log.info("[SYNC-ACHATS] Site {} - {} factures retenues après filtrage", site.getCode(), invoices.size());

            if (CollectionUtils.isEmpty(invoices)) {
                log.debug("[SYNC-ACHATS] Aucune facture à synchroniser pour le site {}", site.getCode());
                continue;
            }

            for (SupplierInvoiceResponse.SupplierInvoiceItem invoice : invoices) {
                long startInvoice = System.currentTimeMillis();
                try {
                    InvoiceService.SyncResult result = invoiceService.syncInvoice(invoice, site, categoryIds);
                    hasProcessedInvoices = true;

                    if (result.isCreated()) {
                        facturesImportees.incrementAndGet();
                        if (result.isFournisseurCree()) fournisseursCrees.incrementAndGet();
                        if (result.isDocumentTelecharge()) documentsTelechargees.incrementAndGet();
                        log.info("[SYNC-ACHATS] [{}] ✓ Facture importée", invoice.getId());
                    } else {
                        facturesIgnorees.incrementAndGet();
                        flowLogger.infoFactureIgnoree(FlowType.SYNC_ACHATS, String.valueOf(invoice.getId()), "Facture déjà existante");
                    }

                } catch (final RestClientException e) {
                    flowLogger.errorApiPennylane(FlowType.SYNC_ACHATS, String.valueOf(invoice.getId()), 0, e.getMessage());
                } catch (final ServiceException e) {
                    log.error("[SYNC-ACHATS] [{}] ✗ Erreur service: {}", invoice.getId(), e.getMessage());
                } catch (final Exception e) {
                    log.error("[SYNC-ACHATS] [{}] ✗ Erreur inattendue: {}", invoice.getId(), e.getMessage(), e);
                } finally {
                    long durationInvoice = System.currentTimeMillis() - startInvoice;
                    log.debug("[SYNC-ACHATS] [{}] Traitement terminé en {} ms", invoice.getId(), durationInvoice);
                }
            }

            long durationSite = System.currentTimeMillis() - startSite;
            log.info("[SYNC-ACHATS] Fin du traitement du site {} ({} ms)", site.getCode(), durationSite);
        }

        if (hasProcessedInvoices) {
            LocalDateTime now = LocalDateTime.now();
            config.setLastInsertPurchases(now);
            log.debug("[SYNC-ACHATS] Date de dernière synchronisation mise à jour: {}", now);
        }

        // Fin du flux avec bilan
        flowLogger.endSyncAchats(correlationId,
            totalFacturesRecuperees.get(), totalFacturesRetenues.get(),
            facturesImportees.get(), facturesIgnorees.get(),
            fournisseursCrees.get(), documentsTelechargees.get());
    }



    @Scheduled(cron = "${cron.PurchasesV2}")
    public void SyncPurchasesV2() {
        List<SiteEntity> sites = siteRepository.findAllByPennylaneAchatTrue();
        if (CollectionUtils.isEmpty(sites)) {
            log.trace("[SYNC-ACHATS-V2] Aucun site actif pour la synchronisation des achats");
            return;
        }

        int daysBackward = Integer.parseInt(config.getDaysBackward());
        List<String> statusAFiltrer = config.getStatusAFiltrer();
        List<String> categoriesAFiltrer = config.getCategoriesAFiltrer();

        // Démarrage du flux avec logging amélioré
        String correlationId = flowLogger.startFlow(FlowType.SYNC_ACHATS_V2,
            Map.of("Période", daysBackward + " jours",
                   "Statuts", statusAFiltrer.toString(),
                   "Catégories", categoriesAFiltrer.toString()));

        OffsetDateTime syncDateTime = LocalDate.now()
                .minusDays(daysBackward)
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC);

        // Compteurs pour le bilan
        AtomicInteger changelogsTraites = new AtomicInteger(0);
        AtomicInteger facturesImportees = new AtomicInteger(0);
        AtomicInteger facturesIgnorees = new AtomicInteger(0);
        AtomicInteger erreurs = new AtomicInteger(0);

        for (SiteEntity site : sites) {
            try {
                log.info("[SYNC-ACHATS-V2] Traitement du site {} ...", site.getCode());

                List<Category> categories = invoiceApi.listAllCategories(site);

                List<Long> categoryIds = categories.stream()
                        .filter(c -> categoriesAFiltrer.contains(c.getLabel()))
                        .map(Category::getId)
                        .filter(Objects::nonNull)
                        .toList();

                List<ChangelogResponse.ChangelogItem> changelogs = invoiceApi.listAllSupplierInvoiceChangelogs(site, syncDateTime);

                if (CollectionUtils.isEmpty(changelogs)) {
                    log.debug("[SYNC-ACHATS-V2] Aucune entrée dans le changelog pour le site {}", site.getCode());
                    continue;
                }

                log.info("[SYNC-ACHATS-V2] {} entrées changelog à traiter pour le site {}", changelogs.size(), site.getCode());

                for (ChangelogResponse.ChangelogItem changelogItem : changelogs) {
                    try {
                        changelogsTraites.incrementAndGet();

                        SupplierInvoiceResponse.SupplierInvoiceItem invoice =
                                invoiceApi.getSupplierInvoiceById(site, String.valueOf(changelogItem.getId()));

                        if (invoice == null) {
                            log.warn("[SYNC-ACHATS-V2] [{}] Impossible de récupérer la facture", changelogItem.getId());
                            facturesIgnorees.incrementAndGet();
                            continue;
                        }

                        CategoryResponse category =
                                accountsApi.getCategoryByUrl(invoice.getCategories().getUrl(), site);

                        if (!categoryIds.contains(category.getId() != null ? category.getId().longValue() : null)) {
                            log.debug("[SYNC-ACHATS-V2] [{}] Facture ignorée: catégorie {} non autorisée",
                                invoice.getId(), category.getId());
                            facturesIgnorees.incrementAndGet();
                            continue;
                        }

                        if (!CollectionUtils.isEmpty(statusAFiltrer)
                                && !statusAFiltrer.contains(invoice.getPaymentStatus())) {
                            log.debug("[SYNC-ACHATS-V2] [{}] Facture ignorée: statut {} non autorisé",
                                invoice.getId(), invoice.getPaymentStatus());
                            facturesIgnorees.incrementAndGet();
                            continue;
                        }

                        invoiceService.syncInvoice(invoice, site, categoryIds);
                        facturesImportees.incrementAndGet();
                        log.info("[SYNC-ACHATS-V2] [{}] ✓ Facture importée", invoice.getId());

                    } catch (ServiceException e) {
                        erreurs.incrementAndGet();
                        log.error("[SYNC-ACHATS-V2] [{}] ✗ Erreur service: {}", changelogItem.getId(), e.getMessage());
                    } catch (RestClientException e) {
                        erreurs.incrementAndGet();
                        log.error("[SYNC-ACHATS-V2] [{}] ✗ Erreur API: {}", changelogItem.getId(), e.getMessage());
                    } catch (Exception e) {
                        erreurs.incrementAndGet();
                        log.error("[SYNC-ACHATS-V2] [{}] ✗ Erreur inattendue: {}", changelogItem.getId(), e.getMessage(), e);
                    }
                }

            } catch (Exception e) {
                log.error("[SYNC-ACHATS-V2] ✗ Erreur lors du traitement du site {}: {}", site.getCode(), e.getMessage(), e);
            }
        }

        // Fin du flux avec bilan
        Map<String, Object> stats = new HashMap<>();
        stats.put("Changelogs traités", changelogsTraites.get());
        stats.put("Factures importées", facturesImportees.get());
        stats.put("Factures ignorées", facturesIgnorees.get());
        stats.put("Erreurs", erreurs.get());
        flowLogger.endFlow(correlationId, stats);
    }


    @Scheduled(cron = "${cron.UpdatePurchaseReglement}")
    public void UpdatePurchaseReglement() {
        List<SiteEntity> sites = siteRepository.findAllByPennylaneAchatTrue();
        if (CollectionUtils.isEmpty(sites)) {
            log.trace("[SYNC-REGLEMENTS] Aucun site actif pour la mise à jour des règlements");
            return;
        }

        // Démarrage du flux avec logging amélioré
        String correlationId = flowLogger.startSyncReglements(sites.size());

        // Compteurs pour le bilan
        AtomicInteger reglementsTraites = new AtomicInteger(0);
        AtomicInteger reglementsEnErreur = new AtomicInteger(0);
        AtomicInteger facturesPayees = new AtomicInteger(0);
        AtomicInteger facturesPartielles = new AtomicInteger(0);
        AtomicInteger surpaiements = new AtomicInteger(0);

        sites.forEach(site -> {
            long startSite = System.currentTimeMillis();
            log.info("[SYNC-REGLEMENTS] Traitement du site {} ...", site.getCode());

            List<String> aFactureList = ecritureRepository.getMajReglement(site.getCode());

            if (CollectionUtils.isEmpty(aFactureList)) {
                log.debug("[SYNC-REGLEMENTS] Aucune facture à mettre à jour pour le site {}", site.getCode());
                return;
            }

            log.info("[SYNC-REGLEMENTS] {} règlement(s) à synchroniser pour le site {}", aFactureList.size(), site.getCode());

            aFactureList.forEach(aFacture -> {
                long startInvoice = System.currentTimeMillis();
                log.debug("[SYNC-REGLEMENTS] [{}] Mise à jour en cours...", aFacture);

                try {
                    InvoiceService.ReglementResult result = invoiceService.updateReglements(aFacture, site);
                    reglementsTraites.incrementAndGet();

                    // Logging du statut de paiement
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
                        default -> log.info("[SYNC-REGLEMENTS] [{}] ✓ Statut: {}", aFacture, result.getStatut());
                    }

                } catch (final RestClientException e) {
                    reglementsEnErreur.incrementAndGet();
                    flowLogger.errorApiPennylane(FlowType.SYNC_REGLEMENTS, aFacture, 0, e.getMessage());
                } catch (final ServiceException e) {
                    reglementsEnErreur.incrementAndGet();
                    log.error("[SYNC-REGLEMENTS] [{}] ✗ Erreur service: {}", aFacture, e.getMessage());
                } catch (final Exception e) {
                    reglementsEnErreur.incrementAndGet();
                    log.error("[SYNC-REGLEMENTS] [{}] ✗ Erreur inattendue: {}", aFacture, e.getMessage(), e);
                } finally {
                    long durationInvoice = System.currentTimeMillis() - startInvoice;
                    log.debug("[SYNC-REGLEMENTS] [{}] Traitement terminé en {} ms", aFacture, durationInvoice);
                }
            });

            long durationSite = System.currentTimeMillis() - startSite;
            log.info("[SYNC-REGLEMENTS] Fin du traitement du site {} ({} ms)", site.getCode(), durationSite);
        });

        // Fin du flux avec bilan
        Map<String, Object> stats = new HashMap<>();
        stats.put("Règlements traités", reglementsTraites.get());
        stats.put("Factures entièrement payées", facturesPayees.get());
        stats.put("Factures partiellement payées", facturesPartielles.get());
        stats.put("Surpaiements détectés", surpaiements.get());
        stats.put("Erreurs", reglementsEnErreur.get());
        flowLogger.endFlow(correlationId, stats);
    }


    @Scheduled(cron = "${cron.UpdatePurchaseReglementV2}")
    public void UpdatePurchaseReglementV2() {
        List<SiteEntity> sites = siteRepository.findAllByPennylaneAchatTrue();
        if (CollectionUtils.isEmpty(sites)) {
            log.trace("[SYNC-REGLEMENTS-V2] Aucun site actif pour la mise à jour des règlements");
            return;
        }

        // Démarrage du flux avec logging amélioré
        String correlationId = flowLogger.startFlow(FlowType.SYNC_REGLEMENTS_V2,
            Map.of("Sites", sites.size()));

        // Compteurs pour le bilan
        AtomicInteger reglementsTraites = new AtomicInteger(0);
        AtomicInteger reglementsEnErreur = new AtomicInteger(0);

        sites.forEach(site -> {
            log.info("[SYNC-REGLEMENTS-V2] Traitement du site {} ...", site.getCode());

            List<String> aFactureList = ecritureRepository.getMajReglement(site.getCode());

            if (CollectionUtils.isEmpty(aFactureList)) {
                log.debug("[SYNC-REGLEMENTS-V2] Aucune facture à mettre à jour pour le site {}", site.getCode());
                return;
            }

            log.info("[SYNC-REGLEMENTS-V2] {} règlement(s) à traiter pour le site {}", aFactureList.size(), site.getCode());

            aFactureList.forEach(aFacture -> {
                long startInvoice = System.currentTimeMillis();
                log.debug("[SYNC-REGLEMENTS-V2] [{}] Mise à jour en cours...", aFacture);

                try {
                    invoiceService.updateReglementsV2(aFacture, site);
                    reglementsTraites.incrementAndGet();
                    log.info("[SYNC-REGLEMENTS-V2] [{}] ✓ Règlement mis à jour", aFacture);
                } catch (final RestClientException e) {
                    reglementsEnErreur.incrementAndGet();
                    log.error("[SYNC-REGLEMENTS-V2] [{}] ✗ Erreur API Pennylane: {}", aFacture, e.getMessage());
                } catch (final ServiceException e) {
                    reglementsEnErreur.incrementAndGet();
                    log.error("[SYNC-REGLEMENTS-V2] [{}] ✗ Erreur service: {}", aFacture, e.getMessage());
                } catch (final Exception e) {
                    reglementsEnErreur.incrementAndGet();
                    log.error("[SYNC-REGLEMENTS-V2] [{}] ✗ Erreur inattendue: {}", aFacture, e.getMessage(), e);
                } finally {
                    long durationInvoice = System.currentTimeMillis() - startInvoice;
                    log.debug("[SYNC-REGLEMENTS-V2] [{}] Traitement terminé en {} ms", aFacture, durationInvoice);
                }
            });

            log.info("[SYNC-REGLEMENTS-V2] Fin du traitement du site {}", site.getCode());
        });

        // Fin du flux avec bilan
        Map<String, Object> stats = new HashMap<>();
        stats.put("Règlements traités", reglementsTraites.get());
        stats.put("Erreurs", reglementsEnErreur.get());
        flowLogger.endFlow(correlationId, stats);
    }
}
