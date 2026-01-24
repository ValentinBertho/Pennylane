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
 *
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

    // Méthode auxiliaire pour gérer les exceptions pendant la synchronisation
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
     * Synchronise les écritures comptables depuis ATHENEO vers Pennylane
     */
    @Scheduled(cron = "${cron.Entries}")
    public void syncEntries() {
        List<SiteEntity> sites = siteRepository.findAllByPennylaneActifTrue();
        if (CollectionUtils.isEmpty(sites)) {
            log.trace("[SYNC-ECRITURES] Aucun site actif pour la synchronisation");
            return;
        }

        // Compter le total des lots à traiter
        AtomicInteger totalLots = new AtomicInteger(0);
        sites.forEach(site -> {
            List<Integer> lots = ecritureRepository.getLotEcritureToExport(site.getId()).stream().toList();
            totalLots.addAndGet(lots.size());
        });

        if (totalLots.get() == 0) {
            log.trace("[SYNC-ECRITURES] Aucun lot à synchroniser");
            return;
        }

        // Démarrage du flux avec logging amélioré
        String correlationId = flowLogger.startSyncEcritures(totalLots.get(), sites.size());

        AtomicReference<List<Item>> accountPennylane = new AtomicReference<>(new ArrayList<>());

        // Compteurs pour le bilan
        AtomicInteger lotsTraites = new AtomicInteger(0);
        AtomicInteger facturesCrees = new AtomicInteger(0);
        AtomicInteger facturesIgnorees = new AtomicInteger(0);
        AtomicInteger clientsCrees = new AtomicInteger(0);
        AtomicInteger produitsCrees = new AtomicInteger(0);
        AtomicInteger documentsUploades = new AtomicInteger(0);
        List<String> erreurs = new ArrayList<>();

        sites.forEach(site -> {
            long startSite = System.currentTimeMillis();
            log.info("[SYNC-ECRITURES] Traitement du site {} ...", site.getCode());

            List<Integer> ecrituresList = ecritureRepository.getLotEcritureToExport(site.getId()).stream().toList();
            if (CollectionUtils.isEmpty(ecrituresList)) {
                log.debug("[SYNC-ECRITURES] Aucune écriture à synchroniser pour {}", site.getCode());
                return;
            }

            log.info("[SYNC-ECRITURES] {} lot(s) à traiter pour le site {}", ecrituresList.size(), site.getCode());

            accountPennylane.set(accountsApi.listAllLedgerAccounts(site));
            List<Item> finalAccountPennylane = accountPennylane.get();

            ecrituresList.forEach(ecriture -> {
                long startEcriture = System.currentTimeMillis();
                try {
                    AccountingService.SyncResult result = accountingService.syncEcriture(ecriture, site, finalAccountPennylane);

                    // Mise à jour des compteurs
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
                } finally {
                    long durationEcriture = System.currentTimeMillis() - startEcriture;
                    log.info("[SYNC-ECRITURES] [LOT-{}] Lot traité en {} ms", ecriture, durationEcriture);
                }
            });

            long durationSite = System.currentTimeMillis() - startSite;
            log.info("[SYNC-ECRITURES] Fin du traitement du site {} ({} ms)", site.getCode(), durationSite);
        });

        // Fin du flux avec bilan
        flowLogger.endSyncEcritures(correlationId, lotsTraites.get(), totalLots.get(),
            facturesCrees.get(), facturesIgnorees.get(),
            clientsCrees.get(), produitsCrees.get(),
            documentsUploades.get(), erreurs);
    }


    /**
     * Met à jour le statut "Bon À Payer" (BAP) des factures clients dans Pennylane
     */
    @Scheduled(cron = "${cron.UpdateSale}")
    public void UpdateSale() {
        List<SiteEntity> sites = siteRepository.findAllByPennylaneAchatTrue();
        if (CollectionUtils.isEmpty(sites)) {
            log.trace("[SYNC-BAP] Aucun site actif pour la mise à jour BAP");
            return;
        }

        // Démarrage du flux
        String correlationId = flowLogger.startFlow(FlowType.SYNC_BAP);

        // Compteurs pour le bilan
        AtomicInteger facturesTraitees = new AtomicInteger(0);
        AtomicInteger facturesEnErreur = new AtomicInteger(0);
        List<String> erreurs = new ArrayList<>();

        sites.forEach(site -> {
            long startSite = System.currentTimeMillis();
            log.info("[SYNC-BAP] Traitement des factures pour le site {} ...", site.getCode());

            List<String> aFactureList = ecritureRepository.getAFactureBAP(site.getCode());

            if (CollectionUtils.isEmpty(aFactureList)) {
                log.debug("[SYNC-BAP] Aucune facture à mettre en BAP pour {}", site.getCode());
                return;
            }

            log.info("[SYNC-BAP] {} facture(s) à traiter pour le site {}", aFactureList.size(), site.getCode());

            aFactureList.forEach(aFacture -> {
                long startFacture = System.currentTimeMillis();
                log.info("[SYNC-BAP] [{}] Mise à jour BAP en cours...", aFacture);
                try {
                    invoiceService.updateInvoice(aFacture, site);
                    facturesTraitees.incrementAndGet();
                    log.info("[SYNC-BAP] [{}] ✓ Facture passée en Bon À Payer", aFacture);
                } catch (final RestClientException e) {
                    facturesEnErreur.incrementAndGet();
                    erreurs.add(String.format("%s: Erreur API Pennylane - %s", aFacture, e.getMessage()));
                    flowLogger.errorApiPennylane(FlowType.SYNC_BAP, aFacture, 0, e.getMessage());
                } catch (final ServiceException e) {
                    facturesEnErreur.incrementAndGet();
                    erreurs.add(String.format("%s: Erreur service - %s", aFacture, e.getMessage()));
                    log.error("[SYNC-BAP] [{}] ✗ Erreur spécifique au service: {}", aFacture, e.getMessage());
                } catch (final Exception e) {
                    facturesEnErreur.incrementAndGet();
                    erreurs.add(String.format("%s: Erreur inattendue - %s", aFacture, e.getMessage()));
                    log.error("[SYNC-BAP] [{}] ✗ Erreur non gérée: {}", aFacture, e.getMessage(), e);
                } finally {
                    long durationFacture = System.currentTimeMillis() - startFacture;
                    log.debug("[SYNC-BAP] [{}] Traitement terminé en {} ms", aFacture, durationFacture);
                }
            });

            long durationSite = System.currentTimeMillis() - startSite;
            log.info("[SYNC-BAP] Fin du traitement du site {} ({} ms)", site.getCode(), durationSite);
        });

        // Fin du flux avec bilan
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("Factures mises à jour", facturesTraitees.get());
        stats.put("Factures en erreur", facturesEnErreur.get());
        flowLogger.endFlow(correlationId, stats);
    }

    /**
     * Purge les anciens enregistrements de logs métier de la table T_LOG
     */
    @Scheduled(cron = "${cron.PurgeLog}")
    public void purgeLogs() {
        String correlationId = flowLogger.startFlow(FlowType.PURGE_LOGS);
        try {
            logRepository.logPurger();
            log.info("[PURGE-LOGS] ✓ Purge des logs terminée avec succès");
        } catch (Exception e) {
            log.error("[PURGE-LOGS] ✗ Erreur lors de la purge des logs: {}", e.getMessage(), e);
        } finally {
            flowLogger.endFlow(correlationId);
        }
    }

}
