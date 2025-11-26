# Documentation Fonctionnelle - Interface ATHENEO ↔ PENNYLANE

## Table des matières

1. [Vue d'ensemble](#vue-densemble)
2. [Architecture](#architecture)
3. [Fonctionnalités principales](#fonctionnalités-principales)
4. [Flux de synchronisation](#flux-de-synchronisation)
5. [Configuration](#configuration)
6. [Monitoring et logs](#monitoring-et-logs)
7. [Sécurité](#sécurité)
8. [Résilience et fiabilité](#résilience-et-fiabilité)
9. [Gestion des erreurs](#gestion-des-erreurs)
10. [FAQ](#faq)

---

## Vue d'ensemble

### Description

L'**Interface ATHENEO-PENNYLANE** est une application Java Spring Boot qui assure la **synchronisation bidirectionnelle** des données comptables entre :
- **ATHENEO** : ERP de gestion interne
- **PENNYLANE** : Plateforme SaaS de gestion comptable

### Version
- **Version actuelle** : 1.10.2
- **Framework** : Spring Boot 3.3.0
- **Java** : Version 21
- **Base de données** : SQL Server (ATHENEO_MISMO)

### Objectifs

L'application permet de :
- ✅ Synchroniser automatiquement les factures de vente d'ATHENEO vers Pennylane
- ✅ Importer les factures d'achat depuis Pennylane vers ATHENEO
- ✅ Gérer les produits, clients et fournisseurs entre les deux systèmes
- ✅ Synchroniser les écritures comptables et le plan comptable
- ✅ Suivre les règlements et statuts de paiement
- ✅ Centraliser les logs et erreurs pour faciliter le support

---

## Architecture

### Architecture technique

```
┌─────────────────────────────────────────────────────────────┐
│                    Interface Web (Logs)                     │
│                    http://server:8093/api/v1                │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────┴─────────────────────────────────┐
│              Application Spring Boot                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Controllers  │  │  Schedulers  │  │   Services   │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   API REST   │  │  API SOAP    │  │ Repositories │     │
│  │  (Pennylane) │  │ (WSDocument) │  │     (JPA)    │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└───────┬──────────────────────┬──────────────────┬──────────┘
        │                      │                  │
        ▼                      ▼                  ▼
┌──────────────┐      ┌──────────────┐    ┌──────────────┐
│  Pennylane   │      │  WSDocument  │    │  SQL Server  │
│   API REST   │      │  SOAP (Docs) │    │  ATHENEO DB  │
└──────────────┘      └──────────────┘    └──────────────┘
```

### Composants principaux

#### 1. **Controllers**
- **LogController** : Interface web de consultation des logs
  - Dashboard avec statistiques
  - Recherche multicritères
  - Export PDF des logs
  - Détection des traitements lents

#### 2. **Schedulers (Tâches planifiées)**
- **schedulerAccounting** :
  - `syncEntries()` : Synchronisation des écritures comptables ATHENEO → Pennylane
  - `UpdateSale()` : Mise à jour des factures achats avec statut BAP
  - `purgeLogs()` : Purge des logs anciens

- **schedulerPurchases** :
  - `SyncPurchases()` : Import factures fournisseurs Pennylane → ATHENEO
  - `SyncPurchasesV2()` : Version optimisée avec changelog
  - `UpdatePurchaseReglement()` : Synchronisation des règlements
  - `UpdatePurchaseReglementV2()` : Version détaillée avec transactions

#### 3. **Services**
- **AccountingService** : Gestion écritures, factures ventes, produits, clients
- **InvoiceService** : Synchronisation factures achats et règlements
- **DocumentService** : Gestion documents PDF (conversion, base64)
- **WsDocumentService** : Appels SOAP vers WSDocumentAth
- **LogsService** : Gestion centralisée des logs applicatifs
- **CategoryCacheService** : Cache des catégories Pennylane

#### 4. **API Clients**
- **InvoiceApi** : Factures, catégories, changelog
- **CustomerApi** : Gestion clients
- **SupplierApi** : Gestion fournisseurs
- **ProductApi** : Gestion produits
- **AccountsApi** : Comptes comptables, plan comptable, upload fichiers

#### 5. **Repositories**
- Accès base de données via JPA
- Appels procédures stockées SQL Server
- Gestion transactionnelle

---

## Fonctionnalités principales

### 1. Synchronisation ATHENEO → PENNYLANE (Factures de vente)

**Processus** :
1. Récupération des lots d'écritures à exporter depuis ATHENEO
2. Pour chaque facture :
   - ✅ Création/MAJ du produit dans Pennylane (si nécessaire)
   - ✅ Création/MAJ du client dans Pennylane (si nécessaire)
   - ✅ Création de la facture client avec tous les détails
   - ✅ Récupération du PDF de la facture via WSDocument
   - ✅ Upload du PDF vers Pennylane
   - ✅ Mise à jour du statut dans ATHENEO

**Données synchronisées** :
- Informations client (nom, adresse, SIRET, TVA intracommunautaire)
- Lignes de facture (produits, quantités, prix, TVA)
- Dates (émission, échéance)
- Totaux (HT, TTC, TVA)
- Documents PDF

**Fréquence** : Configurable via cron `cron.Entries` (par défaut : toutes les 10 secondes)

---

### 2. Synchronisation PENNYLANE → ATHENEO (Factures d'achat)

**Processus** :
1. Récupération des factures fournisseurs depuis Pennylane
   - Filtrage par catégorie et statut
   - Pagination automatique
2. Pour chaque facture :
   - ✅ Création/MAJ de la facture dans ATHENEO
   - ✅ Téléchargement du PDF via Pennylane API
   - ✅ Stockage du PDF dans ATHENEO via WSDocument
   - ✅ Mise à jour des métadonnées

**Version V2 (optimisée)** :
- Utilise le changelog Pennylane pour récupérer uniquement les modifications
- Réduit significativement le nombre d'appels API
- Améliore les performances

**Données synchronisées** :
- Informations fournisseur
- Lignes de facture (produits, montants, TVA)
- Statuts de paiement
- Règlements (montants, dates, modes de paiement)
- Documents PDF

**Fréquence** : Configurable via cron `cron.Purchases` (désactivé par défaut, à activer en production)

---

### 3. Synchronisation des règlements

**Deux versions disponibles** :

#### Version globale (`UpdatePurchaseReglement`)
- Met à jour le statut de paiement global de la facture
- Statuts : `paid`, `unpaid`, `late`, `pending`

#### Version détaillée (`UpdatePurchaseReglementV2`)
- Synchronise toutes les transactions de paiement individuelles
- Inclut : montant, date, mode de paiement, référence
- Permet un suivi détaillé des règlements partiels

**Fréquence** : Configurable via cron (désactivé par défaut)

---

### 4. Gestion des produits

**Synchronisation automatique** :
- Création de nouveaux produits dans Pennylane lors de l'export de factures
- Mise à jour des produits existants (label, prix unitaire, taux TVA)
- Cache local pour améliorer les performances

**Données produit** :
- Label (nom du produit)
- Prix unitaire
- Taux de TVA
- Unité (pièce, heure, etc.)
- Référence

---

### 5. Gestion des clients et fournisseurs

**Clients (ATHENEO → Pennylane)** :
- Création automatique lors de l'export de factures
- Mise à jour des informations existantes
- Support des sociétés et particuliers

**Données client/fournisseur** :
- Raison sociale ou nom complet
- Adresse complète
- SIRET / SIREN
- Numéro TVA intracommunautaire
- Email
- Téléphone
- Coordonnées bancaires (IBAN, BIC)

---

### 6. Plan comptable et écritures

**Synchronisation du plan comptable** :
- Création automatique des comptes comptables manquants dans Pennylane
- Mapping entre ATHENEO et Pennylane

**Écritures comptables** :
- Export des lots d'écritures vers Pennylane
- Support des imputations analytiques
- Gestion des dates d'imputation

---

### 7. Interface de consultation des logs

**Dashboard** :
- Statistiques globales (total logs, erreurs, avertissements)
- Graphiques de répartition
- Accès rapide aux dernières erreurs

**Recherche avancée** :
- Filtres : niveau (ERROR, WARN, INFO, DEBUG, TRACE)
- Traitement spécifique
- Période (date début/fin)
- Recherche textuelle dans les messages
- Site concerné

**Fonctionnalités** :
- ✅ Consultation détaillée de chaque log
- ✅ Export PDF d'un log individuel
- ✅ Détection automatique des traitements lents (> seuil configurable)
- ✅ Pagination et tri

**Accès** : `http://server:8093/api/v1/logs`

---

## Flux de synchronisation

### Flux 1 : Export facture de vente (ATHENEO → Pennylane)

```
┌─────────────┐
│  ATHENEO DB │
│   (Lot en   │
│   attente)  │
└──────┬──────┘
       │ 1. Récupération lot
       ▼
┌─────────────────────────┐
│  AccountingService      │
│  syncEntries()          │
└──────┬──────────────────┘
       │ 2. Pour chaque facture
       ▼
┌─────────────────────────┐      ┌──────────────┐
│ Vérifier/Créer Produit  │─────▶│  Pennylane   │
└──────┬──────────────────┘      │     API      │
       │                         └──────────────┘
       ▼
┌─────────────────────────┐      ┌──────────────┐
│ Vérifier/Créer Client   │─────▶│  Pennylane   │
└──────┬──────────────────┘      │     API      │
       │                         └──────────────┘
       ▼
┌─────────────────────────┐      ┌──────────────┐
│ Télécharger PDF facture │─────▶│  WSDocument  │
│  depuis ATHENEO         │      │     SOAP     │
└──────┬──────────────────┘      └──────────────┘
       │
       ▼
┌─────────────────────────┐      ┌──────────────┐
│ Créer facture Pennylane │─────▶│  Pennylane   │
└──────┬──────────────────┘      │     API      │
       │                         └──────────────┘
       ▼
┌─────────────────────────┐      ┌──────────────┐
│  Upload PDF facture     │─────▶│  Pennylane   │
└──────┬──────────────────┘      │     API      │
       │                         └──────────────┘
       ▼
┌─────────────────────────┐
│ MAJ statut dans ATHENEO │
│   (Lot traité)          │
└─────────────────────────┘
```

### Flux 2 : Import facture d'achat (Pennylane → ATHENEO)

```
┌──────────────┐
│  Pennylane   │
│     API      │
│  (Factures   │
│ fournisseur) │
└──────┬───────┘
       │ 1. Liste factures (filtres + pagination)
       ▼
┌─────────────────────────┐
│  InvoiceService         │
│  SyncPurchasesV2()      │
└──────┬──────────────────┘
       │ 2. Pour chaque facture
       ▼
┌─────────────────────────┐      ┌──────────────┐
│ Récupérer détails       │─────▶│  Pennylane   │
│ facture complète        │      │     API      │
└──────┬──────────────────┘      └──────────────┘
       │
       ▼
┌─────────────────────────┐      ┌──────────────┐
│ Télécharger PDF         │─────▶│  Pennylane   │
│ de la facture           │      │     API      │
└──────┬──────────────────┘      └──────────────┘
       │
       ▼
┌─────────────────────────┐      ┌──────────────┐
│ Créer/MAJ facture       │─────▶│  ATHENEO DB  │
│ dans ATHENEO            │      │  (Proc SQL)  │
└──────┬──────────────────┘      └──────────────┘
       │
       ▼
┌─────────────────────────┐      ┌──────────────┐
│ Stocker PDF dans        │─────▶│  WSDocument  │
│ ATHENEO via SOAP        │      │     SOAP     │
└─────────────────────────┘      └──────────────┘
```

---

## Configuration

### Fichiers de configuration

#### 1. **application.yml**

Fichier principal de configuration. **IMPORTANT** : Ne JAMAIS committer ce fichier avec des credentials en clair.

Utiliser plutôt des variables d'environnement :

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

wsdocument:
  defaultUri: ${WSDOCUMENT_URI}
  login: ${WSDOCUMENT_LOGIN}
  password: ${WSDOCUMENT_PASSWORD}

security:
  user:
    name: ${SECURITY_USERNAME:admin}
    password: ${SECURITY_PASSWORD:changeme}
```

#### 2. **Variables d'environnement (.env)**

Créer un fichier `.env` à partir de `.env.example` :

```bash
# Base de données
DB_URL=jdbc:sqlserver://SERVER\\INSTANCE;databaseName=ATHENEO_MISMO;encrypt=false
DB_USERNAME=atheneo_sql
DB_PASSWORD=votre_mot_de_passe_secret

# WSDocument
WSDOCUMENT_URI=http://server:8081/WSDocumentAth/WSDocumentAth.svc
WSDOCUMENT_LOGIN=admin
WSDOCUMENT_PASSWORD=mot_de_passe_wsdoc

# Sécurité
SECURITY_USERNAME=admin
SECURITY_PASSWORD=mot_de_passe_securise

# Crons (format cron ou "-" pour désactiver)
CRON_ENTRIES=*/10 * * * * *
CRON_PURCHASES=-
```

### Configuration des tâches planifiées

Les tâches sont configurées via les propriétés `cron.*` :

| Tâche | Propriété | Description | Exemple |
|-------|-----------|-------------|---------|
| Écritures comptables | `cron.Entries` | Export ATHENEO → Pennylane | `*/10 * * * * *` (toutes les 10s) |
| Factures achats | `cron.Purchases` | Import Pennylane → ATHENEO | `0 */30 * * * *` (toutes les 30 min) |
| Règlements | `cron.PurchaseReglement` | Sync règlements | `0 0 * * * *` (toutes les heures) |
| Purge logs | `cron.PurgeLog` | Nettoyage logs anciens | `0 0 2 * * *` (tous les jours à 2h) |

**Format cron** : `secondes minutes heures jour mois jour_semaine`
**Désactiver** : utiliser `-` comme valeur

### Configuration de la sécurité

#### Mode développement (désactiver la sécurité)
```yaml
security:
  basic:
    enabled: false
```

#### Mode production (sécurité activée)
```yaml
security:
  basic:
    enabled: true
  user:
    name: ${SECURITY_USERNAME}
    password: ${SECURITY_PASSWORD}
```

**Authentification** : Basic HTTP
**Accès API** : Fournir username/password dans chaque requête

### Configuration de la résilience

La résilience est configurée automatiquement via Resilience4j :

#### Circuit Breaker
```yaml
resilience4j:
  circuitbreaker:
    instances:
      pennylane-api:
        slidingWindowSize: 20
        failureRateThreshold: 40
        waitDurationInOpenState: 30s
```

#### Retry
```yaml
resilience4j:
  retry:
    instances:
      pennylane-api:
        maxAttempts: 4
        waitDuration: 2s
        enableExponentialBackoff: true
```

#### Rate Limiter
```yaml
resilience4j:
  ratelimiter:
    instances:
      pennylane-api:
        limitForPeriod: 2
        limitRefreshPeriod: 1s
```

---

## Monitoring et logs

### Endpoints Actuator

L'application expose plusieurs endpoints de monitoring via Spring Boot Actuator :

| Endpoint | Description | Accès |
|----------|-------------|-------|
| `/actuator/health` | État de santé de l'application | Public |
| `/actuator/metrics` | Métriques applicatives | Authentifié |
| `/actuator/prometheus` | Métriques format Prometheus | Authentifié |
| `/actuator/info` | Informations sur l'application | Authentifié |

**URL de base** : `http://server:8093/api/v1/actuator`

### Health Checks

```bash
curl http://server:8093/api/v1/actuator/health
```

Réponse :
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "circuitBreakers": { "status": "UP" }
  }
}
```

### Métriques Prometheus

```bash
curl http://admin:password@server:8093/api/v1/actuator/prometheus
```

Métriques disponibles :
- `http_server_requests_seconds` : Latence des requêtes HTTP
- `resilience4j_circuitbreaker_state` : État des circuit breakers
- `resilience4j_retry_calls` : Nombre de retry
- `jvm_memory_used_bytes` : Utilisation mémoire
- `system_cpu_usage` : CPU usage

### Logs applicatifs

#### Niveaux de logs

| Niveau | Description | Utilisation |
|--------|-------------|-------------|
| ERROR | Erreur bloquante | Échecs critiques nécessitant intervention |
| WARN | Avertissement | Situations anormales mais non bloquantes |
| INFO | Information | Événements métier importants |
| DEBUG | Débogage | Informations détaillées pour débogage |
| TRACE | Trace détaillée | Trace complète des appels (dev uniquement) |

#### Stockage des logs

Les logs sont stockés :
1. **Console** : Affichage temps réel (stdout)
2. **Base de données** : Table `LOG` pour consultation via interface web
3. **Forum** : Traçabilité métier dans tables `FORUM` et `FORUM_LIGNE`

#### Interface web de logs

Accès : `http://server:8093/api/v1/logs`

Fonctionnalités :
- 📊 Dashboard avec statistiques
- 🔍 Recherche multicritères
- 📄 Export PDF
- ⚠️ Alerte traitements lents
- 📅 Filtrage par période

---

## Sécurité

### Authentification

L'application utilise **HTTP Basic Authentication** :

```bash
curl -u admin:password http://server:8093/api/v1/logs
```

### Protection CSRF

La protection CSRF est activée par défaut en production :
- Utilise des cookies HttpOnly
- Exemption pour les health checks
- Token CSRF requis pour les requêtes POST/PUT/DELETE

### Bonnes pratiques de sécurité

✅ **FAIRE** :
- Utiliser des variables d'environnement pour les credentials
- Changer le mot de passe par défaut
- Activer la sécurité en production (`security.basic.enabled=true`)
- Utiliser HTTPS en production
- Limiter l'accès réseau (firewall)

❌ **NE PAS FAIRE** :
- Committer des mots de passe dans Git
- Désactiver CSRF en production
- Utiliser le mot de passe par défaut
- Exposer l'application directement sur Internet sans reverse proxy

### Chiffrement des credentials

**Recommandations pour la production** :

1. **Utiliser un vault** (HashiCorp Vault, AWS Secrets Manager, Azure Key Vault)
2. **Variables d'environnement** injectées au runtime
3. **Spring Cloud Config** avec chiffrement
4. **Jasypt** pour chiffrer les propriétés

---

## Résilience et fiabilité

### Patterns de résilience implémentés

#### 1. Circuit Breaker

Protège contre les cascades de pannes :

```
États :
CLOSED → OPEN → HALF_OPEN → CLOSED
  ↑                            │
  └────────────────────────────┘

CLOSED : Fonctionnement normal
OPEN : Service défaillant, requêtes rejetées immédiatement
HALF_OPEN : Test de rétablissement
```

**Configuration** :
- Seuil d'erreur : 40% pour Pennylane API
- Fenêtre d'observation : 20 appels
- Attente avant retry : 30 secondes

#### 2. Retry avec backoff exponentiel

Retry automatique des appels échoués :

```
Tentative 1 : immédiat
Tentative 2 : +2s
Tentative 3 : +4s
Tentative 4 : +8s
```

**Configuration** :
- Max tentatives : 4 pour Pennylane
- Retry sur : erreurs réseau, erreurs serveur 5xx
- Backoff exponentiel activé

#### 3. Rate Limiting

Respect des quotas API :

**Pennylane** :
- Limite : 2 requêtes/seconde
- Remplace les `Thread.sleep()` bloquants
- Timeout si limite atteinte : 5 secondes

#### 4. Gestion des timeouts

**API REST** :
- Connexion : 10 secondes
- Lecture : 30 secondes

**SOAP WSDocument** :
- Connexion : 60 secondes
- Lecture : 60 secondes

### Stratégies de recovery

En cas d'échec :
1. **Retry automatique** (jusqu'à 4 fois avec backoff)
2. **Circuit breaker** ouvre si trop d'échecs
3. **Logs détaillés** pour diagnostic
4. **Continuation du traitement** pour les autres éléments

### Surveillance de la fiabilité

Métriques à surveiller :
- `resilience4j_circuitbreaker_state` : État des circuit breakers
- `resilience4j_circuitbreaker_failure_rate` : Taux d'échec
- `resilience4j_retry_calls_total` : Nombre total de retry
- Interface logs : Erreurs récentes et traitements lents

---

## Gestion des erreurs

### Types d'erreurs

| Type | Gravité | Traitement | Exemple |
|------|---------|------------|---------|
| Erreur réseau temporaire | WARN | Retry auto | Timeout HTTP |
| Erreur API 4xx | ERROR | Log + skip | Client non trouvé |
| Erreur API 5xx | ERROR | Retry + log | Serveur indisponible |
| Erreur métier | ERROR | Log + notification | Catégorie invalide |
| Erreur technique | ERROR | Log + alerte | NullPointerException |

### Stratégie par type d'erreur

#### Erreurs réseau (timeout, connexion)
- ✅ Retry automatique (4 tentatives)
- ✅ Backoff exponentiel
- ✅ Log WARN
- ✅ Continue avec l'élément suivant

#### Erreurs API 4xx (client)
- ❌ Pas de retry (erreur client permanente)
- ✅ Log ERROR détaillé
- ✅ Skip l'élément
- ✅ Continue le traitement

#### Erreurs API 5xx (serveur)
- ✅ Retry automatique
- ✅ Circuit breaker si trop d'échecs
- ✅ Log ERROR
- ✅ Alerte si circuit ouvert

#### Erreurs métier
- ✅ Log ERROR avec contexte
- ✅ Enregistrement en base (table LOG)
- ✅ Visible dans l'interface web
- ⚠️ Nécessite investigation manuelle

### Consultation des erreurs

#### Via l'interface web

1. Accéder à `http://server:8093/api/v1/logs/errors`
2. Filtrer par :
   - Période
   - Type de traitement
   - Niveau (ERROR uniquement)
3. Consulter le détail de l'erreur
4. Télécharger le rapport PDF si nécessaire

#### Via les logs applicatifs

```bash
# Logs en temps réel
tail -f /var/log/pennylane-interface.log | grep ERROR

# Recherche d'erreurs spécifiques
grep "Pennylane API" /var/log/pennylane-interface.log | grep ERROR
```

#### Via la base de données

```sql
-- Erreurs des dernières 24h
SELECT *
FROM LOG
WHERE NIVEAU = 'ERROR'
  AND DATE_ENREGISTREMENT > DATEADD(day, -1, GETDATE())
ORDER BY DATE_ENREGISTREMENT DESC;

-- Top 10 des erreurs les plus fréquentes
SELECT TOP 10
    TRAITEMENT,
    MESSAGE,
    COUNT(*) as NB_OCCURRENCES
FROM LOG
WHERE NIVEAU = 'ERROR'
  AND DATE_ENREGISTREMENT > DATEADD(day, -7, GETDATE())
GROUP BY TRAITEMENT, MESSAGE
ORDER BY NB_OCCURRENCES DESC;
```

---

## FAQ

### Questions fréquentes

#### Q1 : Comment activer/désactiver une tâche planifiée ?

**R** : Modifier la propriété `cron.*` correspondante dans `application.yml` ou via variable d'environnement :

```yaml
# Activer (toutes les 30 minutes)
cron:
  Purchases: "0 */30 * * * *"

# Désactiver
cron:
  Purchases: "-"
```

Redémarrer l'application pour prendre en compte le changement.

---

#### Q2 : Pourquoi mes factures ne sont pas synchronisées ?

**R** : Vérifier dans l'ordre :

1. **Tâche planifiée activée ?**
   ```bash
   # Vérifier les logs au démarrage
   grep "Scheduled" /var/log/pennylane-interface.log
   ```

2. **Erreurs dans les logs ?**
   - Interface web : `http://server:8093/api/v1/logs/errors`
   - Filtrer par traitement concerné

3. **Circuit breaker ouvert ?**
   ```bash
   curl http://admin:pass@server:8093/api/v1/actuator/health
   # Vérifier status des circuit breakers
   ```

4. **Token Pennylane valide ?**
   - Vérifier table `T_SITE.PENNYLANE_TOKEN`
   - Tester avec curl :
     ```bash
     curl -H "Authorization: Bearer YOUR_TOKEN" \
       https://app.pennylane.com/api/external/v2/customer_invoices
     ```

5. **Connectivité réseau ?**
   ```bash
   curl -I https://app.pennylane.com
   ```

---

#### Q3 : Comment changer les credentials de la base de données ?

**R** :

1. **Avec variables d'environnement** (recommandé) :
   ```bash
   export DB_USERNAME=nouveau_user
   export DB_PASSWORD=nouveau_pass
   ```

2. **Modifier application.yml** :
   ```yaml
   spring:
     datasource:
       username: nouveau_user
       password: nouveau_pass
   ```

3. Redémarrer l'application

---

#### Q4 : Comment augmenter la fréquence de synchronisation ?

**R** : Modifier la propriété cron correspondante :

```yaml
# Avant : toutes les 10 secondes
cron:
  Entries: "*/10 * * * * *"

# Après : toutes les 5 secondes
cron:
  Entries: "*/5 * * * * *"
```

⚠️ **Attention** : Respecter le rate limit Pennylane (2 req/s)

---

#### Q5 : Comment désactiver temporairement la sécurité pour le développement ?

**R** :

```yaml
security:
  basic:
    enabled: false
```

⚠️ **JAMAIS en production !**

---

#### Q6 : Que faire si le circuit breaker est ouvert ?

**R** :

1. **Identifier la cause** :
   - Consulter les logs d'erreurs
   - Vérifier la disponibilité du service externe

2. **Corriger le problème** :
   - Réseau : vérifier connectivité
   - API : vérifier status Pennylane
   - Credentials : vérifier token valide

3. **Attendre la fermeture automatique** :
   - Le circuit passe en HALF_OPEN après 30s
   - Test automatique de rétablissement
   - Fermeture si les tests réussissent

4. **Ou redémarrer l'application** (réinitialise les circuit breakers)

---

#### Q7 : Comment purger les anciens logs ?

**R** : Activer la tâche de purge :

```yaml
cron:
  PurgeLog: "0 0 2 * * *"  # Tous les jours à 2h du matin
```

Ou manuellement en SQL :

```sql
-- Supprimer logs de plus de 90 jours
DELETE FROM LOG
WHERE DATE_ENREGISTREMENT < DATEADD(day, -90, GETDATE());
```

---

#### Q8 : Comment surveiller les performances ?

**R** :

1. **Interface web** : Section "Traitements lents"
   - `http://server:8093/api/v1/logs/slow`

2. **Métriques Prometheus** :
   ```bash
   curl http://admin:pass@server:8093/api/v1/actuator/prometheus
   ```

3. **Requêtes SQL sur les logs** :
   ```sql
   -- Durées moyennes par traitement
   SELECT
       TRAITEMENT,
       AVG(DUREE_MS) as DUREE_MOYENNE,
       MAX(DUREE_MS) as DUREE_MAX,
       COUNT(*) as NB_EXECUTIONS
   FROM LOG
   WHERE DATE_ENREGISTREMENT > DATEADD(day, -7, GETDATE())
   GROUP BY TRAITEMENT
   ORDER BY DUREE_MOYENNE DESC;
   ```

---

#### Q9 : Comment exporter les données vers un nouveau comptable ?

**R** :

1. **Export factures depuis Pennylane** :
   - Interface Pennylane : menu Export
   - Formats : CSV, Excel, PDF

2. **Export depuis ATHENEO** :
   - Requêtes SQL sur tables métier
   - Export via procédures stockées

3. **API Pennylane** :
   - Utiliser les endpoints de l'API pour export programmatique
   - Documentation : https://pennylane.readme.io/

---

#### Q10 : L'application peut-elle gérer plusieurs sites/sociétés ?

**R** : **Oui**, l'application est multi-sites :

- Configuration par site dans table `T_SITE`
- Token Pennylane spécifique par site
- Filtrage automatique des données par site
- Logs et forum tracent le site concerné

Configuration :
```sql
SELECT
    ID_SITE,
    NOM_SITE,
    PENNYLANE_TOKEN,
    ACTIF
FROM T_SITE;
```

---

## Glossaire

| Terme | Description |
|-------|-------------|
| **ATHENEO** | ERP de gestion interne (système source) |
| **Pennylane** | Plateforme SaaS de gestion comptable (système cible) |
| **Circuit Breaker** | Pattern de résilience qui détecte les défaillances et évite les appels inutiles |
| **Retry** | Mécanisme de retry automatique des appels échoués |
| **Rate Limiter** | Limitation du nombre de requêtes par période |
| **CSRF** | Cross-Site Request Forgery (protection contre les attaques) |
| **Backoff exponentiel** | Augmentation progressive du délai entre les retry |
| **WSDocument** | Service SOAP de gestion documentaire ATHENEO |
| **Changelog** | Journal des modifications (utilisé pour sync incrémentale) |
| **BAP** | Bon À Payer (statut de validation d'une facture) |

---

## Support et contact

Pour toute question ou problème :

1. **Consulter les logs** : Interface web ou base de données
2. **Vérifier la FAQ** ci-dessus
3. **Consulter la documentation technique** : README.md
4. **Contacter le support** : [email/contact à définir]

---

**Version de la documentation** : 1.0
**Dernière mise à jour** : 2025-11-26
**Auteur** : Interface ATHENEO-PENNYLANE Team
