IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[SP_GET_FACTURE]') AND is_ms_shipped = 0 AND [type] IN ('P'))
    DROP PROCEDURE [SP_GET_FACTURE]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procédure stockée : SP_GET_FACTURE

Version : 001

Description :
Récupère une facture et ses lignes associées.

Historique des mises à jour :

> v001 - VABE - 29/07/2025 - Création

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_GET_FACTURE]
    @NO_V_FACTURE INT
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT * FROM sysobjects
        WHERE id = OBJECT_ID(N'[spe_SP_GET_FACTURE]')
        AND OBJECTPROPERTY(id, N'IsProcedure') = 1
    )
    BEGIN
        EXEC spe_SP_GET_FACTURE @NO_V_FACTURE
    END
    ELSE
    BEGIN

        -- Récupération des informations de la facture et de ses lignes
        SELECT TOP 1
            F.NO_V_FACTURE AS noVFacture,
            F.CHRONO_V_FACTURE AS chronoVFacture,
            F.COD_ETAT AS codEtat,
            F.DATE_FACTURE AS dateFacture,
            F.MTT_HT AS mttHt,
            F.MTT_TTC AS mttTtc,
            F.NETAPAYER AS netAPayer,
            ISNULL(F.OBJET, '') AS objet,
            ISNULL(E.DATE_ECH, F.DATE_FACTURE) AS dateEcheance,
            ISNULL(F.INVOICE_ID, '') AS documentId,
            ISNULL(F.TYPE_MODULE, '') AS moduleId,
            ISNULL(F.COD_ETAT_PDP, '') AS statutPdp,
            '' AS etatRejet,
            ISNULL(CONVERT(VARCHAR, SM.DATE_SYNCHRO, 120), '') AS dateSync,
            ISNULL(F.MEMO_PDP, '') AS comment,
            ISNULL(SM.COD_STATUT_SYNCHRO_MARQUAGE, 'NON_SYNCHRONISE') AS syncStatus,
            ISNULL(SM.INFO, '') AS syncComment
        FROM
            V_FACTURE F
        LEFT JOIN ECHEANCE E ON E.NO_V_FACTURE = F.NO_V_FACTURE
        LEFT JOIN SYNCHRO_MARQUAGE SM ON F.NO_V_FACTURE = SM.NO_ENTITE AND SM.NOM_ENTITE = 'V_FACTURE'
        WHERE
            F.NO_V_FACTURE = @NO_V_FACTURE
    END
END
GO
