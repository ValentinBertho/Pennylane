# Documentation Fonctionnelle - Interface PENNYLANE

**Version** : 2.0.4
**Date de rédaction** : Janvier 2026
**Public cible** : MOA, Consultants fonctionnels, Chefs de projet, Support N2/N3

---

## Table des matières

1. [Contexte et objectifs](#1-contexte-et-objectifs)
2. [Architecture fonctionnelle globale](#2-architecture-fonctionnelle-globale)
3. [Flux fonctionnels](#3-flux-fonctionnels)
4. [Règles de gestion](#4-règles-de-gestion)
5. [Paramétrage fonctionnel](#5-paramétrage-fonctionnel)
6. [Contrôles fonctionnels](#6-contrôles-fonctionnels)
7. [Reprise et relance](#7-reprise-et-relance)
8. [Dépendances externes](#8-dépendances-externes)
9. [Glossaire](#9-glossaire)

---

## 1. Contexte et objectifs

### 1.1 Présentation générale

L'interface PENNYLANE est une solution d'intégration qui assure la **synchronisation bidirectionnelle** des données comptables entre l'ERP interne **ATHENEO** et le logiciel de comptabilité cloud **Pennylane**.

Cette interface permet d'automatiser les échanges de données comptables, réduisant ainsi les ressaisies manuelles et les risques d'erreurs tout en garantissant la cohérence des informations entre les deux systèmes.

### 1.2 Objectifs métier

| Objectif | Description |
|----------|-------------|
| **Automatisation comptable** | Éliminer les ressaisies manuelles des factures et écritures entre les systèmes |
| **Cohérence des données** | Garantir que les informations comptables sont identiques dans ATHENEO et Pennylane |
| **Traçabilité complète** | Assurer un suivi détaillé de chaque opération de synchronisation |
| **Gestion multi-site** | Permettre une configuration différenciée par site/établissement |
| **Suivi des paiements** | Synchroniser les règlements pour un suivi en temps réel des encaissements |

### 1.3 Périmètre fonctionnel

L'interface couvre les domaines fonctionnels suivants :

- **Factures de vente** : Export des factures clients vers Pennylane
- **Factures d'achat** : Import des factures fournisseurs depuis Pennylane
- **Tiers** : Synchronisation des clients et fournisseurs
- **Produits** : Création automatique des articles dans Pennylane
- **Comptes comptables** : Création automatique des comptes manquants
- **Règlements** : Suivi et mise à jour des statuts de paiement
- **Documents** : Upload des pièces justificatives (PDF)

### 1.4 Bénéfices attendus

- **Gain de temps** : Réduction significative des opérations manuelles de saisie
- **Fiabilité** : Diminution des erreurs de saisie et des écarts inter-systèmes
- **Réactivité** : Mise à jour quasi temps réel des données comptables
- **Visibilité** : Tableau de bord et logs pour suivre l'activité de l'interface
- **Conformité** : Archivage automatique des pièces justificatives

---

## 2. Architecture fonctionnelle globale

### 2.1 Vue d'ensemble

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        ARCHITECTURE FONCTIONNELLE                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   ┌──────────────┐                              ┌──────────────────┐    │
│   │   ATHENEO    │                              │    PENNYLANE     │    │
│   │    (ERP)     │                              │   (Comptabilité) │    │
│   └──────┬───────┘                              └────────┬─────────┘    │
│          │                                               │              │
│          │         ┌─────────────────────┐               │              │
│          │         │     INTERFACE       │               │              │
│          ├────────►│    PENNYLANE        │◄──────────────┤              │
│          │         │                     │               │              │
│          │         │  • Synchronisation  │               │              │
│          │         │  • Transformation   │               │              │
│          │         │  • Validation       │               │              │
│          │         │  • Traçabilité      │               │              │
│          │         └─────────────────────┘               │              │
│          │                                               │              │
│   ┌──────▼───────────────────────────────────────────────▼──────┐      │
│   │                    FLUX DE DONNÉES                           │      │
│   │                                                              │      │
│   │  SORTANT (ATHENEO → Pennylane)                              │      │
│   │  ─────────────────────────────                              │      │
│   │  • Factures de vente                                        │      │
│   │  • Clients                                                  │      │
│   │  • Produits                                                 │      │
│   │  • Documents PDF                                            │      │
│   │                                                              │      │
│   │  ENTRANT (Pennylane → ATHENEO)                              │      │
│   │  ─────────────────────────────                              │      │
│   │  • Factures d'achat                                         │      │
│   │  • Fournisseurs                                             │      │
│   │  • Règlements                                               │      │
│   │  • Documents PDF                                            │      │
│   └──────────────────────────────────────────────────────────────┘      │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Composants fonctionnels

| Composant | Rôle fonctionnel |
|-----------|------------------|
| **Moteur de synchronisation** | Orchestration des échanges de données selon les planifications définies |
| **Module de transformation** | Conversion des données entre les formats ATHENEO et Pennylane |
| **Module de validation** | Vérification des règles métier avant envoi/réception |
| **Gestionnaire de documents** | Upload et téléchargement des pièces justificatives |
| **Système de traçabilité** | Enregistrement de toutes les opérations et anomalies |
| **Tableau de bord** | Visualisation de l'activité et des indicateurs clés |

### 2.3 Modes de fonctionnement

L'interface fonctionne selon deux modes complémentaires :

1. **Mode automatique (batch)** : Traitements planifiés s'exécutant à intervalles réguliers
2. **Mode manuel** : Possibilité de déclencher une synchronisation à la demande

### 2.4 Gestion multi-site

L'interface supporte une **configuration par site** permettant :
- D'activer ou désactiver la synchronisation par établissement
- D'utiliser des tokens d'authentification différents par site
- De choisir les flux à activer (ventes uniquement, achats uniquement, ou les deux)

---

## 3. Flux fonctionnels

### 3.1 Vue synthétique des flux

| N° | Nom du flux | Direction | Déclencheur | Fréquence par défaut |
|----|-------------|-----------|-------------|---------------------|
| F1 | Synchronisation des écritures comptables | ATHENEO → Pennylane | Automatique | Toutes les 10 secondes |
| F2 | Mise à jour du statut "Bon À Payer" | ATHENEO → Pennylane | Automatique | Désactivé par défaut |
| F3 | Synchronisation des factures fournisseurs | Pennylane → ATHENEO | Automatique | Désactivé par défaut |
| F4 | Mise à jour des règlements | Pennylane → ATHENEO | Automatique | Désactivé par défaut |

---

### 3.2 FLUX F1 : Synchronisation des écritures comptables

#### 3.2.1 Description fonctionnelle

Ce flux permet d'exporter les **factures de vente** et leurs éléments associés (clients, produits, documents) depuis ATHENEO vers Pennylane. C'est le flux principal de l'interface.

#### 3.2.2 Déclencheur

| Type | Valeur | Modifiable |
|------|--------|------------|
| Automatique (planifié) | Toutes les 10 secondes | Oui, via paramétrage |

#### 3.2.3 Données d'entrée

| Donnée | Source | Description |
|--------|--------|-------------|
| Lot d'écritures | Table LOT_ECRITURE | Regroupement des écritures à traiter |
| Factures de vente | Table V_FACTURE | Détail des factures clients |
| Lignes de facture | Tables ECRITURE_PIECE, ECRITURE_LIGNE | Lignes comptables de chaque facture |
| Tiers (clients) | Table SOCIETE | Informations sur les clients |
| Produits | Table PRODUITS | Articles et services facturés |
| Documents | Table COURRIER | Fichiers PDF des factures |
| Configuration site | Table T_SITE | Token Pennylane et options d'activation |

#### 3.2.4 Traitements fonctionnels

```
┌─────────────────────────────────────────────────────────────────┐
│              PROCESSUS DE SYNCHRONISATION F1                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. RÉCUPÉRATION DES DONNÉES                                    │
│     └─► Lecture des lots d'écritures en attente de traitement   │
│                                                                  │
│  2. POUR CHAQUE LOT D'ÉCRITURE                                  │
│     │                                                            │
│     ├─► 2.1 Récupération du site concerné                       │
│     │       └─► Vérification que le site est actif              │
│     │                                                            │
│     ├─► 2.2 Traitement des produits                             │
│     │       └─► Création/mise à jour dans Pennylane             │
│     │                                                            │
│     ├─► 2.3 Traitement du client                                │
│     │       └─► Création/mise à jour dans Pennylane             │
│     │                                                            │
│     ├─► 2.4 Préparation de la facture                           │
│     │       ├─► Transformation au format Pennylane              │
│     │       └─► Vérification des règles de validation           │
│     │                                                            │
│     ├─► 2.5 Upload du document PDF                              │
│     │       └─► Récupération depuis stockage documentaire       │
│     │                                                            │
│     └─► 2.6 Envoi de la facture complète                        │
│             └─► Création dans Pennylane avec pièce jointe       │
│                                                                  │
│  3. MISE À JOUR DU STATUT                                       │
│     └─► Marquage du lot comme traité                            │
│                                                                  │
│  4. TRAÇABILITÉ                                                 │
│     └─► Enregistrement du résultat dans les logs                │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**Détail des étapes :**

1. **Récupération des lots** : Le système identifie les lots d'écritures comptables non encore traités
2. **Vérification du site** : Contrôle que le site est configuré et actif pour Pennylane
3. **Traitement des produits** : Pour chaque article facturé, création ou mise à jour dans Pennylane
4. **Traitement du client** : Vérification de l'existence du client, création si nécessaire
5. **Préparation de la facture** : Transformation des données au format attendu par Pennylane
6. **Upload du document** : Si un PDF est disponible, téléversement vers Pennylane
7. **Envoi de la facture** : Création de la facture dans Pennylane avec toutes ses données
8. **Mise à jour du statut** : Marquage du lot comme traité pour éviter les doublons

#### 3.2.5 Données de sortie

| Donnée | Destination | Description |
|--------|-------------|-------------|
| Facture créée | Pennylane | Facture client avec toutes ses lignes |
| Client créé/mis à jour | Pennylane | Fiche client synchronisée |
| Produits créés/mis à jour | Pennylane | Articles référencés dans la facture |
| Comptes comptables créés | Pennylane | Comptes créés automatiquement si manquants |
| Document PDF attaché | Pennylane | Pièce justificative de la facture |
| Log de traitement | Table LOG | Trace complète de l'opération |
| Mise à jour forum | Table FORUM | Historique métier du traitement |

#### 3.2.6 Cas nominal

| Étape | Résultat attendu |
|-------|------------------|
| Lecture du lot | Lot récupéré avec toutes ses écritures |
| Vérification site | Site actif avec token valide |
| Création produits | Produits créés ou déjà existants dans Pennylane |
| Création client | Client créé ou déjà existant dans Pennylane |
| Validation facture | Montants cohérents, dates valides |
| Upload PDF | Document téléversé avec succès |
| Création facture | Facture créée avec ID Pennylane retourné |
| Finalisation | Lot marqué comme traité, log de succès enregistré |

#### 3.2.7 Cas d'erreur

| Erreur | Comportement | Impact |
|--------|--------------|--------|
| Site inactif | Lot ignoré | Factures non synchronisées pour ce site |
| Token invalide | Erreur d'authentification | Arrêt du traitement, notification en log |
| Facture déjà existante | Détection de doublon | Facture ignorée, marquée comme "ALREADY_EXISTS" |
| Client introuvable | Création automatique | Nouveau client créé dans Pennylane |
| Produit introuvable | Création automatique | Nouveau produit créé dans Pennylane |
| Compte comptable manquant | Création automatique | Nouveau compte créé dans Pennylane |
| Montant invalide | Rejet de la facture | Log d'erreur, facture non créée |
| Document PDF indisponible | Poursuite sans PDF | Facture créée sans pièce jointe |
| Erreur réseau | Retry automatique (3 tentatives) | Traitement différé puis erreur si échec |
| Surcharge API Pennylane | Rate limiting | Ralentissement automatique des appels |

#### 3.2.8 Impacts métier et comptables

| Impact | Description |
|--------|-------------|
| **Création de facture** | La facture apparaît dans Pennylane avec son numéro d'origine |
| **Référencement croisé** | Le numéro de facture ATHENEO est conservé comme référence externe |
| **Archivage documentaire** | Le PDF original est archivé dans Pennylane |
| **Comptes automatiques** | Les comptes comptables manquants sont créés automatiquement |
| **Données client** | Les informations client (SIRET, adresse, contact) sont synchronisées |

---

### 3.3 FLUX F2 : Mise à jour du statut "Bon À Payer"

#### 3.3.1 Description fonctionnelle

Ce flux permet de mettre à jour le statut de paiement des factures fournisseurs pour les passer en "Bon À Payer" (BAP) dans Pennylane.

#### 3.3.2 Déclencheur

| Type | Valeur | Modifiable |
|------|--------|------------|
| Automatique (planifié) | Désactivé par défaut | Oui, via paramétrage |

#### 3.3.3 Données d'entrée

| Donnée | Source | Description |
|--------|--------|-------------|
| Factures à valider | Table A_FACTURE | Factures marquées pour validation BAP |
| Configuration site | Table T_SITE | Token Pennylane |

#### 3.3.4 Traitements fonctionnels

1. **Identification** : Récupération des factures d'achat marquées pour validation BAP
2. **Récupération Pennylane** : Lecture de la facture dans Pennylane pour vérifier son état actuel
3. **Mise à jour statut** : Passage du statut au "to_be_paid" (bon à payer)
4. **Enregistrement** : Mise à jour du forum avec le résultat

#### 3.3.5 Données de sortie

| Donnée | Destination | Description |
|--------|-------------|-------------|
| Statut mis à jour | Pennylane | Facture passée en "to_be_paid" |
| Log de traitement | Table LOG | Trace de l'opération |
| Mise à jour forum | Table FORUM | Historique métier |

#### 3.3.6 Cas nominal

La facture est correctement identifiée dans Pennylane et son statut est mis à jour vers "to_be_paid".

#### 3.3.7 Cas d'erreur

| Erreur | Comportement | Impact |
|--------|--------------|--------|
| Facture non trouvée | Log d'erreur | Facture non mise à jour |
| Facture déjà réglée | Ignorée | Pas d'erreur, traitement continue |
| Statut incompatible | Log d'avertissement | Facture non mise à jour |

#### 3.3.8 Impacts métier et comptables

- La facture devient éligible au paiement dans le circuit de validation Pennylane
- Le workflow de paiement peut se poursuivre côté comptabilité

---

### 3.4 FLUX F3 : Synchronisation des factures fournisseurs

#### 3.4.1 Description fonctionnelle

Ce flux permet d'importer les **factures fournisseurs** depuis Pennylane vers ATHENEO. Il récupère également les informations fournisseurs et les documents PDF associés.

#### 3.4.2 Déclencheur

| Type | Valeur | Modifiable |
|------|--------|------------|
| Automatique (planifié) | Désactivé par défaut | Oui, via paramétrage |

#### 3.4.3 Données d'entrée

| Donnée | Source | Description |
|--------|--------|-------------|
| Factures fournisseurs | API Pennylane | Factures d'achat à importer |
| Fournisseurs | API Pennylane | Informations sur les fournisseurs |
| Catégories | API Pennylane | Classification des factures |
| Documents PDF | API Pennylane | Pièces justificatives |
| Configuration filtrage | application.yml | Critères de sélection des factures |

#### 3.4.4 Traitements fonctionnels

```
┌─────────────────────────────────────────────────────────────────┐
│         PROCESSUS D'IMPORT FACTURES FOURNISSEURS F3             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. CHARGEMENT DES CATÉGORIES                                   │
│     └─► Mise en cache des catégories Pennylane                  │
│                                                                  │
│  2. RÉCUPÉRATION DES FACTURES                                   │
│     └─► Appel API avec filtres (statut, catégorie, période)     │
│                                                                  │
│  3. FILTRAGE DES FACTURES                                       │
│     ├─► Par statut de paiement (to_be_processed, to_be_paid)    │
│     ├─► Par catégorie (ACH par défaut)                          │
│     └─► Par période (360 jours en arrière par défaut)           │
│                                                                  │
│  4. POUR CHAQUE FACTURE RETENUE                                 │
│     │                                                            │
│     ├─► 4.1 Récupération du fournisseur complet                 │
│     │                                                            │
│     ├─► 4.2 Vérification/Création du fournisseur local          │
│     │                                                            │
│     ├─► 4.3 Enregistrement de la facture en base                │
│     │                                                            │
│     └─► 4.4 Téléchargement et stockage du PDF                   │
│                                                                  │
│  5. TRAÇABILITÉ                                                 │
│     └─► Log du nombre de factures traitées                      │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

#### 3.4.5 Données de sortie

| Donnée | Destination | Description |
|--------|-------------|-------------|
| Factures créées | Table A_FACTURE | Factures fournisseurs importées |
| Fournisseurs créés | Table SOCIETE | Nouveaux fournisseurs |
| Documents PDF | Stockage documentaire | Pièces justificatives archivées |
| Log de traitement | Table LOG | Statistiques d'import |

#### 3.4.6 Cas nominal

| Étape | Résultat attendu |
|-------|------------------|
| Récupération | X factures brutes récupérées |
| Filtrage | Y factures retenues après filtrage |
| Traitement | Y factures enregistrées en base |
| Documents | PDF téléchargés et archivés |

#### 3.4.7 Cas d'erreur

| Erreur | Comportement | Impact |
|--------|--------------|--------|
| Fournisseur non identifiable | Exception levée | Facture non importée, log d'erreur |
| Catégories non correspondantes | Arrêt du cron | Alerte pour vérification configuration |
| Document PDF indisponible | Poursuite sans PDF | Facture importée sans pièce jointe |
| Facture déjà existante | Ignorée | Pas de doublon créé |

#### 3.4.8 Impacts métier et comptables

- Les factures fournisseurs sont disponibles dans ATHENEO pour validation
- Les fournisseurs sont automatiquement créés avec leurs informations complètes
- Les pièces justificatives sont archivées conformément aux obligations légales

---

### 3.5 FLUX F4 : Mise à jour des règlements

#### 3.5.1 Description fonctionnelle

Ce flux synchronise les **informations de paiement** depuis Pennylane vers ATHENEO, permettant de suivre l'état de règlement des factures clients.

#### 3.5.2 Déclencheur

| Type | Valeur | Modifiable |
|------|--------|------------|
| Automatique (planifié) | Désactivé par défaut | Oui, via paramétrage |

#### 3.5.3 Données d'entrée

| Donnée | Source | Description |
|--------|--------|-------------|
| Factures clients | Table V_FACTURE | Factures dont le règlement doit être vérifié |
| Transactions | API Pennylane | Mouvements de paiement |
| Statuts de paiement | API Pennylane | État courant du règlement |

#### 3.5.4 Traitements fonctionnels

1. **Identification** : Récupération des factures nécessitant une mise à jour
2. **Lecture Pennylane** : Pour chaque facture, récupération des transactions de paiement
3. **Calcul du statut** : Détermination du statut de paiement selon les montants
4. **Enregistrement** : Mise à jour des règlements en base locale

#### 3.5.5 Calcul du statut de paiement

| Statut | Condition | Description |
|--------|-----------|-------------|
| **FULLY_PAID** | Reste à payer ≈ 0€ (± 0.01€) | Facture totalement réglée |
| **PARTIALLY_PAID** | 0 < Reste à payer < Montant total | Paiement partiel reçu |
| **TO_BE_PAID** | Reste à payer ≈ Montant total | Aucun paiement reçu |
| **OVERPAID** | Reste à payer < 0 | Surpaiement détecté (avoir à créer) |

#### 3.5.6 Données de sortie

| Donnée | Destination | Description |
|--------|-------------|-------------|
| Règlements créés | Table REGLEMENT | Historique des paiements |
| Statut mis à jour | Table V_FACTURE | Nouvel état de paiement |
| Log de traitement | Table LOG | Trace des opérations |

#### 3.5.7 Cas d'erreur

| Erreur | Comportement | Impact |
|--------|--------------|--------|
| Facture non trouvée dans Pennylane | Log d'avertissement | Règlement non mis à jour |
| Surpaiement détecté | Log d'avertissement spécifique | Signalement pour création d'avoir |
| Montant incohérent | Log d'erreur | Statut INVALID_AMOUNT |

#### 3.5.8 Impacts métier et comptables

- Visibilité en temps réel sur l'état des encaissements
- Détection automatique des surpaiements nécessitant un avoir
- Suivi du DSO (Days Sales Outstanding) facilité

---

## 4. Règles de gestion

### 4.1 Règles de validation des données

#### 4.1.1 Validation des montants

| Règle | Description | Seuils |
|-------|-------------|--------|
| **Montant minimum** | Tout montant doit être supérieur au seuil minimum | > 0.01€ |
| **Montant maximum** | Tout montant doit être inférieur au seuil maximum | < 10 000 000€ |
| **Positivité** | Les montants HT, TVA et TTC doivent être positifs | ≥ 0 |
| **Cohérence HT/TVA/TTC** | HT + TVA doit être égal à TTC | Tolérance ± 0.02€ |

#### 4.1.2 Validation des dates

| Règle | Description | Seuils |
|-------|-------------|--------|
| **Date facture passée** | La date de facture ne peut pas être dans le futur | ≤ Date du jour |
| **Ancienneté facture** | La date de facture ne peut pas être trop ancienne | ≤ 10 ans |
| **Date d'échéance** | L'échéance doit être postérieure ou égale à la date facture | ≥ Date facture |
| **Échéance raisonnable** | L'échéance ne peut pas être trop éloignée | ≤ 10 ans |

#### 4.1.3 Validation des identifiants

| Règle | Description | Format |
|-------|-------------|--------|
| **SIRET** | Le SIRET doit être au format standard | 14 chiffres + clé de contrôle |
| **SIREN** | Le SIREN doit être au format standard | 9 chiffres |
| **Email** | L'adresse email doit être valide | Format standard email |

### 4.2 Règles de gestion des doublons

| Situation | Règle appliquée | Résultat |
|-----------|-----------------|----------|
| Facture avec même numéro existe | Détection préalable | Marquage "ALREADY_EXISTS", pas de création |
| Client avec même identifiant existe | Mise à jour des données | Client existant mis à jour |
| Produit avec même référence existe | Mise à jour des données | Produit existant mis à jour |

### 4.3 Règles de création automatique

| Entité | Condition de création | Données utilisées |
|--------|----------------------|-------------------|
| **Compte comptable** | Compte référencé mais inexistant dans Pennylane | Numéro et libellé du compte |
| **Client** | Client référencé mais inexistant dans Pennylane | SIRET, raison sociale, adresse, contact |
| **Fournisseur** | Fournisseur référencé mais inexistant localement | Données Pennylane complètes |
| **Produit** | Produit facturé mais inexistant dans Pennylane | Référence, libellé, catégorie |

### 4.4 Règles de filtrage (factures fournisseurs)

| Critère | Valeur par défaut | Description |
|---------|-------------------|-------------|
| **Statuts de paiement** | to_be_processed, to_be_paid | Seules les factures non payées |
| **Catégories** | ACH | Uniquement les achats |
| **Période** | 360 jours | Factures des 12 derniers mois |

### 4.5 Règles de retry et résilience

| Situation | Comportement | Paramètres |
|-----------|--------------|------------|
| **Erreur réseau** | Retry automatique | 3 tentatives, délai exponentiel |
| **Surcharge API** | Rate limiting | Max 100 appels/minute |
| **Timeout** | Abandon avec log | 30 secondes max par appel |
| **Deadlock SQL** | Retry automatique | 3 tentatives, délai 100-400ms |

---

## 5. Paramétrage fonctionnel

### 5.1 Localisation du paramétrage

Le paramétrage de l'interface est réparti sur plusieurs emplacements :

| Emplacement | Type de paramétrage | Qui peut modifier |
|-------------|---------------------|-------------------|
| **Fichier application.yml** | Configuration technique et planification | Équipe technique / DevOps |
| **Table T_SITE** | Configuration par site (tokens, activation) | Administrateur fonctionnel |
| **Table PENNYLANE_DEFAULT_VALUES** | Valeurs par défaut métier | Administrateur fonctionnel |

### 5.2 Paramétrage de la planification (Crons)

#### 5.2.1 Description des paramètres

| Paramètre | Description | Valeur par défaut |
|-----------|-------------|-------------------|
| **cron.Entries** | Fréquence de synchronisation des écritures | */10 * * * * * (10 secondes) |
| **cron.Purchases** | Fréquence de synchronisation des achats | - (désactivé) |
| **cron.UpdateSale** | Fréquence de mise à jour BAP | - (désactivé) |
| **cron.UpdatePurchaseReglement** | Fréquence de mise à jour des règlements | - (désactivé) |
| **cron.PurgeLog** | Fréquence de purge des logs | - (désactivé) |

#### 5.2.2 Format des expressions cron

```
Format : secondes minutes heures jour_mois mois jour_semaine

Exemples :
- "*/10 * * * * *"  → Toutes les 10 secondes
- "0 */5 * * * *"   → Toutes les 5 minutes
- "0 0 * * * *"     → Toutes les heures
- "0 0 6 * * *"     → Tous les jours à 6h00
- "-"               → Désactivé
```

#### 5.2.3 Impacts d'une modification

| Modification | Impact | Recommandation |
|--------------|--------|----------------|
| Augmenter la fréquence | Plus de données en temps réel, charge accrue | Surveiller les performances |
| Diminuer la fréquence | Moins de charge, données moins fraîches | Acceptable pour flux secondaires |
| Désactiver un cron | Flux complètement arrêté | Prévoir un mode de reprise |

### 5.3 Paramétrage du filtrage des factures

#### 5.3.1 Description des paramètres

| Paramètre | Description | Valeur par défaut |
|-----------|-------------|-------------------|
| **facture.statusAFiltrer** | Statuts de paiement à inclure | to_be_processed, to_be_paid |
| **facture.categoriesAFiltrer** | Catégories de factures à inclure | ACH |
| **facture.daysBackward** | Nombre de jours à récupérer en arrière | 360 |
| **facture.lastInsertPurchases** | Date de dernière synchronisation | Variable |

#### 5.3.2 Impacts d'une modification

| Modification | Impact | Recommandation |
|--------------|--------|----------------|
| Ajouter des statuts | Plus de factures importées | Vérifier la pertinence métier |
| Ajouter des catégories | Plus de factures importées | S'assurer du paramétrage associé |
| Augmenter daysBackward | Historique plus large, temps de traitement allongé | Utiliser avec parcimonie |
| Modifier lastInsertPurchases | Reprendre depuis une date antérieure | Utile pour rattrapage |

### 5.4 Paramétrage par site (Table T_SITE)

#### 5.4.1 Description des paramètres

| Champ | Description | Obligatoire |
|-------|-------------|-------------|
| **CODE** | Code unique du site | Oui |
| **LIBELLE** | Libellé descriptif du site | Oui |
| **PENNYLANE_TOKEN** | Token d'authentification API Pennylane | Oui si activé |
| **PENNYLANE_ACTIF** | Activer la synchronisation des écritures | Oui |
| **PENNYLANE_ACHAT** | Activer la synchronisation des achats | Oui |

#### 5.4.2 Impacts d'une modification

| Modification | Impact |
|--------------|--------|
| Changer le token | Nouvelle authentification vers Pennylane |
| Désactiver PENNYLANE_ACTIF | Arrêt de la synchronisation des ventes pour ce site |
| Désactiver PENNYLANE_ACHAT | Arrêt de la synchronisation des achats pour ce site |

### 5.5 Valeurs par défaut métier (PENNYLANE_DEFAULT_VALUES)

#### 5.5.1 Description des paramètres

| Paramètre | Description | Valeur par défaut |
|-----------|-------------|-------------------|
| **COD_COM** | Code commande par défaut | SIE_Cpta |
| **COD_SERVICE** | Service par défaut | ADM |
| **COD_DIRECTION** | Direction par défaut | ADM |
| **COD_AGENCE** | Agence par défaut | DG |
| **COD_TYPE** | Type de document par défaut | FAC |
| **COD_ETAT** | État par défaut | 3 |
| **COD_STATUT** | Statut par défaut | 1 |
| **NO_INTERLO** | Interlocuteur par défaut | -1 |

Ces valeurs sont utilisées lors de la création de factures d'achat importées depuis Pennylane.

---

## 6. Contrôles fonctionnels

### 6.1 Contrôles à l'entrée

| Contrôle | Description | Action si KO |
|----------|-------------|--------------|
| **Site actif** | Vérification que le site est configuré et actif | Lot ignoré |
| **Token valide** | Vérification que le token Pennylane est fonctionnel | Erreur, arrêt du traitement |
| **Données obligatoires** | Vérification de la présence des champs requis | Rejet avec log d'erreur |
| **Format des montants** | Vérification du format numérique | Rejet avec log d'erreur |
| **Cohérence des dates** | Vérification de la logique des dates | Avertissement ou rejet |

### 6.2 Contrôles métier

| Contrôle | Description | Action si KO |
|----------|-------------|--------------|
| **Doublon de facture** | Vérification de l'unicité du numéro de facture | Marquage ALREADY_EXISTS |
| **Cohérence HT/TVA/TTC** | Vérification mathématique des montants | Avertissement si écart |
| **Validité SIRET** | Vérification du format et de la clé de contrôle | Avertissement |
| **Catégories cohérentes** | Vérification de la correspondance des catégories | Alerte si divergence |

### 6.3 Contrôles de sortie

| Contrôle | Description | Action si KO |
|----------|-------------|--------------|
| **Création réussie** | Vérification du retour API Pennylane | Log d'erreur si échec |
| **ID retourné** | Vérification de la présence de l'identifiant Pennylane | Log d'avertissement |
| **Document attaché** | Vérification de l'attachement du PDF | Log si échec (non bloquant) |

### 6.4 Tableau de bord des contrôles

L'interface expose un tableau de bord permettant de visualiser :

- Nombre de traitements réussis / en erreur par période
- Liste des erreurs récentes avec détails
- Temps moyen de traitement
- Statistiques par site
- Logs des opérations lentes (> 1 seconde)

---

## 7. Reprise et relance

### 7.1 Scénarios de reprise

#### 7.1.1 Reprise après erreur ponctuelle

| Situation | Procédure |
|-----------|-----------|
| Erreur réseau temporaire | Automatique : retry intégré (3 tentatives) |
| Timeout API | Automatique : retry avec délai exponentiel |
| Deadlock SQL | Automatique : retry avec délai |

#### 7.1.2 Reprise après incident majeur

| Situation | Procédure |
|-----------|-----------|
| Indisponibilité prolongée de Pennylane | Attendre le rétablissement, les lots non traités seront repris automatiquement |
| Perte de données locale | Restaurer la base de données, relancer les traitements |
| Token expiré | Renouveler le token dans T_SITE, le traitement reprendra automatiquement |

### 7.2 Mécanismes de relance

#### 7.2.1 Relance automatique

Les traitements planifiés reprennent automatiquement :
- Les lots non traités sont réessayés à chaque exécution du cron
- Les factures en erreur peuvent être retraitées après correction

#### 7.2.2 Relance manuelle

| Action | Procédure |
|--------|-----------|
| Relancer un lot spécifique | Remettre le lot en statut "à traiter" en base |
| Relancer depuis une date | Modifier lastInsertPurchases dans la configuration |
| Forcer la synchronisation d'un site | Réactiver le site dans T_SITE |

### 7.3 Gestion des incidents

#### 7.3.1 Circuit breaker

L'interface intègre un mécanisme de "circuit breaker" qui protège contre les pannes en cascade :

| État | Condition | Comportement |
|------|-----------|--------------|
| **CLOSED** | Fonctionnement normal | Tous les appels passent |
| **OPEN** | Plus de 50% d'échecs sur 10 appels | Appels bloqués pendant 30 secondes |
| **HALF_OPEN** | Après 30 secondes d'attente | Test de 3 appels pour vérifier le rétablissement |

#### 7.3.2 Procédure d'escalade

1. **Niveau 1** : Vérification des logs et tableau de bord
2. **Niveau 2** : Analyse des erreurs détaillées, vérification de la connectivité
3. **Niveau 3** : Intervention technique pour correction ou contournement

---

## 8. Dépendances externes

### 8.1 Dépendances systèmes

| Système | Nature de la dépendance | Impact si indisponible |
|---------|------------------------|------------------------|
| **ATHENEO (SQL Server)** | Base de données source/cible | Arrêt complet de l'interface |
| **Pennylane API** | API REST externe | Arrêt des synchronisations vers/depuis Pennylane |
| **WSDocument** | Service de stockage documentaire | Documents non récupérables/stockables |

### 8.2 Dépendances fonctionnelles

| Dépendance | Description | Criticité |
|------------|-------------|-----------|
| **Saisie des factures** | Les factures doivent être saisies dans ATHENEO | Haute |
| **Configuration des sites** | Les sites doivent être paramétrés avec token valide | Haute |
| **Plan comptable** | Les comptes utilisés doivent exister ou être créables | Moyenne |
| **Référentiel produits** | Les produits doivent être référencés | Moyenne |
| **Référentiel tiers** | Les clients/fournisseurs doivent être identifiables | Haute |

### 8.3 Contrats d'interface

#### 8.3.1 API Pennylane

| Endpoint | Méthode | Usage |
|----------|---------|-------|
| /customer_invoices/import | POST | Création de factures clients |
| /suppliers/invoices | GET | Lecture des factures fournisseurs |
| /company_customers | GET/POST/PUT | Gestion des clients |
| /suppliers | GET/POST/PUT | Gestion des fournisseurs |
| /products | GET/POST/PUT | Gestion des produits |
| /ledger_accounts | GET/POST | Gestion des comptes comptables |
| /categories | GET | Lecture des catégories |
| /files | POST | Upload de fichiers |

#### 8.3.2 Limites API Pennylane

| Limite | Valeur | Gestion |
|--------|--------|---------|
| Appels par minute | 100 | Rate limiter intégré |
| Taille max fichier | 10 Mo | Vérification avant upload |
| Timeout | 30 secondes | Time limiter intégré |

---

## 9. Glossaire

| Terme | Définition |
|-------|------------|
| **ATHENEO** | ERP interne utilisé pour la gestion commerciale et administrative |
| **Pennylane** | Logiciel de comptabilité cloud partenaire |
| **Écriture comptable** | Enregistrement d'une opération dans les comptes (débit/crédit) |
| **Lot d'écritures** | Regroupement d'écritures comptables pour traitement |
| **BAP** | Bon À Payer - statut autorisant le règlement d'une facture fournisseur |
| **Tiers** | Entité externe (client ou fournisseur) avec laquelle l'entreprise est en relation |
| **SIRET** | Numéro d'identification des établissements (14 chiffres) |
| **SIREN** | Numéro d'identification des entreprises (9 chiffres) |
| **Token** | Jeton d'authentification pour accéder à l'API Pennylane |
| **Cron** | Expression définissant la planification d'une tâche automatique |
| **Circuit breaker** | Mécanisme de protection contre les pannes en cascade |
| **Rate limiter** | Mécanisme de limitation du nombre d'appels API |
| **Retry** | Mécanisme de réessai automatique en cas d'erreur |
| **Deadlock** | Situation de blocage mutuel entre transactions concurrentes |
| **Forum** | Table de traçabilité métier des opérations |
| **WSDocument** | Service web de gestion documentaire |

---

## Annexes

### Annexe A : Schéma des flux de données

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           FLUX DE DONNÉES DÉTAILLÉS                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ATHENEO                        INTERFACE                       PENNYLANE   │
│  ────────                       ─────────                       ─────────   │
│                                                                              │
│  ┌──────────┐                                                               │
│  │V_FACTURE │────────────────────┐                                          │
│  └──────────┘                    │                                          │
│                                  ▼                                          │
│  ┌──────────┐              ┌──────────┐              ┌──────────────┐       │
│  │ SOCIETE  │─────────────►│ TRANSFO  │─────────────►│ CUSTOMER_INV │       │
│  └──────────┘              │          │              └──────────────┘       │
│                            │          │                                     │
│  ┌──────────┐              │ MAPPING  │              ┌──────────────┐       │
│  │ PRODUITS │─────────────►│          │─────────────►│   PRODUCTS   │       │
│  └──────────┘              │          │              └──────────────┘       │
│                            │          │                                     │
│  ┌──────────┐              │          │              ┌──────────────┐       │
│  │ COURRIER │─────────────►│ VALIDATION─────────────►│    FILES     │       │
│  └──────────┘              └──────────┘              └──────────────┘       │
│                                                                              │
│  ════════════════════════════════════════════════════════════════════════   │
│                                                                              │
│  ┌──────────┐              ┌──────────┐              ┌──────────────┐       │
│  │A_FACTURE │◄─────────────│ TRANSFO  │◄─────────────│ SUPPLIER_INV │       │
│  └──────────┘              │          │              └──────────────┘       │
│                            │          │                                     │
│  ┌──────────┐              │ MAPPING  │              ┌──────────────┐       │
│  │ SOCIETE  │◄─────────────│          │◄─────────────│  SUPPLIERS   │       │
│  └──────────┘              │          │              └──────────────┘       │
│                            │          │                                     │
│  ┌──────────┐              │          │              ┌──────────────┐       │
│  │REGLEMENT │◄─────────────│ VALIDATION◄─────────────│ TRANSACTIONS │       │
│  └──────────┘              └──────────┘              └──────────────┘       │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Annexe B : Matrice des responsabilités

| Activité | Métier | Support N2 | Support N3 | Technique |
|----------|--------|------------|------------|-----------|
| Paramétrage des sites | R | C | I | I |
| Modification des crons | I | C | R | A |
| Analyse des erreurs | I | R | C | I |
| Correction des données | R | A | C | I |
| Modification du code | I | I | C | R |
| Supervision quotidienne | I | R | I | I |

*R = Responsable, A = Approbateur, C = Consulté, I = Informé*

### Annexe C : Contacts et escalade

| Niveau | Périmètre | Délai de réponse |
|--------|-----------|------------------|
| Support N1 | Questions utilisateurs, aide à l'utilisation | 4h |
| Support N2 | Analyse des anomalies, corrections de données | 8h |
| Support N3 | Incidents complexes, coordination technique | 24h |
| Équipe technique | Corrections de bugs, évolutions | Selon planning |

---

*Document rédigé dans le cadre du projet d'intégration ATHENEO-PENNYLANE*
*Pour toute question ou suggestion d'amélioration, contacter l'équipe projet.*
