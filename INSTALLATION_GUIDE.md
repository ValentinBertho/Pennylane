# Guide d'installation de la base de données Pennylane

## Description

Ce guide explique comment installer toutes les structures de base de données nécessaires pour l'application Pennylane en utilisant le script d'installation automatisé.

## Prérequis

- SQL Server 2016 ou version supérieure
- SQL Server Management Studio (SSMS) ou Azure Data Studio
- Droits suffisants pour créer des tables, fonctions et procédures stockées dans la base de données cible

## Structure du script d'installation

Le script `INSTALL_DATABASE.sql` installe les composants dans l'ordre suivant :

### 1. Tables (13 tables)
- ERROR_LOGS - Logs des erreurs de l'application
- LOG - Logs généraux
- T_SITE - Configuration des sites
- T_PARAMETRE - Paramètres de l'application
- SOCIETE - Informations sur les sociétés
- SOCIETE_EXPORT - Export des sociétés
- PENNYLANE_DEFAULT_VALUES - Valeurs par défaut
- PENNYLANE_REFERENTIALS - Référentiels Pennylane
- PRODUITS - Catalogue produits
- A_FACTURE - Factures d'achat
- V_FACTURE - Factures de vente
- LOT_ECRITURE - Lots d'écritures comptables
- REGLEMENT - Règlements

### 2. Fonctions (3 fonctions)
- fn_PENNYLANE_COD_COM - Code commercial
- fn_PENNYLANE_COD_SERVICE - Code service
- fn_PENNYLANE_COD_TYPE - Code type

### 3. Procédures stockées (31 procédures)

#### Procédures OBS (Obsolètes mais conservées)
- SP_PENNYLANE_EXPORT_COMPTES
- SP_PENNYLANE_EXPORT_JOURNAUX
- SP_PENNYLANE_EXPORT_PRODUCTS
- SP_PENNYLANE_EXPORT_TIERS
- SP_PENNYLANE_SYNC_PRODUCT
- SP_PENNYLANE_TRAITER_PIECE

#### Procédures de traitement générales
- SP_LOG_PURGER
- SP_PENNYLANE_AJOUT_FORUM_LIGNE
- SP_PENNYLANE_SYNCHRO_MARQUAGE

#### Procédures Entries (10 procédures)
- SP_PENNYLANE_GET_FACTURE
- SP_PENNYLANE_GET_PRODUCTS
- SP_PENNYLANE_GET_TIERS
- SP_PENNYLANE_MAJ_PRODUITS
- SP_PENNYLANE_MAJ_SOCIETE
- SP_PENNYLANE_EXPORT_ECRITURES
- SP_PENNYLANE_EXPORT_FACTURE_COURRIER
- SP_PENNYLANE_EXPORT_LOT
- SP_PENNYLANE_TRAITER_FACTURE
- SP_PENNYLANE_TRAITER_LOT

#### Procédures Purchases (4 procédures)
- SP_PENNYLANE_MAJ_SOCIETE_FOURNISSEUR
- SP_PENNYLANE_SUPPLIER_INVOICE_CREER
- SP_PENNYLANE_SUPPLIER_INVOICE_EXIST
- SP_PENNYLANE_SUPPLIER_INVOICE_MAJ

#### Procédures UpdatePurchaseReglement (7 procédures)
- SP_PENNYLANE_PROCESS_FULLY_PAID
- SP_PENNYLANE_PROCESS_PARTIALLY_PAID
- SP_PENNYLANE_PROCESS_SOLDEE
- SP_PENNYLANE_SUPPLIER_INVOICE_CREER_REGLEMENT
- SP_PENNYLANE_SUPPLIER_INVOICE_MAJ_REGLEMENTS
- SP_PENNYLANE_SUPPLIER_INVOICE_PURGE_OBSOLETE_TRANSACTIONS
- SP_PENNYLANE_SUPPLIER_INVOICE_REGLEMENT

#### Procédures UpdateSale (2 procédures)
- SP_PENNYLANE_CUSTOMER_INVOICE_BAP
- SP_PENNYLANE_TRAITER_CUSTOMER_INVOICE_BAP

## Instructions d'installation

### Méthode 1 : Via SQL Server Management Studio (SSMS)

1. **Ouvrir SSMS**
   - Lancez SQL Server Management Studio
   - Connectez-vous à votre serveur SQL Server

2. **Sélectionner la base de données**
   - Dans l'explorateur d'objets, sélectionnez la base de données cible
   - Ou créez une nouvelle base de données si nécessaire

3. **Ouvrir le script**
   - Menu : Fichier > Ouvrir > Fichier
   - Naviguez vers `INSTALL_DATABASE.sql`

4. **Activer le mode SQLCMD** (Important !)
   - Menu : Requête > Mode SQLCMD
   - Cette option est nécessaire pour que la commande `:r` fonctionne

5. **Vérifier le répertoire de travail**
   - Assurez-vous que le répertoire de travail de SSMS est le dossier racine du projet Pennylane
   - Ou utilisez des chemins absolus dans le script

6. **Exécuter le script**
   - Cliquez sur le bouton "Exécuter" ou appuyez sur F5
   - Le script s'exécutera et affichera la progression dans l'onglet Messages

### Méthode 2 : Via ligne de commande (sqlcmd)

```bash
# Naviguez vers le dossier du projet
cd /path/to/Pennylane

# Exécutez le script avec sqlcmd
sqlcmd -S <NomServeur> -d <NomBaseDeDonnées> -U <Utilisateur> -P <MotDePasse> -i INSTALL_DATABASE.sql
```

**Exemple :**
```bash
sqlcmd -S localhost -d PennylaneDB -U sa -P VotreMotDePasse -i INSTALL_DATABASE.sql
```

### Méthode 3 : Via Azure Data Studio

1. **Ouvrir Azure Data Studio**
   - Connectez-vous à votre serveur SQL Server

2. **Ouvrir le script**
   - Fichier > Ouvrir le fichier
   - Sélectionnez `INSTALL_DATABASE.sql`

3. **Activer SQLCMD**
   - Dans les paramètres de la requête, activez le mode SQLCMD

4. **Exécuter**
   - Cliquez sur "Exécuter" ou appuyez sur F5

## Vérification de l'installation

Après l'exécution du script, vous pouvez vérifier que tout a été créé correctement :

```sql
-- Vérifier les tables
SELECT name FROM sys.tables WHERE type = 'U' ORDER BY name;

-- Vérifier les fonctions
SELECT name FROM sys.objects WHERE type = 'FN' ORDER BY name;

-- Vérifier les procédures stockées
SELECT name FROM sys.procedures ORDER BY name;
```

## Réinstallation ou mise à jour

Le script est conçu pour être **idempotent** :
- Les tables utilisent `IF NOT EXISTS` - elles ne seront créées que si elles n'existent pas déjà
- Les procédures stockées utilisent `DROP ... IF EXISTS` puis `CREATE` - elles seront recréées à chaque exécution

Pour une réinstallation complète, vous pouvez :
1. Supprimer manuellement les objets existants
2. Ou simplement réexécuter le script (les procédures seront recréées, les tables conservées)

## Dépendances

L'ordre d'exécution dans le script respecte les dépendances :
1. **Tables** en premier (car les fonctions et procédures peuvent les référencer)
2. **Fonctions** ensuite (car les procédures peuvent les utiliser)
3. **Procédures stockées** en dernier

## Troubleshooting

### Erreur : "Could not find file"
- **Cause** : Le mode SQLCMD n'est pas activé ou le chemin vers les fichiers est incorrect
- **Solution** : Activez le mode SQLCMD dans SSMS/Azure Data Studio et vérifiez que vous exécutez le script depuis le bon répertoire

### Erreur de permissions
- **Cause** : L'utilisateur n'a pas les droits suffisants
- **Solution** : Utilisez un compte avec les privilèges `db_ddladmin` ou `db_owner`

### Erreur de dépendances
- **Cause** : Certains objets dépendent d'autres qui n'existent pas encore
- **Solution** : Le script respecte déjà l'ordre des dépendances. Vérifiez que tous les fichiers sources existent

## Support

Pour toute question ou problème, veuillez consulter la documentation du projet ou contacter l'équipe de développement.

## Auteur

Script généré automatiquement pour le projet Pennylane
Date : 2025-12-19
