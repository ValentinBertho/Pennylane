IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[SP_TRAITER_FACTURE]') AND is_ms_shipped = 0 AND [type] IN ('P'))
    DROP PROCEDURE [SP_TRAITER_FACTURE]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procédure stockée : SP_TRAITER_FACTURE

Version : 001

Description :
Procédure appelée par l'interface SAGE pour mettre à jour l'état d'une facture après traitement.

Historique des mises à jour :
> v001 - VABE - 29/07/2025 - Création

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_TRAITER_FACTURE]
    @NO_V_FACTURE INT,
    @FACTURE_ID VARCHAR(50) = NULL,
    @FACTURE_ETAT VARCHAR(50) = NULL,
    @FACTURE_ACTION VARCHAR(50) = NULL,
    @FACTURE_COMMENTAIRE VARCHAR(MAX) = NULL,
    @SUCCES BIT = 1
AS
BEGIN
    SET NOCOUNT ON;

    -- Contrôle d'existence de la facture
    IF NOT EXISTS (SELECT 1 FROM V_FACTURE WHERE NO_V_FACTURE = @NO_V_FACTURE)
    BEGIN
        RAISERROR('Aucune facture trouvée pour le numéro spécifié.', 16, 1);
        RETURN;
    END

    -- Appel conditionnel à la procédure spécifique
    IF EXISTS (
        SELECT * FROM sysobjects
        WHERE id = OBJECT_ID(N'[spe_SP_TRAITER_FACTURE]')
        AND OBJECTPROPERTY(id, N'IsProcedure') = 1
    )
    BEGIN
        EXEC spe_SP_TRAITER_FACTURE
            @NO_V_FACTURE,
            @FACTURE_ID,
            @FACTURE_ETAT,
            @FACTURE_ACTION,
            @FACTURE_COMMENTAIRE,
            @SUCCES;
        RETURN;
    END

    -- Traitement par défaut (si procédure spécifique non présente)
    BEGIN TRY
        -- Mise à jour de la facture
        UPDATE V_FACTURE
        SET
            MODIF_LE = GETDATE(),
            MODIF_PAR = 'FACTUREX',
            TYPE_MODULE = ISNULL(@FACTURE_ACTION, 'pdp_outbound'),
            COD_ETAT_ENVOI_FACTURE = ISNULL(@FACTURE_ACTION, CASE WHEN @SUCCES = 1 THEN 'GENERATED' ELSE 'ERROR' END),
            INVOICE_ID = ISNULL(@FACTURE_ID, CHORUS_ID),
            COD_ETAT_PDP = CASE @SUCCES WHEN 0 THEN '1' ELSE '3' END,
            MEMO_PDP = CASE
                WHEN @SUCCES = 0 AND @FACTURE_COMMENTAIRE IS NULL THEN 'Erreur lors du traitement.'
                ELSE ISNULL(@FACTURE_COMMENTAIRE, MEMO_PDP)
            END
        WHERE NO_V_FACTURE = @NO_V_FACTURE;

		DECLARE @InfoValue NVARCHAR(10);

		SET @InfoValue = CASE WHEN @SUCCES = 1 THEN 'MAJ' ELSE 'ERREUR' END;

		EXEC SP_FACTUREX_SYNCHRO_MARQUAGE
			@NO_ENTITE = @NO_V_FACTURE,
			@ENTITE = 'V_FACTURE',
			@INFO = @InfoValue,
			@REF_EXT = '';

    END TRY
    BEGIN CATCH
        -- Log de l'erreur SQL
        DECLARE @ErrorMessage NVARCHAR(4000) = ERROR_MESSAGE();
        DECLARE @ErrorSeverity INT = ERROR_SEVERITY();
        DECLARE @ErrorState INT = ERROR_STATE();

        RAISERROR(@ErrorMessage, @ErrorSeverity, @ErrorState);
    END CATCH
END
GO
