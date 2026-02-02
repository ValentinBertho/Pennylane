package fr.mismo.pennylane.Scheduler;

import fr.mismo.pennylane.api.AccountsApi;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dao.repository.EcritureRepository;
import fr.mismo.pennylane.dao.repository.LogRepository;
import fr.mismo.pennylane.dao.repository.SiteRepository;
import fr.mismo.pennylane.dto.accounting.Item;
import fr.mismo.pennylane.logging.FlowLogger;
import fr.mismo.pennylane.logging.FlowType;
import fr.mismo.pennylane.service.AccountingService;
import fr.mismo.pennylane.service.InvoiceService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Scheduler pour la synchronisation des données comptables entre ATHENEO et Pennylane
 */
@Component
@Slf4j
public class schedulerAccounting {

    @Autowired
    private EcritureRepository ecritureRepository;

    @Autowired
    private AccountingService accountingService;

    @Autowired
    private AccountsApi accountsApi;

    @Autowired
    SiteRepository siteRepository;

    @Autowired
    InvoiceService invoiceService;

    @Autowired
    LogRepository logRepository;

    @Autowired
    FlowLogger flowLogger;

    private void handleException(Integer id, Exception e) {
        if (e instanceof RestClientException) {
            log.error("Erreur lors de la communication avec Pennylane", e);
        } else if (e instanceof ServiceException) {
            accountingService.processError(id, e);
        } else {
            log.error("Erreur non gérée", e);
        }
    }

    /**
     * Synchronise les écritures comptables depuis ATHENEO vers Pennylane.
     * Ne produit aucun log si aucun lot n'est à traiter.
     */
    @Scheduled(cron = "${cron.Entries}")
    public void syncEntries() {
        List<SiteEntity> sites = siteRepository.findAllByPennylaneActifTrue();
        if (CollectionUtils.isEmpty(sites)) return;

        // Compter le total des lots à traiter
        AtomicInteger totalLots = new AtomicInteger(0);
        sites.forEach(site -> {
            List<Integer> lots = ecritureRepository.getLotEcritureToExport(site.getId()).stream().toList();
            totalLots.addAndGet(lots.size());
        });

        // Rien à faire → aucun log
        if (totalLots.get() == 0) return;

        // Il y a du travail : on lance le flux
        String correlationId = flowLogger.startSyncEcritures(totalLots.get(), sites.size());

        AtomicReference<List<Item>> accountPennylane = new AtomicReference<>(new ArrayList<>());

        AtomicInteger lotsTraites = new AtomicInteger(0);
        AtomicInteger facturesCrees = new AtomicInteger(0);
        AtomicInteger facturesIgnorees = new AtomicInteger(0);
        AtomicInteger clientsCrees = new AtomicInteger(0);
        AtomicInteger produitsCrees = new AtomicInteger(0);
        AtomicInteger documentsUploades = new AtomicInteger(0);
        List<String> erreurs = new ArrayList<>();

        sites.forEach(site -> {
            List<Integer> ecrituresList = ecritureRepository.getLotEcritureToExport(site.getId()).stream().toList();
            if (CollectionUtils.isEmpty(ecrituresList)) return;

            log.debug("[SYNC-ECRITURES] {} lot(s) pour le site {}", ecrituresList.size(), site.getCode());

            accountPennylane.set(accountsApi.listAllLedgerAccounts(site));
            List<Item> finalAccountPennylane = accountPennylane.get();

            ecrituresList.forEach(ecriture -> {
                try {
                    AccountingService.SyncResult result = accountingService.syncEcriture(ecriture, site, finalAccountPennylane);

                    lotsTraites.incrementAndGet();
                    facturesCrees.addAndGet(result.getFacturesCrees());
                    facturesIgnorees.addAndGet(result.getFacturesIgnorees());
                    clientsCrees.addAndGet(result.getClientsCrees());
                    produitsCrees.addAndGet(result.getProduitsCrees());
                    documentsUploades.addAndGet(result.getDocumentsUploades());

                    if (result.hasErrors()) {
                        erreurs.addAll(result.getErreurs());
                    }

                } catch (Exception e) {
                    handleException(ecriture, e);
                    erreurs.add(String.format("LOT-%d: %s", ecriture, e.getMessage()));
                }
            });
        });

        flowLogger.endSyncEcritures(correlationId, lotsTraites.get(), totalLots.get(),
            facturesCrees.get(), facturesIgnorees.get(),
            clientsCrees.get(), produitsCrees.get(),
            documentsUploades.get(), erreurs);
    }

    /**
     * Met à jour le statut "Bon À Payer" (BAP) des factures clients dans Pennylane.
     * Ne produit aucun log si aucune facture n'est à traiter.
     */
    @Scheduled(cron = "${cron.UpdateSale}")
    public void UpdateSale() {
        List<SiteEntity> sites = siteRepository.findAllByPennylaneAchatTrue();
        if (CollectionUtils.isEmpty(sites)) return;

        String correlationId = flowLogger.startFlow(FlowType.SYNC_BAP);

        AtomicInteger facturesTraitees = new AtomicInteger(0);
        AtomicInteger facturesEnErreur = new AtomicInteger(0);

        sites.forEach(site -> {
            List<String> aFactureList = ecritureRepository.getAFactureBAP(site.getCode());
            if (CollectionUtils.isEmpty(aFactureList)) return;

            log.debug("[SYNC-BAP] {} facture(s) BAP pour le site {}", aFactureList.size(), site.getCode());

            aFactureList.forEach(aFacture -> {
                try {
                    invoiceService.updateInvoice(aFacture, site);
                    facturesTraitees.incrementAndGet();
                } catch (final RestClientException e) {
                    facturesEnErreur.incrementAndGet();
                    flowLogger.errorApiPennylane(FlowType.SYNC_BAP, aFacture, 0, e.getMessage());
                } catch (final ServiceException e) {
                    facturesEnErreur.incrementAndGet();
                    log.error("[SYNC-BAP] [{}] Erreur service: {}", aFacture, e.getMessage());
                } catch (final Exception e) {
                    facturesEnErreur.incrementAndGet();
                    log.error("[SYNC-BAP] [{}] Erreur: {}", aFacture, e.getMessage(), e);
                }
            });
        });

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("Factures mises à jour", facturesTraitees.get());
        if (facturesEnErreur.get() > 0)
            stats.put("Factures en erreur", facturesEnErreur.get());
        flowLogger.endFlow(correlationId, stats);
    }

    /**
     * Purge les anciens enregistrements de logs métier.
     */
    @Scheduled(cron = "${cron.PurgeLog}")
    public void purgeLogs() {
        String correlationId = flowLogger.startFlow(FlowType.PURGE_LOGS);
        try {
            logRepository.logPurger();
            log.info("[PURGE-LOGS] Purge terminée");
        } catch (Exception e) {
            log.error("[PURGE-LOGS] Erreur: {}", e.getMessage(), e);
        } finally {
            flowLogger.endFlow(correlationId);
        }
    }
}
