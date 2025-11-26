IF EXISTS (SELECT * FROM sys.objects
           WHERE object_id = object_id(N'[SP_PENNYLANE_GET_PRODUCTS]')
           AND is_ms_shipped = 0
           AND [type] IN ('P'))
    DROP PROCEDURE [SP_PENNYLANE_GET_PRODUCTS]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////
Nom de la procédure stockée : SP_PENNYLANE_GET_PRODUCTS
No Version : 001
Description :
    Récupère les informations des produits de la table PRODUITS et retourne
    les résultats sous forme de JSON. Utilisé par l'interface PENNYLANE.

Historique des mises à jour :
> v001 - VABE - 14/06/2024 - Création
//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_PENNYLANE_GET_PRODUCTS]
    @NO_PRODUIT INT
AS
BEGIN
    IF EXISTS (SELECT * FROM sysobjects
               WHERE id = object_id(N'[spe_SP_PENNYLANE_GET_PRODUCTS]')
               AND OBJECTPROPERTY(id, N'IsProcedure') = 1)
    BEGIN
        EXEC spe_SP_PENNYLANE_GET_PRODUCTS @NO_PRODUIT
    END
    ELSE
    BEGIN
        SELECT DISTINCT
		    P.PENNYLANE_ID AS "id",
			CAST(P.NO_PRODUIT AS VARCHAR) AS "externalReference",
            COALESCE(P.COD_PROD, '') AS "reference",  -- Remplace null par une chaîne vide
            COALESCE(P.DES_COM, '') AS "label",  -- Remplace null par une chaîne vide
            COALESCE(P.DES_TEC, '') AS "description",  -- Remplace null par une chaîne vide
            COALESCE(TAX.CODE_CPTE_TAXE, 'FR_200') AS "vatRate",  -- Remplace null par une chaîne vide
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
		LEFT JOIN T_TAXES TAX on TAX.CODE = P.COD_TAXE1
        WHERE P.NO_PRODUIT = @NO_PRODUIT
    END
END
GO