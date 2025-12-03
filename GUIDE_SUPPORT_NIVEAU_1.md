# Guide d'Assistance Niveau 1 - Interface Pennylane

## 📋 Table des matières
1. [Vue d'ensemble](#vue-densemble)
2. [Prérequis et accès](#prérequis-et-accès)
3. [Procédures stockées SQL](#procédures-stockées-sql)
4. [Paramètres YAML](#paramètres-yaml)
5. [Tables de base de données](#tables-de-base-de-données)
6. [Vérifications de base](#vérifications-de-base)
7. [Logs et monitoring](#logs-et-monitoring)
8. [Scénarios de dépannage courants](#scénarios-de-dépannage-courants)
9. [Endpoints API utiles](#endpoints-api-utiles)
10. [Escalade niveau 2](#escalade-niveau-2)

---

## 🎯 Vue d'ensemble

### Qu'est-ce que l'interface Pennylane ?

L'interface Pennylane est une application Spring Boot qui synchronise automatiquement les données entre :
- **ATHENEO** : Système comptable legacy (SQL Server)
- **Pennylane** : Plateforme comptable cloud (API REST)

### Flux de données principaux

```
┌──────────────────────────────────────────────────────┐
│                 FLUX BIDIRECTIONNELS                  │
└──────────────────────────────────────────────────────┘

ATHENEO → Pennylane:
  ✅ Écritures comptables (toutes les 10 secondes)
  ⚠️  Statuts factures clients BAP (désactivé)

Pennylane → ATHENEO:
  ⚠️  Factures fournisseurs (désactivé)
  ⚠️  Règlements/paiements (désactivé)
```

### Informations techniques
- **Version** : 1.10.2
- **Java** : 21
- **Spring Boot** : 3.3.0
- **Serveur** : Port 8088
- **Base de données** : SQL Server ATHENEO_MISMO
- **Serveur DB** : NA-ATH01.mismo.local\ATHENEO

---

## 🔐 Prérequis et accès

### Accès nécessaires pour le support

1. **Accès SSH/Bureau distant au serveur d'application**
   - Pour consulter les logs : `/var/log/pennylane/` ou répertoire configuré
   - Pour redémarrer l'application

2. **Accès SQL Server Management Studio (SSMS)**
   - Serveur : `NA-ATH01.mismo.local\ATHENEO`
   - Base : `ATHENEO_MISMO`
   - User : `atheneo_sql` (credentials dans application.yml)

3. **Accès web à l'interface**
   - Dashboard : `http://[serveur]:8088/`
   - Actuator : `http://[serveur]:8088/actuator/`

4. **Accès Pennylane (optionnel)**
   - Console admin : `https://app.pennylane.com/`
   - Tokens API stockés dans table `T_SITE`

---

## 🗄️ Procédures stockées SQL

### Pourquoi consulter les procédures stockées ?
Les procédures stockées retournent les données que l'interface va synchroniser. Si la synchronisation échoue, vérifier d'abord ce que les procédures retournent.

### Liste des procédures principales

#### 1. `SP_PENNYLANE_EXPORT_LOT`
**Rôle** : Récupère les écritures comptables à exporter vers Pennylane

**Utilisation** :
```sql
-- Voir les écritures en attente pour un site
EXEC SP_PENNYLANE_EXPORT_LOT @SITE_ID = 1;

-- Paramètres possibles (à vérifier dans la définition)
-- @SITE_ID = ID du site dans T_SITE
```

**Que vérifier** :
- ✅ La procédure retourne des lignes → Écritures en attente
- ❌ Aucune ligne → Pas d'écritures à synchroniser (normal)
- ⚠️ Erreur SQL → Problème dans la procédure (escalade N2)

**Colonnes importantes retournées** :
- `ID_ECRITURE` : Identifiant de l'écriture
- `COMPTE` : Numéro de compte général
- `MONTANT` : Montant HT ou TTC
- `LIBELLE` : Description de l'écriture
- `DATE_ECRITURE` : Date de l'opération

---

#### 2. `SP_PENNYLANE_CUSTOMER_INVOICE_BAP`
**Rôle** : Récupère les factures clients dont le statut BAP (Bon À Payer) a changé

**Utilisation** :
```sql
-- Voir les factures clients à mettre à jour
EXEC SP_PENNYLANE_CUSTOMER_INVOICE_BAP @SITE_CODE = 'SITE01';

-- Paramètres
-- @SITE_CODE = Code du site (ex: 'SITE01', 'SITE02')
```

**Que vérifier** :
- ✅ Des factures retournées → Mises à jour en attente
- ❌ Aucune ligne → Pas de MAJ (normal si scheduler désactivé)

**Colonnes importantes** :
- `A_FACTURE` : Numéro de facture
- `STATUT_BAP` : Nouveau statut à appliquer
- `DATE_MODIFICATION` : Date du changement

---

#### 3. `SP_PENNYLANE_SUPPLIER_INVOICE_REGLEMENT`
**Rôle** : Récupère les factures fournisseurs dont les règlements doivent être mis à jour

**Utilisation** :
```sql
-- Voir les règlements à synchroniser
EXEC SP_PENNYLANE_SUPPLIER_INVOICE_REGLEMENT @SITE_CODE = 'SITE01';
```

**Que vérifier** :
- Les numéros de factures retournés existent-ils dans Pennylane ?
- Les montants de règlement sont-ils cohérents ?

---

#### 4. `SP_PENNYLANE_LOG_PURGER`
**Rôle** : Purge les anciens logs de la table `T_LOG`

**Utilisation** :
```sql
-- Purger les logs (⚠️ action destructive)
EXEC SP_PENNYLANE_LOG_PURGER;
```

**Que vérifier** :
- Nombre de lignes supprimées
- Ancienneté des logs conservés (généralement 90 jours)

---

#### 5. `SP_PENNYLANE_GET_FACTURE`
**Rôle** : Récupère les détails complets d'une facture spécifique

**Utilisation** :
```sql
-- Détails d'une facture
EXEC SP_PENNYLANE_GET_FACTURE @FACTURE_ID = 12345;
```

---

### 📝 Comment diagnostiquer un problème via les procédures

**Scénario 1 : "Les écritures ne se synchronisent plus"**
```sql
-- 1. Vérifier si des écritures sont en attente
EXEC SP_PENNYLANE_EXPORT_LOT @SITE_ID = 1;

-- 2. Si aucune ligne : normal, pas d'écritures
-- 3. Si des lignes : vérifier les logs applicatifs
-- 4. Vérifier si les comptes existent dans Pennylane (voir section logs)
```

**Scénario 2 : "Une facture spécifique pose problème"**
```sql
-- 1. Récupérer les détails de la facture
EXEC SP_PENNYLANE_GET_FACTURE @FACTURE_ID = 12345;

-- 2. Vérifier les montants, dates, fournisseur
-- 3. Comparer avec les logs de l'interface (voir section logs)
```

---

## ⚙️ Paramètres YAML

### Fichier de configuration : `application.yml`

Le fichier `application.yml` contient TOUS les paramètres configurables de l'application. Il se trouve dans `/home/user/Pennylane/src/main/resources/` (développement) ou dans le répertoire d'installation (production).

### Sections importantes à connaître

#### 1. Configuration Base de données
```yaml
spring:
  datasource:
    url: jdbc:sqlserver://NA-ATH01.mismo.local\ATHENEO;databaseName=ATHENEO_MISMO;encrypt=false
    username: atheneo_sql
    password: SQL19_4TH)sP3g{7
```

**Pourquoi regarder cette section ?**
- ❌ Connexion DB impossible → Vérifier URL, username, password
- ❌ `Timeout` → Serveur DB inaccessible ou surchargé
- ✅ Application démarre mais erreurs SQL → Regarder les permissions

**Comment tester la connexion ?**
```sql
-- Depuis SSMS, avec les credentials du YAML
-- Si connexion OK, le problème est ailleurs
```

---

#### 2. Configuration des CRON (Schedulers)
```yaml
cron:
  Entries: "*/10 * * * * *"          # ✅ ACTIF - Toutes les 10 secondes
  Purchases: "-"                      # ⚠️  DÉSACTIVÉ
  PurchasesV2: "-"                    # ⚠️  DÉSACTIVÉ
  UpdateSale: "-"                     # ⚠️  DÉSACTIVÉ
  UpdatePurchaseReglement: "-"        # ⚠️  DÉSACTIVÉ
  UpdatePurchaseReglementV2: "-"      # ⚠️  DÉSACTIVÉ
  PurgeLog: "-"                       # ⚠️  DÉSACTIVÉ
```

**Pourquoi regarder cette section ?**
- ❓ "Pourquoi les factures fournisseurs ne s'importent pas ?" → Vérifier que le scheduler n'est pas à `"-"`
- ⚠️ "Trop de charge sur le serveur" → Espacer les CRON (ex: toutes les 30s au lieu de 10s)

**Format CRON** :
```
"seconde minute heure jour mois jour_semaine"
"*/10 * * * * *"  → Toutes les 10 secondes
"0 */15 * * * *"  → Toutes les 15 minutes
"0 0 3 * * *"     → Tous les jours à 3h du matin
"-"               → DÉSACTIVÉ
```

**Comment activer un scheduler ?**
1. Modifier le CRON dans `application.yml`
2. Redémarrer l'application
3. Vérifier les logs : `🔄 [CRON XXX] Démarrage...`

---

#### 3. Configuration API Pennylane
```yaml
api:
  url_v1: https://app.pennylane.com/api/external/v2/
  url_v2: https://app.pennylane.com/api/external/v2/
```

**Pourquoi regarder cette section ?**
- ❌ Erreurs 404 sur les appels API → URL incorrecte
- ⚠️ Rate limiting (HTTP 429) → Trop d'appels (voir Resilience4j)

---

#### 4. Configuration Factures Fournisseurs
```yaml
facture:
  statusAFiltrer: 'to_be_processed'   # Statuts à importer
  daysBackward: 360                    # Remonter jusqu'à 360 jours
  categoriesAFiltrer:
    - ACH                              # Catégories à synchroniser
  lastInsertPurchases: 2024-01-01T00:00:00  # Dernière synchro
```

**Pourquoi regarder cette section ?**
- ❓ "Certaines factures ne s'importent pas" → Vérifier `categoriesAFiltrer` et `statusAFiltrer`
- ⚠️ "Trop de factures importées" → Réduire `daysBackward` (ex: 30 jours au lieu de 360)
- ℹ️ "Quelle est la dernière synchro ?" → Regarder `lastInsertPurchases`

**Statuts possibles** :
- `to_be_processed` : À traiter
- `partially_paid` : Partiellement payée
- `paid` : Payée
- `late` : En retard
- `` (vide) : Tous les statuts

---

#### 5. Configuration Resilience4j (Nouveau)
```yaml
resilience4j:
  circuitbreaker:
    instances:
      pennylaneAPI:
        failureRateThreshold: 50        # 50% d'échecs = ouverture
        waitDurationInOpenState: 30s    # Attente avant retry

  ratelimiter:
    instances:
      pennylaneAPI:
        limitForPeriod: 100             # Max 100 appels
        limitRefreshPeriod: 60s         # Par minute
```

**Pourquoi regarder cette section ?**
- ❌ "Circuit breaker ouvert" → Trop d'erreurs API, attendre 30s
- ❌ "Rate limit dépassé" → Trop d'appels, espacer les schedulers
- ⚙️ Ajuster les seuils selon la charge

---

#### 6. Configuration Logs
```yaml
logging:
  level:
    root: INFO
    fr.mismo: TRACE          # Logs très détaillés de l'interface
    org.springframework: INFO
```

**Pourquoi regarder cette section ?**
- 🐛 Debugging → Mettre `fr.mismo: DEBUG` ou `TRACE`
- 🚀 Production → Mettre `fr.mismo: INFO` (moins verbeux)

**Niveaux de logs** (du plus verbeux au moins) :
- `TRACE` : Tous les détails (debug profond)
- `DEBUG` : Informations de debug
- `INFO` : Informations importantes
- `WARN` : Avertissements
- `ERROR` : Erreurs uniquement

---

#### 7. Configuration Serveur
```yaml
server:
  port: 8088                          # Port HTTP
  servlet:
    context-path: /                   # Racine de l'application
```

**Pourquoi regarder cette section ?**
- ❌ "Impossible d'accéder à l'application" → Vérifier le port et le pare-feu
- 🔗 URL de l'interface : `http://[serveur]:8088/`

---

### 🔍 Comment modifier un paramètre YAML ?

1. **Éditer le fichier** :
   ```bash
   nano /chemin/vers/application.yml
   # ou
   vi /chemin/vers/application.yml
   ```

2. **Modifier la valeur** :
   ```yaml
   cron:
     Entries: "*/30 * * * * *"  # Changé de 10s à 30s
   ```

3. **Redémarrer l'application** :
   ```bash
   systemctl restart pennylane
   # ou
   ./restart.sh
   ```

4. **Vérifier les logs de démarrage** :
   ```bash
   tail -f /var/log/pennylane/application.log
   ```

---

## 📊 Tables de base de données

### Tables principales à connaître

#### 1. `T_SITE` - Configuration multi-tenant
**Rôle** : Stocke les sites (clients/entités) et leurs tokens API Pennylane

```sql
-- Lister tous les sites actifs
SELECT
    ID,
    CODE,
    NOM,
    PENNYLANE_ACTIF,           -- 1 = Sync écritures activée
    PENNYLANE_ACHAT,           -- 1 = Sync factures activée
    PENNYLANE_TOKEN            -- Token API (sensible !)
FROM T_SITE
WHERE PENNYLANE_ACTIF = 1 OR PENNYLANE_ACHAT = 1;
```

**Que vérifier** :
- ✅ `PENNYLANE_ACTIF = 1` → Le site est bien actif pour la synchro
- ✅ `PENNYLANE_TOKEN IS NOT NULL` → Token API configuré
- ❌ Token vide ou expiré → Synchronisation impossible

**Dépannage** :
```sql
-- Désactiver temporairement un site problématique
UPDATE T_SITE SET PENNYLANE_ACTIF = 0 WHERE CODE = 'SITE01';

-- Réactiver après correction
UPDATE T_SITE SET PENNYLANE_ACTIF = 1 WHERE CODE = 'SITE01';
```

---

#### 2. `T_LOG` - Logs métier de synchronisation
**Rôle** : Enregistre toutes les opérations de synchronisation avec statuts

```sql
-- Derniers logs (succès et erreurs)
SELECT TOP 50
    DATE_LOG,
    NIVEAU,                    -- ERROR, WARN, INFO, DEBUG, TRACE
    MESSAGE,
    INITIATEUR,                -- 'INTERFACE_PENNYLANE'
    OBJET_CONCERNE,            -- Type d'objet (facture, écriture...)
    ID_OBJET                   -- ID de l'objet traité
FROM T_LOG
ORDER BY DATE_LOG DESC;
```

**Filtrer les erreurs** :
```sql
-- Erreurs des dernières 24h
SELECT *
FROM T_LOG
WHERE NIVEAU = 'ERROR'
  AND DATE_LOG > DATEADD(HOUR, -24, GETDATE())
ORDER BY DATE_LOG DESC;
```

**Chercher les logs d'un objet spécifique** :
```sql
-- Logs d'une facture précise
SELECT *
FROM T_LOG
WHERE OBJET_CONCERNE = 'FACTURE'
  AND ID_OBJET = '12345'
ORDER BY DATE_LOG DESC;
```

**Statistiques des logs** :
```sql
-- Comptage par niveau des dernières 24h
SELECT
    NIVEAU,
    COUNT(*) AS NOMBRE
FROM T_LOG
WHERE DATE_LOG > DATEADD(HOUR, -24, GETDATE())
GROUP BY NIVEAU
ORDER BY NOMBRE DESC;
```

---

#### 3. `V_ECRITURE` - Écritures comptables
**Rôle** : Vue sur les écritures comptables à synchroniser

```sql
-- Écritures en attente d'export
SELECT TOP 100 *
FROM V_ECRITURE
WHERE EXPORT_PENNYLANE = 0     -- Pas encore exportée
  AND SITE_ID = 1
ORDER BY DATE_ECRITURE DESC;
```

**Colonnes importantes** :
- `EXPORT_PENNYLANE` : 0 = En attente, 1 = Exportée
- `DATE_ECRITURE` : Date de l'opération
- `COMPTE` : Numéro de compte général
- `MONTANT` : Montant de l'écriture

---

#### 4. `V_FACTURE` - Factures fournisseurs
**Rôle** : Vue sur les factures fournisseurs importées de Pennylane

```sql
-- Factures fournisseurs récentes
SELECT TOP 50
    NUMERO_FACTURE,
    FOURNISSEUR,
    MONTANT_TTC,
    DATE_FACTURE,
    STATUT_PAIEMENT,
    DATE_IMPORT_PENNYLANE
FROM V_FACTURE
ORDER BY DATE_IMPORT_PENNYLANE DESC;
```

**Rechercher une facture** :
```sql
-- Par numéro
SELECT * FROM V_FACTURE WHERE NUMERO_FACTURE = 'FAC-2024-001';

-- Par fournisseur
SELECT * FROM V_FACTURE WHERE FOURNISSEUR LIKE '%ACME%';
```

---

### 🔎 Requêtes de diagnostic rapide

#### Vérifier la santé globale
```sql
-- Vue d'ensemble des sites actifs
SELECT
    S.CODE AS SITE,
    S.PENNYLANE_ACTIF AS SYNC_ECRITURES,
    S.PENNYLANE_ACHAT AS SYNC_FACTURES,
    COUNT(DISTINCT E.ID) AS NB_ECRITURES_ATTENTE,
    (SELECT COUNT(*) FROM T_LOG L WHERE L.NIVEAU = 'ERROR'
     AND L.DATE_LOG > DATEADD(HOUR, -24, GETDATE())) AS ERREURS_24H
FROM T_SITE S
LEFT JOIN V_ECRITURE E ON E.SITE_ID = S.ID AND E.EXPORT_PENNYLANE = 0
WHERE S.PENNYLANE_ACTIF = 1 OR S.PENNYLANE_ACHAT = 1
GROUP BY S.CODE, S.PENNYLANE_ACTIF, S.PENNYLANE_ACHAT;
```

---

## ✅ Vérifications de base

### Checklist de dépannage niveau 1

#### 1. L'application est-elle démarrée ?
```bash
# Linux
systemctl status pennylane
# ou
ps aux | grep pennylane

# Windows
# Vérifier dans Services (services.msc)
```

#### 2. L'application répond-elle ?
```bash
# Test HTTP simple
curl http://localhost:8088/actuator/health

# Résultat attendu :
# {"status":"UP"}
```

#### 3. La base de données est-elle accessible ?
```sql
-- Depuis SSMS, se connecter à :
-- Serveur : NA-ATH01.mismo.local\ATHENEO
-- Base : ATHENEO_MISMO

-- Test simple
SELECT GETDATE() AS DATE_SERVEUR;
```

#### 4. Les schedulers s'exécutent-ils ?
```bash
# Chercher les logs de cron
grep "CRON" /var/log/pennylane/application.log | tail -20

# Résultat attendu :
# 🔄 [CRON ENTRIES] Démarrage de la synchronisation...
# ✅ [CRON ENTRIES] Fin de la synchronisation (X ms)
```

#### 5. Y a-t-il des erreurs dans les logs ?
```bash
# Dernières erreurs
grep "ERROR" /var/log/pennylane/application.log | tail -50

# Erreurs des dernières 24h avec comptage
grep "ERROR" /var/log/pennylane/application.log | \
  awk -v date="$(date -d '24 hours ago' '+%Y-%m-%d')" '$0 > date' | \
  wc -l
```

---

## 📝 Logs et monitoring

### Emplacements des logs

**Logs applicatifs** :
- Linux : `/var/log/pennylane/application.log`
- Windows : `C:\ProgramData\Pennylane\logs\application.log`
- Docker : `docker logs pennylane-app`

**Logs Spring Boot** :
- Console : `systemctl status pennylane` (dernières lignes)
- Fichier : Défini par `logging.file.name` dans application.yml

### Format des logs

```
2025-12-03 10:15:23 [pennylane-scheduler-1] INFO  fr.mismo.pennylane.Scheduler.schedulerAccounting - 🔄 [CRON ENTRIES] Démarrage...
2025-12-03 10:15:24 [pennylane-scheduler-1] INFO  fr.mismo.pennylane.Scheduler.schedulerAccounting - ✅ [CRON ENTRIES] Fin (1234 ms)
```

**Éléments** :
- Date/heure
- Thread (ex: `pennylane-scheduler-1`)
- Niveau (`ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`)
- Classe Java
- Message

### Logs importants à surveiller

#### ✅ Logs de succès
```
🔄 [CRON ENTRIES] Démarrage de la synchronisation
✅ [CRON ENTRIES] Fin de la synchronisation (1234 ms)
📊 Nombre de sites à traiter : 3
Écriture 12345 traitée en 250 ms
```

#### ⚠️ Logs d'avertissement
```
⚠️ Les catégories configurées et les catégories trouvées ne correspondent pas
Aucune écriture à synchroniser pour SITE01
Circuit breaker ouvert pour l'API Pennylane
```

#### ❌ Logs d'erreur
```
Erreur lors de la communication avec Pennylane
Erreur spécifique au service lors de la synchronisation
Timeout dépassé (30s) pour l'opération API
```

### Commandes de monitoring

```bash
# Suivre les logs en temps réel
tail -f /var/log/pennylane/application.log

# Suivre uniquement les erreurs
tail -f /var/log/pennylane/application.log | grep ERROR

# Compter les erreurs des 100 dernières lignes
tail -100 /var/log/pennylane/application.log | grep -c ERROR

# Chercher un mot-clé spécifique (ex: "facture 12345")
grep "12345" /var/log/pennylane/application.log

# Logs des dernières 5 minutes
find /var/log/pennylane/ -type f -mmin -5 -exec tail {} \;
```

---

## 🚨 Scénarios de dépannage courants

### Scénario 1 : "Les écritures ne se synchronisent plus"

**Symptômes** :
- Pas de logs `🔄 [CRON ENTRIES]`
- Écritures en attente dans la base

**Diagnostic** :
1. Vérifier que le scheduler est actif :
   ```yaml
   cron:
     Entries: "*/10 * * * * *"  # Doit être différent de "-"
   ```

2. Vérifier qu'il y a des écritures en attente :
   ```sql
   EXEC SP_PENNYLANE_EXPORT_LOT @SITE_ID = 1;
   ```

3. Vérifier les logs d'erreur :
   ```bash
   grep "CRON ENTRIES" /var/log/pennylane/application.log | tail -20
   ```

**Solutions** :
- ✅ CRON désactivé → Activer dans application.yml et redémarrer
- ✅ Erreurs API → Vérifier token Pennylane dans T_SITE
- ✅ Compte inexistant → Créer le compte dans Pennylane ou corriger dans ATHENEO

---

### Scénario 2 : "Erreur HTTP 401 Unauthorized"

**Symptômes** :
- Logs : `Erreur HTTP 401`
- Synchronisation bloquée

**Diagnostic** :
```sql
-- Vérifier les tokens API
SELECT CODE, PENNYLANE_TOKEN FROM T_SITE WHERE PENNYLANE_ACTIF = 1;
```

**Solutions** :
- ✅ Token vide → Récupérer nouveau token depuis Pennylane et mettre à jour T_SITE
- ✅ Token expiré → Régénérer token dans Pennylane
- ✅ Token incorrect → Vérifier copier/coller (espaces, caractères spéciaux)

**Mise à jour du token** :
```sql
UPDATE T_SITE
SET PENNYLANE_TOKEN = 'nouveau_token_ici'
WHERE CODE = 'SITE01';
```

---

### Scénario 3 : "Rate limit dépassé (HTTP 429)"

**Symptômes** :
- Logs : `HTTP 429 Too Many Requests`
- Ralentissement de la synchro

**Diagnostic** :
- Vérifier la fréquence des CRON dans application.yml
- Consulter les métriques Resilience4j : `http://localhost:8088/actuator/ratelimiters`

**Solutions** :
- ✅ Espacer les CRON :
  ```yaml
  cron:
    Entries: "*/30 * * * * *"  # 30s au lieu de 10s
  ```
- ✅ Augmenter le rate limiter :
  ```yaml
  resilience4j:
    ratelimiter:
      pennylaneAPI:
        limitForPeriod: 150  # Augmenter de 100 à 150
  ```

---

### Scénario 4 : "Circuit breaker ouvert"

**Symptômes** :
- Logs : `Circuit breaker ouvert pour l'API Pennylane`
- Toutes les requêtes échouent immédiatement

**Diagnostic** :
- Consulter l'état : `http://localhost:8088/actuator/circuitbreakers`
- Regarder les erreurs précédentes qui ont causé l'ouverture

**Solutions** :
- ⏳ Attendre 30 secondes (fermeture automatique configurée)
- ✅ Corriger la cause racine (token, réseau, API Pennylane en panne)
- ⚙️ Ajuster les seuils si trop sensible :
  ```yaml
  resilience4j:
    circuitbreaker:
      pennylaneAPI:
        failureRateThreshold: 70  # 70% au lieu de 50%
  ```

---

### Scénario 5 : "Base de données inaccessible"

**Symptômes** :
- Logs : `SQLException`, `Timeout`
- Application ne démarre pas

**Diagnostic** :
1. Tester la connexion depuis SSMS avec les credentials du YAML
2. Vérifier réseau : `ping NA-ATH01.mismo.local`
3. Vérifier firewall/ports

**Solutions** :
- ✅ Serveur DB éteint → Contacter équipe infrastructure
- ✅ Credentials incorrects → Vérifier application.yml
- ✅ Timeout réseau → Augmenter timeout ou vérifier réseau

---

### Scénario 6 : "Application consomme trop de mémoire"

**Symptômes** :
- Logs : `OutOfMemoryError`
- Serveur ralenti

**Diagnostic** :
```bash
# Utilisation mémoire (Linux)
ps aux | grep pennylane

# Heap dump JVM
jmap -heap <PID>
```

**Solutions** :
- ✅ Augmenter mémoire JVM :
  ```bash
  java -Xmx2g -Xms512m -jar interface-pennylane.jar
  ```
- ✅ Réduire la charge (espacer CRON, purger logs)
- ✅ Redémarrer l'application périodiquement

---

## 🌐 Endpoints API utiles

### Actuator (Monitoring Spring Boot)

#### Santé de l'application
```bash
curl http://localhost:8088/actuator/health
```
**Résultat attendu** :
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

#### Métriques Resilience4j
```bash
# État des circuit breakers
curl http://localhost:8088/actuator/circuitbreakers

# État des rate limiters
curl http://localhost:8088/actuator/ratelimiters

# Événements de retry
curl http://localhost:8088/actuator/retries
```

#### Informations applicatives
```bash
# Informations version
curl http://localhost:8088/actuator/info

# Métriques générales
curl http://localhost:8088/actuator/metrics

# Métrique spécifique (ex: mémoire)
curl http://localhost:8088/actuator/metrics/jvm.memory.used
```

#### Liste des endpoints disponibles
```bash
curl http://localhost:8088/actuator
```

---

### Dashboard web

**URL** : `http://localhost:8088/`

**Pages disponibles** :
- `/` : Page d'accueil / Dashboard
- `/logs` : Consultation des logs métier (table T_LOG)

**Authentification** :
- Définie par Spring Security dans le code
- Credentials par défaut à vérifier dans la configuration

---

## 🚀 Escalade niveau 2

### Quand escalader vers le niveau 2 ?

Escalader si :
- ❌ Erreurs dans les procédures stockées elles-mêmes
- ❌ Bugs applicatifs (NPE, ClassCastException, etc.)
- ❌ Problèmes de performance complexes
- ❌ Modifications du code nécessaires
- ❌ Migration/mise à jour de version

### Informations à fournir pour l'escalade

1. **Contexte** :
   - Quel scheduler/fonctionnalité est concerné ?
   - Depuis quand le problème se produit ?
   - Y a-t-il eu des changements récents ?

2. **Logs** :
   - Extraire les logs des dernières 24h ou depuis le début du problème
   - Filtrer sur ERROR et WARN
   - Inclure la stacktrace complète

3. **Configuration** :
   - Version de l'application (pom.xml ou logs de démarrage)
   - Extrait du application.yml concerné (⚠️ masquer les passwords/tokens)

4. **Base de données** :
   - Résultats des procédures stockées concernées
   - Nombre de lignes dans les tables (T_LOG, V_ECRITURE, etc.)

5. **Environnement** :
   - OS et version
   - Java version : `java -version`
   - Espace disque : `df -h`
   - Mémoire : `free -h`

### Template d'email d'escalade

```
Objet : [SUPPORT N2] Interface Pennylane - [Résumé du problème]

Bonjour,

Escalade niveau 2 requise pour l'interface Pennylane.

CONTEXTE :
- Fonctionnalité : Synchronisation des écritures comptables
- Début du problème : 03/12/2025 10:00
- Changements récents : Aucun

SYMPTÔMES :
- Les écritures ne se synchronisent plus depuis ce matin
- Logs : "Erreur lors de la communication avec Pennylane"

ACTIONS NIVEAU 1 EFFECTUÉES :
✅ Vérification CRON (actif)
✅ Vérification token API (OK)
✅ Redémarrage application (sans effet)
✅ Vérification procédure stockée (retourne bien des données)

LOGS (voir pièce jointe) :
[Extrait des logs avec stacktrace]

CONFIGURATION :
- Version : 1.10.2
- Java : 21
- CRON Entries : */10 * * * * *

BASES DE DONNÉES :
- Écritures en attente : 45
- Dernière synchro réussie : 02/12/2025 23:55

BESOIN :
Analyse approfondie des logs et correction du bug.

Merci,
[Votre nom]
Support Niveau 1
```

---

## 📞 Contacts

- **Support Niveau 2** : [email@ejemplo.com]
- **Admin Base de données** : [dba@ejemplo.com]
- **Infrastructure** : [infra@ejemplo.com]

---

## 📚 Ressources supplémentaires

- **Documentation détaillée des schedulers** : `DOCUMENTATION_SCHEDULERS.md`
- **Guide Resilience4j** : `application.yml` section `resilience4j:`
- **Documentation Pennylane API** : https://pennylane.readme.io/
- **Spring Boot Actuator** : https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html

---

**Dernière mise à jour** : 2025-12-03
**Version du guide** : 1.0
**Version application** : 1.10.2
