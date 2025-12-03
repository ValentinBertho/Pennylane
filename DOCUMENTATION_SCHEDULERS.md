# Documentation des Schedulers - Interface Pennylane

## Vue d'ensemble

L'application utilise des tâches planifiées (schedulers) pour synchroniser automatiquement les données entre ATHENEO (système comptable legacy) et Pennylane (plateforme cloud).

### Configuration
- **Pool de threads** : 5 threads configurables (`spring.task.scheduling.pool.size`)
- **Préfixe des threads** : `pennylane-scheduler-`
- **Configuration CRON** : Définie dans `application.yml` section `cron:`

---

## 📊 Schedulers Comptabilité (`schedulerAccounting.java`)

### 1. `syncEntries()` - Synchronisation des Écritures Comptables
**Direction** : ATHENEO → Pennylane
**État** : ✅ **ACTIF** - Toutes les 10 secondes
**CRON** : `*/10 * * * * *`
**Procédure stockée** : `SP_PENNYLANE_EXPORT_LOT`

#### Rôle
Exporte les écritures comptables (journal général) depuis ATHENEO vers Pennylane pour synchroniser les comptes généraux.

#### Fonctionnement détaillé
1. **Récupération des sites actifs**
   - Query : `SiteRepository.findAllByPennylaneActifTrue()`
   - Filtre : Sites avec flag `pennylaneActif = true` dans `T_SITE`

2. **Récupération des écritures à exporter**
   - Query : `EcritureRepository.getLotEcritureToExport(siteId)`
   - Source : Procédure stockée `SP_PENNYLANE_EXPORT_LOT`
   - Retourne : Liste d'IDs d'écritures (table `V_ECRITURE` ou similaire)

3. **Récupération du plan comptable Pennylane**
   - API : `AccountsApi.listAllLedgerAccounts(site)`
   - Endpoint : `GET /ledger_accounts`
   - But : Valider que les comptes existent dans Pennylane

4. **Synchronisation des écritures**
   - Pour chaque écriture : `AccountingService.syncEcriture()`
   - Validation des comptes contre le plan comptable Pennylane
   - Création de l'écriture dans Pennylane via API
   - Logging dans `T_LOG` (table de logs métier)

5. **Gestion des erreurs**
   - `RestClientException` : Erreur de communication API
   - `ServiceException` : Erreur métier (compte inexistant, validation échouée)
   - Logging détaillé avec durées d'exécution

#### Monitoring
- **Logs de démarrage** : `🔄 [CRON ENTRIES] Démarrage de la synchronisation des écritures`
- **Logs de fin** : `✅ [CRON ENTRIES] Fin de la synchronisation (X ms)`
- **Métriques** : Durée par écriture, durée par site, durée globale

#### Quand le désactiver
- Mettre `cron.Entries: "-"` dans `application.yml`
- Cas d'usage : Maintenance, migration de données, debugging

---

### 2. `UpdateSale()` - Mise à jour des statuts factures clients (BAP)
**Direction** : ATHENEO → Pennylane
**État** : ⚠️ **DÉSACTIVÉ** - `cron: "-"`
**Procédure stockée** : `SP_PENNYLANE_CUSTOMER_INVOICE_BAP`

#### Rôle
Met à jour le statut "Bon À Payer" (BAP) des factures clients dans Pennylane lorsque leur statut change dans ATHENEO.

#### Fonctionnement détaillé
1. **Récupération des sites avec achat actif**
   - Query : `SiteRepository.findAllByPennylaneAchatTrue()`
   - Filtre : Sites avec flag `pennylaneAchat = true`

2. **Récupération des factures à mettre à jour**
   - Query : `EcritureRepository.getAFactureBAP(siteCode)`
   - Source : Procédure stockée `SP_PENNYLANE_CUSTOMER_INVOICE_BAP`
   - Retourne : Liste de numéros de factures (A_FACTURE)

3. **Mise à jour des factures**
   - Service : `InvoiceService.updateInvoice(aFacture, site)`
   - Met à jour le statut de paiement dans Pennylane
   - Marque la facture comme payée ou partiellement payée

#### Pourquoi désactivé ?
- Nécessite synchronisation bidirectionnelle complexe
- Peut être remplacé par synchronisation manuelle
- Risque de conflits de statuts entre les deux systèmes

#### Comment l'activer
```yaml
cron:
  UpdateSale: "0 */15 * * * *"  # Toutes les 15 minutes
```

---

### 3. `purgeLogs()` - Purge des anciens logs
**Direction** : N/A (Maintenance interne)
**État** : ⚠️ **DÉSACTIVÉ** - `cron: "-"`
**Procédure stockée** : `SP_PENNYLANE_LOG_PURGER` (ou similaire)

#### Rôle
Nettoie les anciens enregistrements de la table de logs métier `T_LOG` pour éviter une croissance excessive de la base de données.

#### Fonctionnement
- Query : `LogRepository.logPurger()`
- Supprime les logs plus anciens qu'un seuil défini (ex: 90 jours)
- Libère de l'espace disque

#### Comment l'activer
```yaml
cron:
  PurgeLog: "0 0 2 * * *"  # Tous les jours à 2h du matin
```

---

## 🛒 Schedulers Achats (`schedulerPurchases.java`)

### 1. `SyncPurchases()` - Synchronisation des Factures Fournisseurs
**Direction** : Pennylane → ATHENEO
**État** : ⚠️ **DÉSACTIVÉ** - `cron: "-"`
**Configuration** : `facture.statusAFiltrer`, `facture.categoriesAFiltrer`, `facture.daysBackward`

#### Rôle
Importe les factures fournisseurs depuis Pennylane vers ATHENEO pour comptabilisation et paiement.

#### Fonctionnement détaillé
1. **Calcul de la date de synchronisation**
   - Formule : `LocalDate.now() - daysBackward` (défaut: 360 jours)
   - Paramètre : `facture.daysBackward` dans `application.yml`

2. **Récupération des sites**
   - Query : `SiteRepository.findAllByPennylaneAchatTrue()`

3. **Récupération des catégories avec cache**
   - Service : `CategoryCacheService.getCategories(site)`
   - **Optimisation** : Cache pour éviter appels API répétés
   - Filtre : Catégories configurées dans `facture.categoriesAFiltrer` (ex: `ACH`)

4. **Récupération des factures depuis Pennylane**
   - API : `InvoiceApi.listAllSupplierInvoices(site, categoryIds, syncDateTime)`
   - Endpoint : `GET /supplier_invoices`
   - Paramètres :
     - `category_id`: Liste des IDs de catégories
     - `updated_after`: Date de synchronisation

5. **Filtrage des factures**
   - Filtre : `facture.statusAFiltrer` (ex: `to_be_processed`)
   - Statuts possibles :
     - `to_be_processed` : À traiter
     - `partially_paid` : Partiellement payée
     - `paid` : Payée
     - `late` : En retard

6. **Synchronisation vers ATHENEO**
   - Service : `InvoiceService.syncInvoice(invoice, site, categoryIds)`
   - Crée ou met à jour la facture dans `V_FACTURE`
   - Crée le fournisseur si nécessaire
   - Attache les documents PDF via SOAP `WSDocumentAth`

7. **Mise à jour de la date de dernière synchronisation**
   - Config : `config.setLastInsertPurchases(LocalDateTime.now())`

#### Paramètres YAML importants
```yaml
facture:
  statusAFiltrer: 'to_be_processed'  # Statuts à importer
  daysBackward: 360                   # Remonter jusqu'à 360 jours
  categoriesAFiltrer:
    - ACH                             # Catégories à synchroniser
  lastInsertPurchases: 2024-01-01T00:00:00  # Dernière synchro
```

#### Comment l'activer
```yaml
cron:
  Purchases: "0 */30 * * * *"  # Toutes les 30 minutes
```

#### Monitoring et métriques
- Durée de récupération des catégories
- Durée de récupération des factures API
- Durée de filtrage
- Nombre de factures brutes vs filtrées
- Durée de traitement par facture

---

### 2. `SyncPurchasesV2()` - Synchronisation Factures (Version Changelog)
**Direction** : Pennylane → ATHENEO
**État** : ⚠️ **DÉSACTIVÉ** - `cron: "-"`
**API utilisée** : `/supplier_invoices/changelogs`

#### Différence avec V1
- **V1** : Récupère TOUTES les factures puis filtre (lourd)
- **V2** : Utilise l'API changelog pour récupérer UNIQUEMENT les factures modifiées (léger)

#### Fonctionnement
1. **Récupération des changelogs**
   - API : `InvoiceApi.listAllSupplierInvoiceChangelogs(site, syncDateTime)`
   - Endpoint : `GET /supplier_invoices/changelogs`
   - Retourne : Liste des IDs de factures modifiées

2. **Récupération facture par facture**
   - API : `InvoiceApi.getSupplierInvoiceById(site, invoiceId)`
   - Endpoint : `GET /supplier_invoices/{id}`
   - Plus lent mais plus précis

3. **Validation de la catégorie**
   - API : `AccountsApi.getCategoryByUrl(invoice.categories.url, site)`
   - Vérifie que la catégorie est autorisée

4. **Synchronisation**
   - Identique à V1 : `InvoiceService.syncInvoice()`

#### Quand utiliser V2 plutôt que V1 ?
- ✅ Volume élevé de factures (> 1000)
- ✅ Synchronisations fréquentes (toutes les 5-10 minutes)
- ❌ Première synchronisation complète (préférer V1)

#### Comment l'activer
```yaml
cron:
  PurchasesV2: "0 */10 * * * *"  # Toutes les 10 minutes
```

---

### 3. `UpdatePurchaseReglement()` - Mise à jour des Règlements Fournisseurs
**Direction** : Pennylane → ATHENEO
**État** : ⚠️ **DÉSACTIVÉ** - `cron: "-"`
**Procédure stockée** : `SP_PENNYLANE_SUPPLIER_INVOICE_REGLEMENT`

#### Rôle
Met à jour les statuts de paiement (règlements) des factures fournisseurs depuis Pennylane vers ATHENEO.

#### Fonctionnement
1. **Récupération des factures à mettre à jour**
   - Query : `EcritureRepository.getMajReglement(siteCode)`
   - Source : Procédure stockée `SP_PENNYLANE_SUPPLIER_INVOICE_REGLEMENT`
   - Retourne : Liste de V_FACTURE (IDs ou numéros)

2. **Mise à jour des règlements**
   - Service : `InvoiceService.updateReglements(aFacture, site)`
   - Récupère le statut de paiement depuis Pennylane
   - Met à jour les champs de règlement dans ATHENEO

#### Comment l'activer
```yaml
cron:
  UpdatePurchaseReglement: "0 */20 * * * *"  # Toutes les 20 minutes
```

---

### 4. `UpdatePurchaseReglementV2()` - Mise à jour Règlements (V2)
**Direction** : Pennylane → ATHENEO
**État** : ⚠️ **DÉSACTIVÉ** - `cron: "-"`

#### Différence avec V1
- Implémentation alternative avec gestion d'erreurs améliorée
- Logs plus détaillés
- Même source de données : `EcritureRepository.getMajReglement()`

#### Comment l'activer
```yaml
cron:
  UpdatePurchaseReglementV2: "0 */20 * * * *"  # Toutes les 20 minutes
```

---

## 🔧 Configuration recommandée

### Environnement de production
```yaml
cron:
  Entries: "*/30 * * * * *"              # Écritures toutes les 30 sec
  Purchases: "-"                          # Désactivé (utiliser V2)
  PurchasesV2: "0 */15 * * * *"          # Factures toutes les 15 min
  UpdateSale: "-"                         # Désactivé (manuel si besoin)
  UpdatePurchaseReglement: "0 */30 * * * *"  # Règlements toutes les 30 min
  UpdatePurchaseReglementV2: "-"          # Désactivé (doublon)
  PurgeLog: "0 0 3 * * *"                # Purge logs tous les jours à 3h
```

### Environnement de développement
```yaml
cron:
  Entries: "-"        # Désactivé (tests manuels)
  Purchases: "-"      # Désactivé
  PurchasesV2: "-"    # Désactivé
  UpdateSale: "-"     # Désactivé
  UpdatePurchaseReglement: "-"    # Désactivé
  UpdatePurchaseReglementV2: "-"  # Désactivé
  PurgeLog: "-"       # Désactivé
```

---

## 📈 Monitoring et Métriques

### Endpoints Actuator disponibles
- `http://localhost:8088/actuator/health` - État global
- `http://localhost:8088/actuator/metrics` - Toutes les métriques
- `http://localhost:8088/actuator/scheduledtasks` - État des schedulers

### Logs à surveiller
```
🔄 [CRON ENTRIES] Démarrage de la synchronisation
✅ [CRON ENTRIES] Fin de la synchronisation (X ms)
⚠️ Les catégories configurées et les catégories trouvées ne correspondent pas
```

### Requêtes SQL utiles pour le support
```sql
-- Dernière exécution des logs métier
SELECT TOP 10 * FROM T_LOG ORDER BY DATE_LOG DESC;

-- Sites actifs pour la synchronisation
SELECT CODE, PENNYLANE_ACTIF, PENNYLANE_ACHAT
FROM T_SITE
WHERE PENNYLANE_ACTIF = 1 OR PENNYLANE_ACHAT = 1;

-- Écritures en attente d'export
EXEC SP_PENNYLANE_EXPORT_LOT @SITE_ID = 1;

-- Factures en attente de mise à jour BAP
EXEC SP_PENNYLANE_CUSTOMER_INVOICE_BAP @SITE_CODE = 'SITE01';

-- Règlements à mettre à jour
EXEC SP_PENNYLANE_SUPPLIER_INVOICE_REGLEMENT @SITE_CODE = 'SITE01';
```

---

## 🚨 Dépannage

### Scheduler ne s'exécute pas
1. Vérifier la configuration CRON dans `application.yml`
2. Vérifier que `cron.XXX` n'est pas à `"-"`
3. Vérifier les logs au démarrage : `Scheduled tasks: ...`

### Trop de requêtes API (HTTP 429)
1. Vérifier Rate Limiter Resilience4j
2. Augmenter `resilience4j.ratelimiter.pennylaneAPI.limitRefreshPeriod`
3. Espacer les CRON (ex: toutes les 30 min au lieu de 10)

### Performances dégradées
1. Vérifier durées dans les logs : `(X ms)`
2. Optimiser nombre de sites actifs
3. Réduire `facture.daysBackward`
4. Utiliser V2 (changelog) au lieu de V1

### Circuit Breaker ouvert
1. Vérifier connectivité API Pennylane
2. Consulter `/actuator/circuitbreakers`
3. Attendre fermeture automatique (30s par défaut)
4. Vérifier les logs d'erreur API

---

## 📚 Procédures stockées référencées

| Procédure stockée | Scheduler | Description |
|-------------------|-----------|-------------|
| `SP_PENNYLANE_EXPORT_LOT` | `syncEntries()` | Récupère les écritures comptables à exporter |
| `SP_PENNYLANE_CUSTOMER_INVOICE_BAP` | `UpdateSale()` | Récupère les factures clients à mettre en BAP |
| `SP_PENNYLANE_SUPPLIER_INVOICE_REGLEMENT` | `UpdatePurchaseReglement()` | Récupère les factures fournisseurs à mettre à jour |
| `SP_PENNYLANE_LOG_PURGER` | `purgeLogs()` | Purge les anciens logs métier |
| `SP_PENNYLANE_GET_FACTURE` | (API interne) | Récupère les détails d'une facture |

---

## 🔄 Flux de données

```
┌─────────────────────────────────────────────────────────────┐
│                    SYNCHRONISATION GLOBALE                   │
└─────────────────────────────────────────────────────────────┘

ATHENEO → Pennylane:
  - Écritures comptables (syncEntries)
  - Statuts factures clients (UpdateSale)

Pennylane → ATHENEO:
  - Factures fournisseurs (SyncPurchases/V2)
  - Règlements (UpdatePurchaseReglement/V2)

Interne:
  - Purge logs (purgeLogs)
```

---

**Date de création** : 2025-12-03
**Version application** : 1.10.2
**Auteur** : Interface Pennylane
