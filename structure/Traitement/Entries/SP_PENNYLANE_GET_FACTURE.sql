if exists (select * from sys.objects where object_id = object_id(N'[SP_PENNYLANE_GET_FACTURE]') and is_ms_shipped = 0 and [type] in ('P'))
drop procedure [SP_PENNYLANE_GET_FACTURE]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procédure stockée : SP_PENNYLANE_GET_FACTURE

Version : 001

Description :
Récupère une facture et ses lignes associées en fonction d'un site donné (COD_SITE).

Historique des mises à jour :

> v001 - VABE - 14/06/2024 - Création

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_PENNYLANE_GET_FACTURE]
    @NO_V_FACTURE INT
AS
IF exists (select * from sysobjects
where id = object_id(N'[spe_SP_PENNYLANE_GET_FACTURE]')
and OBJECTPROPERTY(id, N'IsProcedure') = 1)
BEGIN
	EXEC spe_SP_PENNYLANE_GET_FACTURE @NO_V_FACTURE
END
ELSE
BEGIN
    SET NOCOUNT ON;

    -- Vérification de l'existence d'une facture pour le site donné
    IF NOT EXISTS (
        SELECT 1
        FROM V_FACTURE
        WHERE NO_V_FACTURE = @NO_V_FACTURE
    )
    BEGIN
        RAISERROR('Aucune facture trouvée pour le site spécifié.', 16, 1);
        RETURN;
    END

        -- Vérification de l'existence d'un PDF pour la facture
        IF NOT EXISTS (
            SELECT 1
            FROM COURRIER
            WHERE NO_V_FACTURE = @NO_V_FACTURE
        )
        BEGIN
            RAISERROR('Aucune facture PDF trouvée.', 16, 1);
            RETURN;
        END

    -- Récupération des informations de la facture et de ses lignes
    SELECT
        -- Colonnes de la table V_FACTURE (Facture principale)
        F.NO_V_FACTURE AS noVFacture,       -- Numéro de facture
        F.CHRONO_V_FACTURE AS chronoVFacture,   -- Référence unique de facture
        F.COD_SITE AS codSite,           -- Code du site
        F.COD_ETAT AS codEtat,           -- État de la facture
        F.DATE_FACTURE AS dateFacture,        -- Date de la facture
        F.MTT_HT AS mttHt,            -- Montant HT
        F.MTT_TTC AS mttTtc,           -- Montant TTC
        F.NETAPAYER AS netAPayer,        -- Net à payer
        F.OBJET + ' - ' + F.CHORUS_NUM_FACTURE AS objet,  -- Objet de la facture
        F.NO_SOCIETE AS noSociete,    -- No societe
        SOC.PENNYLANE_ID AS customerPennylaneId,    -- penny lane id
        F.PENNYLANE_ID AS invoicePennylaneId,    -- penny lane id
        -- Colonnes de la table V_L_FACTURE (Lignes de facture)
        LF.NO_V_L_FACTURE AS noVLFacture,    -- Numéro de ligne de facture
        LF.NO_LIGNE AS noLigne,          -- Numéro de ligne
        LF.TYPE_LIGNE AS typeLigne,        -- Type de ligne (Produit, Service, etc.)
        LF.NO_PRODUIT AS noProduit,        -- Numéro du produit
        PROD.PENNYLANE_ID AS idProduit,        -- Id du produit
        LF.DES_COM AS desCom,           -- Description du produit ou service
		TAX.CODE_CPTE_TAXE AS codTaxe,  -- Description du produit ou service
		TAX.TAUX AS tauxTaxe,
		-- 1 AS qteFac,                    -- VERRUE pour correctif
        LF.QTE_FAC AS qteFac,           -- Quantité facturée
        LF.PUVB AS puvb,              -- Prix unitaire brut
        LF.QTE_FAC * (LF.PUVB * (1 + (TAX.TAUX / 100))) AS puNet, -- VERRUE Pour correctif
        -- LF.PUVB * (1 + (TAX.TAUX / 100)) AS puNet, -- Calcul du prix unitaire net
		LF.QTE_FAC * (LF.PUVB * (1 + (TAX.TAUX / 100))) AS totalNet, -- Calcul du prix unitaire net
        LF.TOTALHT AS totalHT,            -- Total HT de la ligne
        LF.DATE_DEBUT AS startDate,
        LF.DATE_FIN AS endDate,
        TRIM(COALESCE(LF.CPTE, '-1')) as CPTE
    FROM
        V_FACTURE F
    INNER JOIN
        V_L_FACTURE LF ON LF.NO_V_FACTURE = F.NO_V_FACTURE
	INNER JOIN
        PRODUITS PROD ON LF.NO_PRODUIT = PROD.NO_PRODUIT
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
GO
