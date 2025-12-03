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

/**
 * Scheduler pour la synchronisation des données comptables entre ATHENEO et Pennylane
 *
 * <h2>Responsabilités</h2>
 * <ul>
 *   <li>Synchroniser les écritures comptables ATHENEO → Pennylane</li>
 *   <li>Mettre à jour les statuts BAP des factures clients</li>
 *   <li>Purger les anciens logs métier</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>Les CRON d'exécution sont définis dans application.yml :</p>
 * <pre>
 * cron:
 *   Entries: "* /10 * * * * *"  # Écritures toutes les 10 secondes
 *   UpdateSale: "-"              # Désactivé par défaut
 *   PurgeLog: "-"                # Désactivé par défaut
 * </pre>
 *
 * <h2>Procédures stockées utilisées</h2>
 * <ul>
 *   <li>{@code SP_PENNYLANE_EXPORT_LOT} - Récupère les écritures à exporter</li>
 *   <li>{@code SP_PENNYLANE_CUSTOMER_INVOICE_BAP} - Récupère les factures clients à mettre en BAP</li>
 *   <li>{@code SP_PENNYLANE_LOG_PURGER} - Purge les anciens logs</li>
 * </ul>
 *
 * @see AccountingService
 * @see InvoiceService
 * @see DOCUMENTATION_SCHEDULERS.md
 * @author Interface Pennylane
 * @since 1.10.2
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
     *
     * <h3>Flux de traitement</h3>
     * <ol>
     *   <li>Récupère tous les sites actifs (pennylaneActif = true)</li>
     *   <li>Pour chaque site :
     *     <ul>
     *       <li>Récupère les écritures à exporter via SP_PENNYLANE_EXPORT_LOT</li>
     *       <li>Récupère le plan comptable Pennylane (ledger accounts)</li>
     *       <li>Valide et synchronise chaque écriture</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * <h3>Configuration YAML</h3>
     * <pre>
     * cron:
     *   Entries: "* /10 * * * * *"  # Actif toutes les 10 secondes
     * </pre>
     *
     * <h3>Procédure stockée</h3>
     * <p>{@code EXEC SP_PENNYLANE_EXPORT_LOT @SITE_ID = ?}</p>
     *
     * <h3>Monitoring</h3>
     * <p>Logs à surveiller :</p>
     * <ul>
     *   <li>🔄 [CRON ENTRIES] Démarrage de la synchronisation</li>
     *   <li>✅ [CRON ENTRIES] Fin de la synchronisation (X ms)</li>
     * </ul>
     *
     * @see EcritureRepository#getLotEcritureToExport(Long)
     * @see AccountingService#syncEcriture(Integer, SiteEntity, List)
     */
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


    /**
     * Met à jour le statut "Bon À Payer" (BAP) des factures clients dans Pennylane
     *
     * <h3>Direction</h3>
     * <p>ATHENEO → Pennylane</p>
     *
     * <h3>État</h3>
     * <p>⚠️ DÉSACTIVÉ par défaut (cron: "-")</p>
     *
     * <h3>Flux de traitement</h3>
     * <ol>
     *   <li>Récupère les sites avec pennylaneAchat = true</li>
     *   <li>Récupère les factures à mettre en BAP via SP_PENNYLANE_CUSTOMER_INVOICE_BAP</li>
     *   <li>Met à jour chaque facture dans Pennylane</li>
     * </ol>
     *
     * <h3>Activation</h3>
     * <pre>
     * cron:
     *   UpdateSale: "0 * /15 * * * *"  # Toutes les 15 minutes
     * </pre>
     *
     * <h3>Procédure stockée</h3>
     * <p>{@code EXEC SP_PENNYLANE_CUSTOMER_INVOICE_BAP @SITE_CODE = ?}</p>
     *
     * @see EcritureRepository#getAFactureBAP(String)
     * @see InvoiceService#updateInvoice(String, SiteEntity)
     */
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

    /**
     * Purge les anciens enregistrements de logs métier de la table T_LOG
     *
     * <h3>État</h3>
     * <p>⚠️ DÉSACTIVÉ par défaut (cron: "-")</p>
     *
     * <h3>Rôle</h3>
     * <p>Nettoie les logs plus anciens qu'un seuil défini pour éviter la croissance
     * excessive de la base de données.</p>
     *
     * <h3>Activation recommandée</h3>
     * <pre>
     * cron:
     *   PurgeLog: "0 0 3 * * *"  # Tous les jours à 3h du matin
     * </pre>
     *
     * <h3>Procédure stockée</h3>
     * <p>{@code EXEC SP_PENNYLANE_LOG_PURGER}</p>
     *
     * @see LogRepository#logPurger()
     */
    @Scheduled(cron = "${cron.PurgeLog}")
    public void purgeLogs() {
        log.info("== Démarrage de la purge des logs ==");
        logRepository.logPurger();
        log.info("== Fin de la purge des logs ==");
    }

}
