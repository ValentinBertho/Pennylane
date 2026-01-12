/*
==============================================================================
SCRIPT D'INSTALLATION DE LA BASE DE DONNÉES PENNYLANE
Auteur: VABE
Date de création: 2025-12-19
==============================================================================
*/

:setvar BasePath "C:\GIT\interface-pennylane"
GO

SET NOCOUNT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

PRINT '==============================================================================' 
PRINT 'DÉBUT DE L''INSTALLATION DE LA BASE DE DONNÉES PENNYLANE'
PRINT 'Date/Heure: ' + CONVERT(VARCHAR, GETDATE(), 120)
PRINT '=============================================================================='
PRINT ''
GO

/* ============================================================================== 
   SECTION 1 : CRÉATION DES TABLES
============================================================================= */
PRINT '=============================================================================='
PRINT 'SECTION 1/3 : CRÉATION DES TABLES'
PRINT '=============================================================================='
GO

PRINT '→ Création de ERROR_LOGS...'
:r $(BasePath)\structure\TABLE\ERROR_LOGS.sql
PRINT '  ✓ ERROR_LOGS créée'
GO

PRINT '→ Création de LOG...'
:r $(BasePath)\structure\TABLE\LOG.sql
PRINT '  ✓ LOG créée'
GO

PRINT '→ Création de T_SITE...'
:r $(BasePath)\structure\TABLE\T_SITE.sql
PRINT '  ✓ T_SITE créée'
GO

PRINT '→ Création de T_PARAMETRE...'
:r $(BasePath)\structure\TABLE\T_PARAMETRE.sql
PRINT '  ✓ T_PARAMETRE créée'
GO

PRINT '→ Création de SOCIETE...'
:r $(BasePath)\structure\TABLE\SOCIETE.sql
PRINT '  ✓ SOCIETE créée'
GO

PRINT '→ Création de SOCIETE_EXPORT...'
:r $(BasePath)\structure\TABLE\SOCIETE_EXPORT.sql
PRINT '  ✓ SOCIETE_EXPORT créée'
GO

PRINT '→ Création de PENNYLANE_DEFAULT_VALUES...'
:r $(BasePath)\structure\TABLE\PENNYLANE_DEFAULT_VALUES.sql
PRINT '  ✓ PENNYLANE_DEFAULT_VALUES créée'
GO

PRINT '→ Création de PENNYLANE_REFERENTIALS...'
:r $(BasePath)\structure\TABLE\PENNYLANE_REFERENTIALS.sql
PRINT '  ✓ PENNYLANE_REFERENTIALS créée'
GO

PRINT '→ Création de PRODUITS...'
:r $(BasePath)\structure\TABLE\PRODUITS.sql
PRINT '  ✓ PRODUITS créée'
GO

PRINT '→ Création de A_FACTURE...'
:r $(BasePath)\structure\TABLE\A_FACTURE.sql
PRINT '  ✓ A_FACTURE créée'
GO

PRINT '→ Création de V_FACTURE...'
:r $(BasePath)\structure\TABLE\V_FACTURE.sql
PRINT '  ✓ V_FACTURE créée'
GO

PRINT '→ Création de LOT_ECRITURE...'
:r $(BasePath)\structure\TABLE\LOT_ECRITURE.sql
PRINT '  ✓ LOT_ECRITURE créée'
GO

PRINT '→ Création de REGLEMENT...'
:r $(BasePath)\structure\TABLE\REGLEMENT.sql
PRINT '  ✓ REGLEMENT créée'
GO

PRINT ''
PRINT '✓ Toutes les tables ont été créées avec succès'
PRINT ''
GO

/* ============================================================================== 
   SECTION 2 : CRÉATION DES FONCTIONS
============================================================================= */
PRINT '=============================================================================='
PRINT 'SECTION 2/3 : CRÉATION DES FONCTIONS'
PRINT '=============================================================================='
GO

PRINT '→ Création de fn_PENNYLANE_COD_COM...'
:r $(BasePath)\structure\FNC\fn_PENNYLANE_COD_COM.sql
PRINT '  ✓ fn_PENNYLANE_COD_COM créée'
GO

PRINT '→ Création de fn_PENNYLANE_COD_SERVICE...'
:r $(BasePath)\structure\FNC\fn_PENNYLANE_COD_SERVICE.sql
PRINT '  ✓ fn_PENNYLANE_COD_SERVICE créée'
GO

PRINT '→ Création de fn_PENNYLANE_COD_TYPE...'
:r $(BasePath)\structure\FNC\fn_PENNYLANE_COD_TYPE.sql
PRINT '  ✓ fn_PENNYLANE_COD_TYPE créée'
GO

PRINT ''
PRINT '✓ Toutes les fonctions ont été créées avec succès'
PRINT ''
GO

/* ============================================================================== 
   SECTION 3 : CRÉATION DES PROCÉDURES STOCKÉES
============================================================================= */
PRINT '=============================================================================='
PRINT 'SECTION 3/3 : CRÉATION DES PROCÉDURES STOCKÉES'
PRINT '=============================================================================='
PRINT ''
GO

-- Procédures générales
PRINT '--- Procédures générales ---'
GO

PRINT '→ Création de SP_LOG_PURGER...'
:r $(BasePath)\structure\Traitement\SP_LOG_PURGER.sql
PRINT '  ✓ SP_LOG_PURGER créée'
GO

PRINT '→ Création de SP_PENNYLANE_AJOUT_FORUM_LIGNE...'
:r $(BasePath)\structure\Traitement\SP_PENNYLANE_AJOUT_FORUM_LIGNE.sql
PRINT '  ✓ SP_PENNYLANE_AJOUT_FORUM_LIGNE créée'
GO

PRINT '→ Création de SP_PENNYLANE_SYNCHRO_MARQUAGE...'
:r $(BasePath)\structure\Traitement\SP_PENNYLANE_SYNCHRO_MARQUAGE.sql
PRINT '  ✓ SP_PENNYLANE_SYNCHRO_MARQUAGE créée'
GO

PRINT ''
GO

-- Procédures Entries
PRINT '--- Procédures Entries ---'
GO

PRINT '→ Création de SP_PENNYLANE_GET_FACTURE...'
:r $(BasePath)\structure\Traitement\Entries\SP_PENNYLANE_GET_FACTURE.sql
PRINT '  ✓ SP_PENNYLANE_GET_FACTURE créée'
GO

PRINT '→ Création de SP_PENNYLANE_GET_PRODUCTS...'
:r $(BasePath)\structure\Traitement\Entries\SP_PENNYLANE_GET_PRODUCTS.sql
PRINT '  ✓ SP_PENNYLANE_GET_PRODUCTS créée'
GO

PRINT '→ Création de SP_PENNYLANE_GET_TIERS...'
:r $(BasePath)\structure\Traitement\Entries\SP_PENNYLANE_GET_TIERS.sql
PRINT '  ✓ SP_PENNYLANE_GET_TIERS créée'
GO

PRINT '→ Création de SP_PENNYLANE_MAJ_PRODUITS...'
:r $(BasePath)\structure\Traitement\Entries\SP_PENNYLANE_MAJ_PRODUITS.sql
PRINT '  ✓ SP_PENNYLANE_MAJ_PRODUITS créée'
GO

PRINT '→ Création de SP_PENNYLANE_MAJ_SOCIETE...'
:r $(BasePath)\structure\Traitement\Entries\SP_PENNYLANE_MAJ_SOCIETE.sql
PRINT '  ✓ SP_PENNYLANE_MAJ_SOCIETE créée'
GO

PRINT '→ Création de SP_PENNYLANE_EXPORT_ECRITURES...'
:r $(BasePath)\structure\Traitement\Entries\SP_PENNYLANE_EXPORT_ECRITURES.sql
PRINT '  ✓ SP_PENNYLANE_EXPORT_ECRITURES créée'
GO

PRINT '→ Création de SP_PENNYLANE_EXPORT_FACTURE_COURRIER...'
:r $(BasePath)\structure\Traitement\Entries\SP_PENNYLANE_EXPORT_FACTURE_COURRIER.sql
PRINT '  ✓ SP_PENNYLANE_EXPORT_FACTURE_COURRIER créée'
GO

PRINT '→ Création de SP_PENNYLANE_EXPORT_LOT...'
:r $(BasePath)\structure\Traitement\Entries\SP_PENNYLANE_EXPORT_LOT.sql
PRINT '  ✓ SP_PENNYLANE_EXPORT_LOT créée'
GO

PRINT '→ Création de SP_PENNYLANE_TRAITER_FACTURE...'
:r $(BasePath)\structure\Traitement\Entries\SP_PENNYLANE_TRAITER_FACTURE.sql
PRINT '  ✓ SP_PENNYLANE_TRAITER_FACTURE créée'
GO

PRINT '→ Création de SP_PENNYLANE_TRAITER_LOT...'
:r $(BasePath)\structure\Traitement\Entries\SP_PENNYLANE_TRAITER_LOT.sql
PRINT '  ✓ SP_PENNYLANE_TRAITER_LOT créée'
GO

PRINT ''
GO

-- Procédures Purchases
PRINT '--- Procédures Purchases ---'
GO

PRINT '→ Création de SP_PENNYLANE_MAJ_SOCIETE_FOURNISSEUR...'
:r $(BasePath)\structure\Traitement\Purchases\SP_PENNYLANE_MAJ_SOCIETE_FOURNISSEUR.sql
PRINT '  ✓ SP_PENNYLANE_MAJ_SOCIETE_FOURNISSEUR créée'
GO

PRINT '→ Création de SP_PENNYLANE_SUPPLIER_INVOICE_CREER...'
:r $(BasePath)\structure\Traitement\Purchases\SP_PENNYLANE_SUPPLIER_INVOICE_CREER.sql
PRINT '  ✓ SP_PENNYLANE_SUPPLIER_INVOICE_CREER créée'
GO

PRINT '→ Création de SP_PENNYLANE_SUPPLIER_INVOICE_EXIST...'
:r $(BasePath)\structure\Traitement\Purchases\SP_PENNYLANE_SUPPLIER_INVOICE_EXIST.sql
PRINT '  ✓ SP_PENNYLANE_SUPPLIER_INVOICE_EXIST créée'
GO

PRINT '→ Création de SP_PENNYLANE_SUPPLIER_INVOICE_MAJ...'
:r $(BasePath)\structure\Traitement\Purchases\SP_PENNYLANE_SUPPLIER_INVOICE_MAJ.sql
PRINT '  ✓ SP_PENNYLANE_SUPPLIER_INVOICE_MAJ créée'
GO

PRINT ''
GO

-- Procédures UpdatePurchaseReglement
PRINT '--- Procédures UpdatePurchaseReglement ---'
GO

PRINT '→ Création de SP_PENNYLANE_PROCESS_FULLY_PAID...'
:r $(BasePath)\structure\Traitement\UpdatePurchaseReglement\SP_PENNYLANE_PROCESS_FULLY_PAID.sql
PRINT '  ✓ SP_PENNYLANE_PROCESS_FULLY_PAID créée'
GO

PRINT '→ Création de SP_PENNYLANE_PROCESS_PARTIALLY_PAID...'
:r $(BasePath)\structure\Traitement\UpdatePurchaseReglement\SP_PENNYLANE_PROCESS_PARTIALLY_PAID.sql
PRINT '  ✓ SP_PENNYLANE_PROCESS_PARTIALLY_PAID créée'
GO

PRINT '→ Création de SP_PENNYLANE_PROCESS_SOLDEE...'
:r $(BasePath)\structure\Traitement\UpdatePurchaseReglement\SP_PENNYLANE_PROCESS_SOLDEE.sql
PRINT '  ✓ SP_PENNYLANE_PROCESS_SOLDEE créée'
GO

PRINT '→ Création de SP_PENNYLANE_SUPPLIER_INVOICE_CREER_REGLEMENT...'
:r $(BasePath)\structure\Traitement\UpdatePurchaseReglement\SP_PENNYLANE_SUPPLIER_INVOICE_CREER_REGLEMENT.sql
PRINT '  ✓ SP_PENNYLANE_SUPPLIER_INVOICE_CREER_REGLEMENT créée'
GO

PRINT '→ Création de SP_PENNYLANE_SUPPLIER_INVOICE_MAJ_REGLEMENTS...'
:r $(BasePath)\structure\Traitement\UpdatePurchaseReglement\SP_PENNYLANE_SUPPLIER_INVOICE_MAJ_REGLEMENTS.sql
PRINT '  ✓ SP_PENNYLANE_SUPPLIER_INVOICE_MAJ_REGLEMENTS créée'
GO

PRINT '→ Création de SP_PENNYLANE_SUPPLIER_INVOICE_PURGE_OBSOLETE_TRANSACTIONS...'
:r $(BasePath)\structure\Traitement\UpdatePurchaseReglement\SP_PENNYLANE_SUPPLIER_INVOICE_PURGE_OBSOLETE_TRANSACTIONS.sql
PRINT '  ✓ SP_PENNYLANE_SUPPLIER_INVOICE_PURGE_OBSOLETE_TRANSACTIONS créée'
GO

PRINT '→ Création de SP_PENNYLANE_SUPPLIER_INVOICE_REGLEMENT...'
:r $(BasePath)\structure\Traitement\UpdatePurchaseReglement\SP_PENNYLANE_SUPPLIER_INVOICE_REGLEMENT.sql
PRINT '  ✓ SP_PENNYLANE_SUPPLIER_INVOICE_REGLEMENT créée'
GO

PRINT ''
GO

-- Procédures UpdateSale
PRINT '--- Procédures UpdateSale ---'
GO

PRINT '→ Création de SP_PENNYLANE_CUSTOMER_INVOICE_BAP...'
:r $(BasePath)\structure\Traitement\UpdateSale\SP_PENNYLANE_CUSTOMER_INVOICE_BAP.sql
PRINT '  ✓ SP_PENNYLANE_CUSTOMER_INVOICE_BAP créée'
GO

PRINT '→ Création de SP_PENNYLANE_TRAITER_CUSTOMER_INVOICE_BAP...'
:r $(BasePath)\structure\Traitement\UpdateSale\SP_PENNYLANE_TRAITER_CUSTOMER_INVOICE_BAP.sql
PRINT '  ✓ SP_PENNYLANE_TRAITER_CUSTOMER_INVOICE_BAP créée'
GO

PRINT ''
PRINT '✓ Toutes les procédures stockées ont été créées avec succès'
PRINT ''
GO

/* ============================================================================== 
   VÉRIFICATION DE L'INSTALLATION
============================================================================= */
PRINT '=============================================================================='
PRINT 'VÉRIFICATION DE L''INSTALLATION'
PRINT '=============================================================================='
PRINT ''
GO

PRINT '→ Tables créées :'
SELECT TABLE_NAME, TABLE_TYPE
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_NAME IN (
    'ERROR_LOGS', 'LOG', 'T_SITE', 'T_PARAMETRE', 'SOCIETE', 'SOCIETE_EXPORT',
    'PENNYLANE_DEFAULT_VALUES', 'PENNYLANE_REFERENTIALS', 'PRODUITS', 
    'A_FACTURE', 'V_FACTURE', 'LOT_ECRITURE', 'REGLEMENT'
)
ORDER BY TABLE_NAME;
GO

PRINT ''
PRINT '→ Fonctions créées :'
SELECT ROUTINE_NAME, ROUTINE_TYPE
FROM INFORMATION_SCHEMA.ROUTINES
WHERE ROUTINE_NAME LIKE 'fn_PENNYLANE_%'
ORDER BY ROUTINE_NAME;
GO

PRINT ''
PRINT '→ Procédures stockées créées :'
SELECT 
    ROUTINE_NAME,
    CREATED,
    CASE 
        WHEN ROUTINE_NAME LIKE '%ENTRIES%' THEN 'Entries'
        WHEN ROUTINE_NAME LIKE '%PURCHASES%' OR ROUTINE_NAME LIKE '%SUPPLIER%' THEN 'Purchases'
        WHEN ROUTINE_NAME LIKE '%REGLEMENT%' OR ROUTINE_NAME LIKE '%PAID%' OR ROUTINE_NAME LIKE '%SOLDEE%' THEN 'UpdatePurchaseReglement'
        WHEN ROUTINE_NAME LIKE '%CUSTOMER%' OR ROUTINE_NAME LIKE '%BAP%' THEN 'UpdateSale'
        ELSE 'Général'
    END AS Catégorie
FROM INFORMATION_SCHEMA.ROUTINES
WHERE ROUTINE_TYPE = 'PROCEDURE'
  AND (ROUTINE_NAME LIKE 'SP_PENNYLANE_%' OR ROUTINE_NAME = 'SP_LOG_PURGER')
ORDER BY Catégorie, ROUTINE_NAME;
GO

/* ============================================================================== 
   FIN
============================================================================= */
PRINT ''
PRINT '==============================================================================' 
PRINT '✓ INSTALLATION PENNYLANE TERMINÉE AVEC SUCCÈS'
PRINT 'Date/Heure: ' + CONVERT(VARCHAR, GETDATE(), 120)
PRINT '=============================================================================='
GO

SET NOCOUNT OFF;
GO