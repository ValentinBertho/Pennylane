IF EXISTS (SELECT * FROM sys.objects WHERE object_id = object_id(N'[SP_PENNYLANE_PROCESS_SOLDEE]') AND is_ms_shipped = 0 AND [type] IN ('P'))
DROP PROCEDURE [SP_PENNYLANE_PROCESS_SOLDEE]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procedure stockee : SP_PENNYLANE_PROCESS_SOLDEE

No Version : 001

Description :
Traite une facture entièrement payée et met à jour les informations
de paiement dans la base de données.

Procedure appelee par :
SP_PENNYLANE_SUPPLIER_INVOICE_MAJ_REGLEMENTS

Historique des mises a jour :

> v001 - [VABE] - 16/04/2025 - Création

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [dbo].[SP_PENNYLANE_PROCESS_SOLDEE]
    @PAID BIT,
    @PAYMENT_STATUS VARCHAR(50),
    @REMAINING_AMOUNT DECIMAL(18, 2),
    @FULLY_PAID_AT VARCHAR(50),
    @CURRENCY_AMOUNT DECIMAL(18, 2),
    @INVOICE_ID VARCHAR(50),
    @RESULT_OUTPUT INT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    BEGIN TRY

        IF EXISTS (
            SELECT * FROM sysobjects
            WHERE id = object_id(N'[spe_SP_PENNYLANE_PROCESS_SOLDEE]')
              AND OBJECTPROPERTY(id, N'IsProcedure') = 1
        )
        BEGIN
            EXEC spe_SP_PENNYLANE_PROCESS_SOLDEE
                @PAID,
                @PAYMENT_STATUS,
                @REMAINING_AMOUNT,
                @FULLY_PAID_AT,
                @CURRENCY_AMOUNT,
                @INVOICE_ID,
                @RESULT_OUTPUT OUTPUT;
        END
        ELSE
        BEGIN
            DECLARE @NO_V_FACTURE INT;
            DECLARE @ID_ECHEANCE INT;

            SET @NO_V_FACTURE = (SELECT TOP 1 NO_V_FACTURE FROM V_FACTURE WHERE PENNYLANE_ID = @INVOICE_ID);
            SET @ID_ECHEANCE = (SELECT TOP 1 NO_ECHEANCE FROM ECHEANCE WHERE NO_V_FACTURE = @NO_V_FACTURE);

            UPDATE V_FACTURE
            SET
                MODIF_LE = GETDATE(),
                MODIF_PAR = 'PENNYLANE',
                M1 = CONCAT(
                    '{"paid": ', @PAID,
                    ', "payment_status": "', @PAYMENT_STATUS, '"',
                    ', "remaining_amount": ', @REMAINING_AMOUNT,
                    ', "fully_paid_at": "', COALESCE(CONVERT(NVARCHAR, @FULLY_PAID_AT, 120), 'NULL'), '"',
                    ', "currency_amount": ', @CURRENCY_AMOUNT,
                    ', "invoice_id": "', @INVOICE_ID, '"}'
                )
            WHERE NO_V_FACTURE = @NO_V_FACTURE;

            -- Solde de l'échéance
            UPDATE ECHEANCE
            SET SOLDEE = 1,
                DATE_RGLT = dbo.Fnc_AthDateCourte(GETDATE()),
                MTT_RGLT = MTT_TTC,
                MTT_RGLT_CUR = MTT_TTC_CUR
            WHERE NO_ECHEANCE = @ID_ECHEANCE;

            -- Recalcul de l'échéance, et de la facture par rebond
            EXEC SP_V_FACTURE_maj_reglement @NO_V_FACTURE;

            IF @@ROWCOUNT > 0
            BEGIN
                SET @RESULT_OUTPUT = 1; -- Succès
            END
            ELSE
            BEGIN
                SET @RESULT_OUTPUT = -1; -- Aucune ligne mise à jour
            END
        END

    END TRY
    BEGIN CATCH
        SET @RESULT_OUTPUT = -99;

        DECLARE @PARAMS_LOG NVARCHAR(MAX);
        DECLARE @ERROR_MESSAGE NVARCHAR(4000);

        SET @PARAMS_LOG =
            'PAID=' + ISNULL(CONVERT(VARCHAR, @PAID), 'NULL') +
            ', PAYMENT_STATUS=' + ISNULL(@PAYMENT_STATUS, 'NULL') +
            ', REMAINING_AMOUNT=' + ISNULL(CONVERT(VARCHAR, @REMAINING_AMOUNT), 'NULL') +
            ', FULLY_PAID_AT=' + ISNULL(@FULLY_PAID_AT, 'NULL') +
            ', CURRENCY_AMOUNT=' + ISNULL(CONVERT(VARCHAR, @CURRENCY_AMOUNT), 'NULL') +
            ', INVOICE_ID=' + ISNULL(@INVOICE_ID, 'NULL');

        SET @ERROR_MESSAGE = ERROR_MESSAGE();

        EXEC SP_INSERT_ERROR_LOG
            @PROCEDURE_NAME = 'SP_PENNYLANE_PROCESS_SOLDEE',
            @PARAMETERS = @PARAMS_LOG;
    END CATCH
END
GO
