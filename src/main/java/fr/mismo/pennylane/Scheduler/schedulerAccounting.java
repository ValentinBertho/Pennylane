package fr.mismo.pennylane.Scheduler;

import fr.mismo.pennylane.api.AccountsApi;
import fr.mismo.pennylane.dao.entity.SiteEntity;
import fr.mismo.pennylane.dao.repository.EcritureRepository;
import fr.mismo.pennylane.dao.repository.LogRepository;
import fr.mismo.pennylane.dao.repository.SiteRepository;
import fr.mismo.pennylane.dto.accounting.Item;
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
import java.util.concurrent.atomic.AtomicReference;

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

    @Scheduled(cron = "${cron.Entries}")
    public void syncEntries() {
        long startGlobal = System.currentTimeMillis();
        log.info("🔄 [CRON ENTRIES] Démarrage de la synchronisation des écritures");
        log.debug("== Début de la synchronisation globale des écritures ==");

        AtomicReference<List<Item>> accountPennylane = new AtomicReference<>(new ArrayList<>());
        List<SiteEntity> sites = siteRepository.findAllByPennylaneActifTrue();

        log.info("📊 Nombre de sites à traiter : {}", sites.size());

        sites.forEach(site -> {
            long startSite = System.currentTimeMillis();
            log.debug("Traitement du site {} ...", site.getCode());

            List<Integer> ecrituresList = ecritureRepository.getLotEcritureToExport(site.getId()).stream().toList();
            if (CollectionUtils.isEmpty(ecrituresList)) {
                log.info("Aucune écriture à synchroniser pour {}", site.getCode());
                return;
            }

            accountPennylane.set(accountsApi.listAllLedgerAccounts(site));
            List<Item> finalAccountPennylane = accountPennylane.get();

            ecrituresList.forEach(ecriture -> {
                long startEcriture = System.currentTimeMillis();
                try {
                    accountingService.syncEcriture(ecriture, site, finalAccountPennylane);
                } catch (Exception e) {
                    handleException(ecriture, e);
                } finally {
                    long durationEcriture = System.currentTimeMillis() - startEcriture;
                    log.info("Écriture {} traitée en {} ms", ecriture, durationEcriture);
                }
            });

            long durationSite = System.currentTimeMillis() - startSite;
            log.debug("== Fin du traitement du site {} ({} ms) ==", site.getCode(), durationSite);
        });

        long durationGlobal = System.currentTimeMillis() - startGlobal;
        log.info("✅ [CRON ENTRIES] Fin de la synchronisation ({} ms)", durationGlobal);
    }


    @Scheduled(cron = "${cron.UpdateSale}")
    public void UpdateSale() {
        long startGlobal = System.currentTimeMillis();
        log.info("== Démarrage de la mise en BAP des FACTURES ACHATS (Athénéo -> Pennylane) ==");

        List<SiteEntity> sites = siteRepository.findAllByPennylaneAchatTrue();

        sites.forEach(site -> {
            long startSite = System.currentTimeMillis();
            log.debug("Traitement des factures pour le site {} ...", site.getCode());

            List<String> aFactureList = ecritureRepository.getAFactureBAP(site.getCode());

            if (CollectionUtils.isEmpty(aFactureList)) {
                log.debug("Aucune A_FACTURE à synchroniser pour {}", site.getCode());
                return;
            }

            aFactureList.forEach(aFacture -> {
                long startFacture = System.currentTimeMillis();
                log.info("== Démarrage de la mise en BAP de la facture {} (Athénéo -> Pennylane) ==", aFacture);
                try {
                    invoiceService.updateInvoice(aFacture, site);
                } catch (final RestClientException e) {
                    log.error("Erreur lors de la communication avec Pennylane", e);
                } catch (final ServiceException e) {
                    log.error("Erreur spécifique au service lors de la synchronisation", e);
                } catch (final Exception e) {
                    log.error("Erreur non gérée", e);
                } finally {
                    long durationFacture = System.currentTimeMillis() - startFacture;
                    log.info("== Fin de la mise en BAP de la facture {} ({} ms) ==", aFacture, durationFacture);
                }
            });

            long durationSite = System.currentTimeMillis() - startSite;
            log.debug("== Fin du traitement des factures du site {} ({} ms) ==", site.getCode(), durationSite);
        });

        long durationGlobal = System.currentTimeMillis() - startGlobal;
        log.info("== Fin de la mise en BAP globale des factures ({} ms) ==", durationGlobal);
    }

    @Scheduled(cron = "${cron.PurgeLog}")
    public void purgeLogs() {
        log.info("== Démarrage de la purge des logs ==");
        logRepository.logPurger();
        log.info("== Fin de la purge des logs ==");
    }

}
