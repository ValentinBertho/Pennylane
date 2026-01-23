# Revue et Professionnalisation des Logs - Interface PENNYLANE

**Version** : 1.0
**Date de rédaction** : Janvier 2026
**Public cible** : Équipe Support, Fonctionnels, Développeurs

---

## Table des matières

1. [Analyse de l'existant](#1-analyse-de-lexistant)
2. [Manques identifiés](#2-manques-identifiés)
3. [Proposition de nouveaux logs](#3-proposition-de-nouveaux-logs)
4. [Reformulation des messages existants](#4-reformulation-des-messages-existants)
5. [Exemples de messages bien rédigés](#5-exemples-de-messages-bien-rédigés)
6. [Matrice de visibilité des logs](#6-matrice-de-visibilité-des-logs)
7. [Recommandations de mise en œuvre](#7-recommandations-de-mise-en-œuvre)

---

## 1. Analyse de l'existant

### 1.1 Infrastructure de logging actuelle

L'interface PENNYLANE dispose d'une infrastructure de logging structurée autour de plusieurs composants :

| Composant | Description | Usage actuel |
|-----------|-------------|--------------|
| **Table LOG** | Table centralisée avec champs détaillés (date, niveau, message, classe, méthode, durée, etc.) | Traçabilité technique |
| **Table ERROR_LOGS** | Table dédiée aux erreurs SQL | Erreurs de procédures stockées |
| **Table FORUM / FORUM_LIGNE** | Historique métier des traitements | Suivi des factures/écritures |
| **LogService** | Service Java centralisé | Écriture des logs |
| **LogHelper** | Utilitaire de formatage | Messages de contexte HTTP |

### 1.2 Niveaux de logs utilisés

| Niveau | Usage actuel | Fréquence |
|--------|--------------|-----------|
| **TRACE** | Détails très fins (debug) | Rare |
| **DEBUG** | Informations de débogage | Occasionnel |
| **INFO** | Étapes principales | Fréquent |
| **WARN** | Avertissements | Modéré |
| **ERROR** | Erreurs applicatives | Variable |
| **FATAL** | Erreurs critiques | Très rare |

### 1.3 Points de logging actuels

**Points bien couverts :**
- Erreurs HTTP (codes retour, trames requête/réponse)
- Erreurs SQL (procédures stockées, deadlocks)
- Durée d'exécution des traitements
- Informations de contexte (IP, URL, méthode HTTP)

**Points partiellement couverts :**
- Début/fin des flux de synchronisation
- Étapes intermédiaires des traitements
- Statistiques de traitement (nombre de factures, taux de succès)

---

## 2. Manques identifiés

### 2.1 Manques fonctionnels

| Manque | Impact | Priorité |
|--------|--------|----------|
| **Absence de log de démarrage de flux** | Impossible de savoir quand un flux a réellement démarré | Haute |
| **Absence de log de fin de flux avec bilan** | Pas de vision synthétique du résultat d'un traitement | Haute |
| **Messages peu explicites pour les fonctionnels** | Difficulté à comprendre les logs sans connaissance technique | Haute |
| **Pas de corrélation entre logs** | Difficulté à suivre un traitement de bout en bout | Moyenne |
| **Statistiques de traitement absentes** | Pas de KPIs exploitables | Moyenne |
| **Logs de reprise non identifiables** | Confusion entre traitement initial et reprise | Moyenne |

### 2.2 Manques techniques

| Manque | Impact | Priorité |
|--------|--------|----------|
| **Pas d'identifiant de corrélation (correlation ID)** | Impossible de tracer un flux complet | Haute |
| **Logs de performance incomplets** | Difficile d'identifier les goulots d'étranglement | Moyenne |
| **Pas de métriques de circuit breaker** | Pas de visibilité sur l'état de résilience | Moyenne |
| **Contexte métier insuffisant** | Logs difficiles à relier aux données métier | Moyenne |

### 2.3 Manques organisationnels

| Manque | Impact | Priorité |
|--------|--------|----------|
| **Pas de distinction claire des audiences** | Tous les logs mélangés | Moyenne |
| **Pas de convention de nommage** | Messages hétérogènes | Basse |
| **Pas de catégorisation des logs** | Filtrage difficile | Basse |

---

## 3. Proposition de nouveaux logs

### 3.1 Logs de début et fin de flux

#### 3.1.1 Flux F1 - Synchronisation des écritures

**DÉBUT DE FLUX**
```
[INFO] [SYNC-ECRITURES] ══════════════════════════════════════════════════
[INFO] [SYNC-ECRITURES] DÉMARRAGE - Synchronisation des écritures comptables
[INFO] [SYNC-ECRITURES] ══════════════════════════════════════════════════
[INFO] [SYNC-ECRITURES] Correlation ID: SYNC-ECR-20260123-143052-A7B2
[INFO] [SYNC-ECRITURES] Heure de démarrage: 2026-01-23 14:30:52
[INFO] [SYNC-ECRITURES] Lots à traiter: 5 lot(s) identifié(s)
```

**FIN DE FLUX - SUCCÈS**
```
[INFO] [SYNC-ECRITURES] ══════════════════════════════════════════════════
[INFO] [SYNC-ECRITURES] FIN - Synchronisation des écritures comptables
[INFO] [SYNC-ECRITURES] ══════════════════════════════════════════════════
[INFO] [SYNC-ECRITURES] Correlation ID: SYNC-ECR-20260123-143052-A7B2
[INFO] [SYNC-ECRITURES] Durée totale: 12.450 secondes
[INFO] [SYNC-ECRITURES] BILAN:
[INFO] [SYNC-ECRITURES]   • Lots traités: 5/5 (100%)
[INFO] [SYNC-ECRITURES]   • Factures créées: 23
[INFO] [SYNC-ECRITURES]   • Factures ignorées (doublons): 2
[INFO] [SYNC-ECRITURES]   • Clients créés: 3
[INFO] [SYNC-ECRITURES]   • Produits créés: 7
[INFO] [SYNC-ECRITURES]   • Documents uploadés: 21
[INFO] [SYNC-ECRITURES]   • Erreurs: 0
[INFO] [SYNC-ECRITURES] Statut final: SUCCÈS
```

**FIN DE FLUX - AVEC ERREURS**
```
[WARN] [SYNC-ECRITURES] ══════════════════════════════════════════════════
[WARN] [SYNC-ECRITURES] FIN - Synchronisation des écritures comptables
[WARN] [SYNC-ECRITURES] ══════════════════════════════════════════════════
[WARN] [SYNC-ECRITURES] Correlation ID: SYNC-ECR-20260123-143052-A7B2
[WARN] [SYNC-ECRITURES] Durée totale: 45.230 secondes
[WARN] [SYNC-ECRITURES] BILAN:
[WARN] [SYNC-ECRITURES]   • Lots traités: 4/5 (80%)
[WARN] [SYNC-ECRITURES]   • Factures créées: 18
[WARN] [SYNC-ECRITURES]   • Factures en erreur: 3
[WARN] [SYNC-ECRITURES]   • Erreurs détaillées:
[WARN] [SYNC-ECRITURES]     - FAC-2026-0042: Montant HT invalide (0€)
[WARN] [SYNC-ECRITURES]     - FAC-2026-0043: Client introuvable (SIRET: 12345678901234)
[WARN] [SYNC-ECRITURES]     - FAC-2026-0044: Erreur API Pennylane (HTTP 422)
[WARN] [SYNC-ECRITURES] Statut final: PARTIEL - Action requise
```

#### 3.1.2 Flux F3 - Synchronisation des factures fournisseurs

**DÉBUT DE FLUX**
```
[INFO] [SYNC-ACHATS] ══════════════════════════════════════════════════
[INFO] [SYNC-ACHATS] DÉMARRAGE - Import des factures fournisseurs
[INFO] [SYNC-ACHATS] ══════════════════════════════════════════════════
[INFO] [SYNC-ACHATS] Correlation ID: SYNC-ACH-20260123-060015-C3D4
[INFO] [SYNC-ACHATS] Période de recherche: 360 jours (depuis le 2025-01-28)
[INFO] [SYNC-ACHATS] Filtres actifs:
[INFO] [SYNC-ACHATS]   • Statuts: to_be_processed, to_be_paid
[INFO] [SYNC-ACHATS]   • Catégories: ACH
```

**FIN DE FLUX**
```
[INFO] [SYNC-ACHATS] ══════════════════════════════════════════════════
[INFO] [SYNC-ACHATS] FIN - Import des factures fournisseurs
[INFO] [SYNC-ACHATS] ══════════════════════════════════════════════════
[INFO] [SYNC-ACHATS] Correlation ID: SYNC-ACH-20260123-060015-C3D4
[INFO] [SYNC-ACHATS] Durée totale: 2 min 34 sec
[INFO] [SYNC-ACHATS] BILAN:
[INFO] [SYNC-ACHATS]   • Factures récupérées (brut): 350
[INFO] [SYNC-ACHATS]   • Factures après filtrage: 25
[INFO] [SYNC-ACHATS]   • Factures importées: 23
[INFO] [SYNC-ACHATS]   • Factures ignorées (existantes): 2
[INFO] [SYNC-ACHATS]   • Fournisseurs créés: 1
[INFO] [SYNC-ACHATS]   • Documents téléchargés: 20
[INFO] [SYNC-ACHATS] Statut final: SUCCÈS
```

### 3.2 Logs d'étapes clés fonctionnelles

#### 3.2.1 Traitement d'une facture

```
[INFO] [SYNC-ECRITURES] ─────────────────────────────────────────────────
[INFO] [SYNC-ECRITURES] Traitement facture: FAC-2026-0042
[INFO] [SYNC-ECRITURES] ─────────────────────────────────────────────────
[DEBUG] [SYNC-ECRITURES] [FAC-2026-0042] Vérification doublon... OK (nouvelle facture)
[DEBUG] [SYNC-ECRITURES] [FAC-2026-0042] Validation montants... OK (HT: 1500.00€, TVA: 300.00€, TTC: 1800.00€)
[INFO] [SYNC-ECRITURES] [FAC-2026-0042] Client: ENTREPRISE DUPONT (SIRET: 12345678901234)
[DEBUG] [SYNC-ECRITURES] [FAC-2026-0042] Client existant dans Pennylane (ID: cust_abc123)
[INFO] [SYNC-ECRITURES] [FAC-2026-0042] Produits: 3 article(s) à synchroniser
[DEBUG] [SYNC-ECRITURES] [FAC-2026-0042] Produit "PRESTATION-001" créé (ID: prod_xyz789)
[INFO] [SYNC-ECRITURES] [FAC-2026-0042] Document PDF: facture_2026_0042.pdf (téléversé avec succès)
[INFO] [SYNC-ECRITURES] [FAC-2026-0042] ✓ Facture créée dans Pennylane (ID: inv_def456)
[INFO] [SYNC-ECRITURES] [FAC-2026-0042] Durée de traitement: 2.340 sec
```

#### 3.2.2 Traitement d'un règlement

```
[INFO] [SYNC-REGLEMENTS] ─────────────────────────────────────────────────
[INFO] [SYNC-REGLEMENTS] Mise à jour règlement: FAC-2026-0042
[INFO] [SYNC-REGLEMENTS] ─────────────────────────────────────────────────
[DEBUG] [SYNC-REGLEMENTS] [FAC-2026-0042] Récupération transactions Pennylane...
[INFO] [SYNC-REGLEMENTS] [FAC-2026-0042] Transactions trouvées: 2
[INFO] [SYNC-REGLEMENTS] [FAC-2026-0042]   • 2026-01-15: +900.00€ (virement)
[INFO] [SYNC-REGLEMENTS] [FAC-2026-0042]   • 2026-01-20: +900.00€ (virement)
[INFO] [SYNC-REGLEMENTS] [FAC-2026-0042] Montant total: 1800.00€ / 1800.00€
[INFO] [SYNC-REGLEMENTS] [FAC-2026-0042] ✓ Statut: ENTIÈREMENT PAYÉE
```

### 3.3 Logs d'anomalies fonctionnelles

#### 3.3.1 Anomalies métier (non bloquantes)

```
[WARN] [SYNC-ECRITURES] [FAC-2026-0045] ⚠ Incohérence montants détectée
[WARN] [SYNC-ECRITURES] [FAC-2026-0045]   HT (1500.00€) + TVA (300.00€) ≠ TTC (1802.00€)
[WARN] [SYNC-ECRITURES] [FAC-2026-0045]   Écart: 2.00€ - Traitement poursuivi avec montants d'origine

[WARN] [SYNC-ECRITURES] [FAC-2026-0046] ⚠ SIRET invalide pour client "ENTREPRISE ABC"
[WARN] [SYNC-ECRITURES] [FAC-2026-0046]   SIRET fourni: 1234567890123 (format incorrect)
[WARN] [SYNC-ECRITURES] [FAC-2026-0046]   Client créé sans SIRET - Correction manuelle requise

[WARN] [SYNC-REGLEMENTS] [FAC-2026-0030] ⚠ Surpaiement détecté
[WARN] [SYNC-REGLEMENTS] [FAC-2026-0030]   Montant facture: 1500.00€
[WARN] [SYNC-REGLEMENTS] [FAC-2026-0030]   Montant encaissé: 1600.00€
[WARN] [SYNC-REGLEMENTS] [FAC-2026-0030]   Excédent: 100.00€ - Avoir à créer
```

#### 3.3.2 Anomalies métier (bloquantes)

```
[ERROR] [SYNC-ECRITURES] [FAC-2026-0047] ✗ Facture non créée - Montant invalide
[ERROR] [SYNC-ECRITURES] [FAC-2026-0047]   Montant HT: 0.00€ (minimum requis: 0.01€)
[ERROR] [SYNC-ECRITURES] [FAC-2026-0047]   Action requise: Corriger la facture dans ATHENEO

[ERROR] [SYNC-ECRITURES] [FAC-2026-0048] ✗ Facture non créée - Client obligatoire manquant
[ERROR] [SYNC-ECRITURES] [FAC-2026-0048]   NO_SOCIETE: null
[ERROR] [SYNC-ECRITURES] [FAC-2026-0048]   Action requise: Associer un client à la facture

[ERROR] [SYNC-ACHATS] [SUPP-INV-789] ✗ Facture non importée - Fournisseur non identifiable
[ERROR] [SYNC-ACHATS] [SUPP-INV-789]   Fournisseur Pennylane: supp_unknown
[ERROR] [SYNC-ACHATS] [SUPP-INV-789]   Aucune correspondance locale trouvée
[ERROR] [SYNC-ACHATS] [SUPP-INV-789]   Action requise: Créer le fournisseur manuellement ou vérifier le mapping
```

### 3.4 Logs d'anomalies techniques

#### 3.4.1 Erreurs réseau et API

```
[ERROR] [SYNC-ECRITURES] [FAC-2026-0049] ✗ Erreur API Pennylane
[ERROR] [SYNC-ECRITURES] [FAC-2026-0049]   Code HTTP: 422 (Unprocessable Entity)
[ERROR] [SYNC-ECRITURES] [FAC-2026-0049]   Message API: "invoice_number already exists"
[ERROR] [SYNC-ECRITURES] [FAC-2026-0049]   Facture marquée comme ALREADY_EXISTS

[ERROR] [SYNC-ECRITURES] ✗ Erreur de connexion API Pennylane
[ERROR] [SYNC-ECRITURES]   Type: Connection timeout
[ERROR] [SYNC-ECRITURES]   Tentative: 2/3
[ERROR] [SYNC-ECRITURES]   Prochaine tentative dans: 2 secondes

[WARN] [SYNC-ECRITURES] Circuit Breaker "pennylaneAPI" passé en état OPEN
[WARN] [SYNC-ECRITURES]   Raison: Taux d'échec > 50% (6/10 appels en erreur)
[WARN] [SYNC-ECRITURES]   Durée d'attente: 30 secondes
[WARN] [SYNC-ECRITURES]   Impact: Appels API temporairement bloqués
```

#### 3.4.2 Erreurs base de données

```
[ERROR] [SYNC-ECRITURES] ✗ Erreur SQL - Deadlock détecté
[ERROR] [SYNC-ECRITURES]   Procédure: SP_PENNYLANE_EXPORT_ECRITURES
[ERROR] [SYNC-ECRITURES]   Erreur SQL: 1205 - Transaction victime d'un verrou mortel
[ERROR] [SYNC-ECRITURES]   Tentative: 1/3
[ERROR] [SYNC-ECRITURES]   Nouvelle tentative dans: 100ms

[INFO] [SYNC-ECRITURES] Deadlock résolu après 2 tentatives
[INFO] [SYNC-ECRITURES] Durée de résolution: 350ms
```

#### 3.4.3 Erreurs de stockage documentaire

```
[WARN] [SYNC-ECRITURES] [FAC-2026-0050] ⚠ Document non disponible
[WARN] [SYNC-ECRITURES] [FAC-2026-0050]   Service WSDocument: Fichier non trouvé
[WARN] [SYNC-ECRITURES] [FAC-2026-0050]   Référence courrier: COU-2026-1234
[WARN] [SYNC-ECRITURES] [FAC-2026-0050]   Facture créée sans pièce jointe

[ERROR] [SYNC-ACHATS] [SUPP-INV-790] ✗ Échec téléchargement PDF
[ERROR] [SYNC-ACHATS] [SUPP-INV-790]   URL: https://files.pennylane.com/doc_xyz.pdf
[ERROR] [SYNC-ACHATS] [SUPP-INV-790]   Erreur: HTTP 403 (Accès refusé)
[ERROR] [SYNC-ACHATS] [SUPP-INV-790]   Facture importée sans document
```

---

## 4. Reformulation des messages existants

### 4.1 Messages actuels vs messages proposés

| Message actuel | Problème | Message proposé |
|----------------|----------|-----------------|
| "Ecriture 123 traitée" | Manque de contexte | "[SYNC-ECRITURES] [LOT-456] Écriture 123 traitée avec succès (facture FAC-2026-0042 créée)" |
| "Error" | Pas informatif | "[ERROR] [SYNC-ECRITURES] [FAC-2026-0042] Échec création facture: montant HT invalide (0€)" |
| "Invoice created" | Anglais, pas de détail | "[INFO] [SYNC-ECRITURES] [FAC-2026-0042] ✓ Facture créée dans Pennylane (ID: inv_abc123)" |
| "Customer not found" | Ambigu | "[WARN] [SYNC-ECRITURES] [FAC-2026-0042] Client non trouvé dans Pennylane - création automatique" |
| "Sync done" | Trop vague | "[INFO] [SYNC-ECRITURES] FIN - 25 factures traitées (23 succès, 2 ignorées) en 12.5 sec" |
| "HTTP 422" | Technique, pas exploitable | "[ERROR] [SYNC-ECRITURES] API Pennylane a rejeté la requête: champ 'amount' requis manquant" |
| "Deadlock" | Trop technique | "[WARN] [SYNC-ECRITURES] Conflit d'accès base de données - nouvelle tentative automatique (1/3)" |
| "Rate limited" | Jargon technique | "[INFO] [SYNC-ECRITURES] Ralentissement automatique: limite d'appels API atteinte (100/min)" |

### 4.2 Principes de reformulation appliqués

1. **Préfixe contextuel** : [NIVEAU] [FLUX] [RÉFÉRENCE]
2. **Langue française** : Messages en français pour les audiences francophones
3. **Symboles visuels** : ✓ (succès), ✗ (erreur), ⚠ (avertissement)
4. **Information actionnable** : Indiquer quoi faire en cas d'erreur
5. **Données métier** : Inclure les références factures, clients, montants
6. **Durées** : Mentionner les temps d'exécution pour diagnostic performance

---

## 5. Exemples de messages bien rédigés

### 5.1 Messages d'information (INFO)

```
✓ BON : [INFO] [SYNC-ECRITURES] [FAC-2026-0042] Facture créée dans Pennylane (ID: inv_abc123, montant: 1800.00€ TTC)

✗ MAUVAIS : "Invoice created"
```

```
✓ BON : [INFO] [SYNC-ECRITURES] Traitement lot LOT-456 terminé: 10 factures, 2 clients créés, durée 5.2 sec

✗ MAUVAIS : "Lot traité"
```

```
✓ BON : [INFO] [SYNC-ACHATS] 350 factures récupérées depuis Pennylane, 25 retenues après filtrage (statut: to_be_paid, catégorie: ACH)

✗ MAUVAIS : "Got 350 invoices"
```

### 5.2 Messages d'avertissement (WARN)

```
✓ BON : [WARN] [SYNC-ECRITURES] [FAC-2026-0043] ⚠ Document PDF indisponible (réf: COU-2026-789) - Facture créée sans pièce jointe

✗ MAUVAIS : "PDF not found"
```

```
✓ BON : [WARN] [SYNC-REGLEMENTS] [FAC-2026-0030] ⚠ Surpaiement détecté: 100.00€ d'excédent sur facture de 1500.00€ - Avoir à émettre

✗ MAUVAIS : "Overpaid invoice"
```

```
✓ BON : [WARN] [SYNC-ECRITURES] Limite d'appels API proche (90/100 par minute) - Ralentissement préventif activé

✗ MAUVAIS : "Rate limit warning"
```

### 5.3 Messages d'erreur (ERROR)

```
✓ BON : [ERROR] [SYNC-ECRITURES] [FAC-2026-0044] ✗ Facture rejetée par Pennylane
        Cause: Montant HT doit être supérieur à 0.01€
        Valeur actuelle: 0.00€
        Action: Corriger le montant HT dans ATHENEO puis relancer la synchronisation

✗ MAUVAIS : "Error 422"
```

```
✓ BON : [ERROR] [SYNC-ECRITURES] ✗ Connexion API Pennylane impossible après 3 tentatives
        Dernière erreur: Connection timeout (30s)
        Impact: 5 factures en attente de synchronisation
        Action: Vérifier la connectivité réseau et le statut de l'API Pennylane

✗ MAUVAIS : "Connection failed"
```

```
✓ BON : [ERROR] [SYNC-ACHATS] [SUPP-INV-567] ✗ Import facture impossible - Fournisseur non identifiable
        Fournisseur Pennylane: "SARL MARTIN" (ID: supp_xyz)
        SIRET: 98765432109876
        Aucune correspondance trouvée dans ATHENEO
        Action: Créer le fournisseur dans ATHENEO avec ce SIRET, ou corriger le SIRET dans Pennylane

✗ MAUVAIS : "Supplier null"
```

### 5.4 Messages de debug (DEBUG)

```
✓ BON : [DEBUG] [SYNC-ECRITURES] [FAC-2026-0042] Validation montants:
        HT: 1500.00€ ✓
        TVA: 300.00€ (20%) ✓
        TTC: 1800.00€ ✓
        Cohérence HT+TVA=TTC: ✓

✗ MAUVAIS : "amounts ok"
```

```
✓ BON : [DEBUG] [SYNC-ECRITURES] [FAC-2026-0042] Appel API Pennylane:
        Endpoint: POST /customer_invoices/import
        Durée: 1.234 sec
        Code retour: 201 Created
        ID créé: inv_abc123

✗ MAUVAIS : "API call done"
```

---

## 6. Matrice de visibilité des logs

### 6.1 Logs visibles par le Support

| Type de log | Niveau | Exemple | Utilité Support |
|-------------|--------|---------|-----------------|
| Début/fin de flux | INFO | "DÉMARRAGE - Synchronisation des écritures" | Savoir si le traitement s'est exécuté |
| Bilan de traitement | INFO | "25 factures traitées (23 succès, 2 erreurs)" | Vision synthétique du résultat |
| Erreurs fonctionnelles | ERROR | "Facture rejetée: montant invalide" | Identifier les corrections à faire |
| Avertissements métier | WARN | "Surpaiement détecté: avoir à créer" | Actions manuelles à planifier |
| Statistiques | INFO | "Durée: 12.5 sec, taux succès: 92%" | Monitoring de la performance |

**Recommandation** : Le support doit avoir accès aux niveaux **INFO**, **WARN** et **ERROR** avec un filtrage par flux (SYNC-ECRITURES, SYNC-ACHATS, etc.).

### 6.2 Logs visibles par le Fonctionnel

| Type de log | Niveau | Exemple | Utilité Fonctionnel |
|-------------|--------|---------|---------------------|
| Factures traitées | INFO | "Facture FAC-2026-0042 créée dans Pennylane" | Suivi des factures synchronisées |
| Anomalies de données | WARN | "SIRET invalide pour client DUPONT" | Qualité des données à corriger |
| Règlements | INFO | "Facture entièrement payée (1800€)" | Suivi des encaissements |
| Erreurs bloquantes | ERROR | "Client obligatoire manquant" | Corrections à apporter |

**Recommandation** : Le fonctionnel doit avoir un **tableau de bord simplifié** avec les logs **INFO** et **WARN** filtrés sur les références métier (factures, clients).

### 6.3 Logs visibles par les Développeurs

| Type de log | Niveau | Exemple | Utilité Développeur |
|-------------|--------|---------|---------------------|
| Détails techniques | DEBUG | "Appel API: POST /invoices, durée: 1.2s" | Diagnostic de performance |
| Stack traces | ERROR | "java.sql.SQLException: Deadlock..." | Analyse des bugs |
| États internes | TRACE | "Variable amount=1500.00, validated=true" | Debug fin |
| Métriques Resilience4j | DEBUG | "Circuit breaker: CLOSED, failures: 2/10" | Surveillance résilience |
| Requêtes/réponses HTTP | DEBUG | Trames JSON complètes | Diagnostic API |

**Recommandation** : Les développeurs doivent avoir accès à **tous les niveaux** de logs avec la possibilité de filtrer par classe Java et par période.

### 6.4 Synthèse par audience

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    MATRICE DE VISIBILITÉ DES LOGS                       │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Niveau de log    │ Support │ Fonctionnel │ Développeur                │
│  ─────────────────┼─────────┼─────────────┼─────────────               │
│  FATAL            │    ✓    │      ✓      │      ✓                     │
│  ERROR            │    ✓    │      ✓      │      ✓                     │
│  WARN             │    ✓    │      ✓      │      ✓                     │
│  INFO             │    ✓    │      ✓      │      ✓                     │
│  DEBUG            │    ✗    │      ✗      │      ✓                     │
│  TRACE            │    ✗    │      ✗      │      ✓                     │
│                                                                         │
│  Informations     │ Support │ Fonctionnel │ Développeur                │
│  ─────────────────┼─────────┼─────────────┼─────────────               │
│  Références métier│    ✓    │      ✓      │      ✓                     │
│  Montants         │    ✓    │      ✓      │      ✓                     │
│  Stack traces     │    ✗    │      ✗      │      ✓                     │
│  Trames HTTP      │    ✗    │      ✗      │      ✓                     │
│  Durées exec.     │    ✓    │      ✗      │      ✓                     │
│  Métriques tech.  │    ✗    │      ✗      │      ✓                     │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Recommandations de mise en œuvre

### 7.1 Actions prioritaires (Court terme)

| Action | Description | Effort | Impact |
|--------|-------------|--------|--------|
| **Ajouter les logs début/fin de flux** | Encadrer chaque flux avec logs de démarrage et bilan | Faible | Élevé |
| **Implémenter le Correlation ID** | Identifiant unique par exécution de flux | Faible | Élevé |
| **Reformuler les messages d'erreur** | Rendre les erreurs compréhensibles et actionnables | Moyen | Élevé |
| **Ajouter les statistiques de bilan** | Compteurs de succès/erreurs en fin de flux | Faible | Moyen |

### 7.2 Actions secondaires (Moyen terme)

| Action | Description | Effort | Impact |
|--------|-------------|--------|--------|
| **Créer un tableau de bord fonctionnel** | Interface simplifiée pour les non-techniques | Moyen | Élevé |
| **Ajouter les logs d'étapes intermédiaires** | Détailler le traitement de chaque entité | Moyen | Moyen |
| **Implémenter le filtrage par audience** | Vues différenciées Support/Fonctionnel/Dev | Moyen | Moyen |
| **Traduire tous les messages en français** | Uniformiser la langue des logs | Faible | Moyen |

### 7.3 Actions d'amélioration continue (Long terme)

| Action | Description | Effort | Impact |
|--------|-------------|--------|--------|
| **Intégrer des alertes automatiques** | Notification en cas d'erreurs critiques | Élevé | Élevé |
| **Créer des rapports périodiques** | Synthèse quotidienne/hebdomadaire des traitements | Moyen | Moyen |
| **Implémenter le log structuré (JSON)** | Format exploitable par outils d'analyse | Élevé | Moyen |
| **Ajouter des métriques Prometheus** | Monitoring temps réel des KPIs | Élevé | Moyen |

### 7.4 Convention de nommage recommandée

```
Format standard:
[NIVEAU] [FLUX] [RÉFÉRENCE] Message descriptif

Où:
- NIVEAU: TRACE, DEBUG, INFO, WARN, ERROR, FATAL
- FLUX: SYNC-ECRITURES, SYNC-ACHATS, SYNC-REGLEMENTS, SYNC-BAP
- RÉFÉRENCE: Identifiant métier (FAC-xxxx, LOT-xxxx, SUPP-INV-xxxx)

Exemples:
[INFO] [SYNC-ECRITURES] [FAC-2026-0042] Facture créée avec succès
[ERROR] [SYNC-ACHATS] [SUPP-INV-789] Fournisseur non identifiable
[WARN] [SYNC-REGLEMENTS] [FAC-2026-0030] Surpaiement détecté
```

### 7.5 Bonnes pratiques à adopter

1. **Toujours inclure un contexte métier** : Numéro de facture, nom du client, montant
2. **Éviter les messages génériques** : Proscrire "Error", "Success", "Done"
3. **Proposer une action en cas d'erreur** : Que doit faire l'utilisateur ?
4. **Mesurer les durées** : Facilite l'identification des problèmes de performance
5. **Utiliser des symboles visuels** : ✓, ✗, ⚠ pour une lecture rapide
6. **Séparer visuellement les sections** : Lignes de séparation pour les débuts/fins de flux
7. **Être cohérent** : Même format pour tous les logs du même type

---

## Annexes

### Annexe A : Modèle de log structuré (JSON)

Pour une exploitation avancée (Elasticsearch, Splunk, etc.), voici le format JSON recommandé :

```json
{
  "timestamp": "2026-01-23T14:30:52.123Z",
  "level": "INFO",
  "correlationId": "SYNC-ECR-20260123-143052-A7B2",
  "flux": "SYNC-ECRITURES",
  "reference": "FAC-2026-0042",
  "message": "Facture créée dans Pennylane",
  "details": {
    "pennylaneId": "inv_abc123",
    "montantHT": 1500.00,
    "montantTTC": 1800.00,
    "client": "ENTREPRISE DUPONT",
    "siret": "12345678901234"
  },
  "duration_ms": 2340,
  "source": {
    "class": "AccountingService",
    "method": "syncEcriture"
  }
}
```

### Annexe B : Checklist de validation des logs

Avant de valider un nouveau log, vérifier :

- [ ] Le message est-il compréhensible par un fonctionnel ?
- [ ] Le niveau de log est-il approprié (INFO/WARN/ERROR) ?
- [ ] Le flux est-il identifié (SYNC-ECRITURES, etc.) ?
- [ ] La référence métier est-elle présente (facture, client, etc.) ?
- [ ] En cas d'erreur, l'action corrective est-elle indiquée ?
- [ ] Le message est-il en français ?
- [ ] Les données sensibles sont-elles masquées si nécessaire ?

---

*Document rédigé dans le cadre de l'amélioration continue de l'interface PENNYLANE*
*Pour toute question ou suggestion, contacter l'équipe technique.*
