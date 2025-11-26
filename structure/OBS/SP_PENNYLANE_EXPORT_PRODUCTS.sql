IF EXISTS (SELECT * FROM sys.objects
           WHERE object_id = object_id(N'[SP_PENNYLANE_EXPORT_PRODUCTS]')
           AND is_ms_shipped = 0
           AND [type] IN ('P'))
    DROP PROCEDURE [SP_PENNYLANE_EXPORT_PRODUCTS]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////
Nom de la procédure stockée : SP_PENNYLANE_EXPORT_PRODUCTS
No Version : 001
Description :
    Récupère les informations des produits de la table PRODUITS et retourne
    les résultats sous forme de JSON. Utilisé par l'interface PENNYLANE.

Historique des mises à jour :
> v001 - VABE - 14/06/2024 - Création
//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_PENNYLANE_EXPORT_PRODUCTS]
    @COD_SITE VARCHAR
AS
BEGIN
    IF EXISTS (SELECT * FROM sysobjects
               WHERE id = object_id(N'[spe_SP_PENNYLANE_EXPORT_PRODUCTS]')
               AND OBJECTPROPERTY(id, N'IsProcedure') = 1)
    BEGIN
        EXEC spe_SP_PENNYLANE_EXPORT_PRODUCTS
    END
    ELSE
    BEGIN
        SELECT DISTINCT
            P.NO_PRODUIT AS "id",
            COALESCE(P.COD_PROD, '') AS "reference",  -- Remplace null par une chaîne vide
            COALESCE(P.DES_COM, '') AS "label",  -- Remplace null par une chaîne vide
            COALESCE(P.DES_TEC, '') AS "description",  -- Remplace null par une chaîne vide
            COALESCE(TAX.CODE_CPTE_TAXE, 'exempt') AS "vatRate",  -- Remplace null par une chaîne vide
            -- COALESCE(P.COD_TAXE1, 'FR_200') AS "vatRate",  -- Remplace null par une chaîne vide
            COALESCE(
                CASE
                    WHEN P.UA IN ('UN') THEN 'piece'
                    WHEN P.UA IN ('HEURE') THEN 'hour'
                    WHEN P.UA IN ('JOUR') THEN 'day'
                    ELSE 'no_unit'
                END,
                'no_unit'
            ) AS "unit",
            'EUR' AS "currency",  -- Valeur par défaut pour la devise
            COALESCE(
                CASE
                    WHEN P.TYPE_PROD IN ('PRESTA', 'CONTRAT') THEN 'services'
                    ELSE 'goods'
                END,
                'goods'
            ) AS "substance",
            COALESCE(P.CREER_LE, GETDATE()) AS "createdAt",  -- Remplace null par la date actuelle
            COALESCE(P.MODIF_LE, GETDATE()) AS "updatedAt",  -- Remplace null par la date actuelle
            COALESCE(P.PAB, 0) AS "priceBeforeTax",  -- Remplace null par 0
            COALESCE(P.PVB, 0) AS "price"  -- Remplace null par 0
        FROM PRODUITS P
        LEFT JOIN SYNCHRO_MARQUAGE SM
            ON SM.NOM_ENTITE = 'PRODUITS'
            AND SM.NO_ENTITE = P.NO_PRODUIT
            AND SM.DATE_SYNCHRO >= COALESCE(P.MODIF_LE, P.CREER_LE)
            AND SM.CREER_PAR = 'INTERFACE-PENNYLANE'
        INNER JOIN T_TAXES TAX on TAX.CODE = P.COD_TAXE1
        WHERE P.INACTIF = 0  -- Ajoutez des conditions spécifiques selon votre besoin
        AND SM.DATE_SYNCHRO >= P.MODIF_LE
    END
END
GO
