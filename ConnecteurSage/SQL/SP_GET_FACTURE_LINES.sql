IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[SP_GET_FACTURE_LINES]') AND is_ms_shipped = 0 AND [type] IN ('P'))
    DROP PROCEDURE [SP_GET_FACTURE_LINES]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procédure stockée : SP_GET_FACTURE_LINES

Version : 001

Description :
Récupère les lignes associées.

Historique des mises à jour :

> v001 - VABE - 29/07/2025 - Création

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_GET_FACTURE_LINES]
    @NO_V_FACTURE INT
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT * FROM sysobjects
        WHERE id = OBJECT_ID(N'[spe_SP_GET_FACTURE_LINES]')
        AND OBJECTPROPERTY(id, N'IsProcedure') = 1
    )
    BEGIN
        EXEC spe_SP_GET_FACTURE_LINES @NO_V_FACTURE
    END
    ELSE
    BEGIN
        -- Récupération des informations de la facture et de ses lignes
        SELECT
            F.NO_V_FACTURE AS noVFacture,       -- Numéro de facture

            -- Colonnes de la table V_L_FACTURE (Lignes de facture)
            LF.NO_V_L_FACTURE AS noVLFacture,    -- Numéro de ligne de facture
            LF.NO_LIGNE AS noLigne,              -- Numéro de ligne
            LF.TYPE_LIGNE AS typeLigne,          -- Type de ligne (Produit, Service, etc.)
            LF.NO_PRODUIT AS noProduit,          -- Numéro du produit
            LF.COD_PROD AS codProd,              -- Code du produit
            LF.DES_COM AS desCom,                -- Description du produit ou service
            TAX.TAUX AS tauxTaxe,                -- Taux de taxe
            LF.QTE_FAC AS qteFac,                -- Quantité facturée
            LF.PUVB AS puvb,                     -- Prix unitaire brut
            LF.PUVB * (1 + (TAX.TAUX / 100)) AS puNet, -- Calcul du prix unitaire net
            LF.QTE_FAC * (LF.PUVB * (1 + (TAX.TAUX / 100))) AS totalNet, -- Calcul du total net
            LF.TOTALHT AS totalHT                -- Total HT de la ligne
        FROM
            V_FACTURE F
        INNER JOIN
            V_L_FACTURE LF ON LF.NO_V_FACTURE = F.NO_V_FACTURE
        INNER JOIN
            T_TAXES TAX ON LF.COD_TAXE1 = TAX.CODE
        INNER JOIN
            SOCIETE SOC ON SOC.NO_SOCIETE = F.NO_SOCIETE
        WHERE
            F.NO_V_FACTURE = @NO_V_FACTURE
        AND
            LF.QTE_FAC != 0
        AND
            LF.TOTALHT != 0
    END
END
GO
