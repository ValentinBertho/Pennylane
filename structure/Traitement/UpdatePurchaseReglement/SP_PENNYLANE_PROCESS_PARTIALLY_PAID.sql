IF EXISTS (SELECT * FROM sys.objects WHERE object_id = object_id(N'[SP_PENNYLANE_PROCESS_PARTIALLY_PAID]') AND is_ms_shipped = 0 AND [type] IN ('P'))
DROP PROCEDURE [SP_PENNYLANE_PROCESS_PARTIALLY_PAID]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procedure stockee : SP_PENNYLANE_PROCESS_PARTIALLY_PAID

No Version : 001

Description :
Traite une facture partiellement payée et met à jour les informations
de paiement dans la base de données.

Procedure appelee par :
SP_PENNYLANE_SUPPLIER_INVOICE_MAJ_REGLEMENTS

Historique des mises a jour :

> v001 - [VABE] - 05/03/2025 - Création

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [dbo].[SP_PENNYLANE_PROCESS_PARTIALLY_PAID]
          @PAID BIT,
          @PAYMENT_STATUS VARCHAR(50),
          @REMAINING_AMOUNT NUMERIC(18,5),
          @FULLY_PAID_AT DATETIME = NULL,
          @CURRENCY_AMOUNT NUMERIC(18,5),
          @INVOICE_ID VARCHAR(50),
          @RESULT_OUTPUT INT OUTPUT
      AS
      IF exists (select * from sysobjects
      where id = object_id(N'[spe_SP_PENNYLANE_PROCESS_PARTIALLY_PAID]')
      and OBJECTPROPERTY(id, N'IsProcedure') = 1)
      BEGIN
      	EXEC spe_SP_PENNYLANE_PROCESS_PARTIALLY_PAID         @PAID,
                                                               @PAYMENT_STATUS,
                                                               @REMAINING_AMOUNT,
                                                               @FULLY_PAID_AT,
                                                               @CURRENCY_AMOUNT,
                                                               @INVOICE_ID,
                                                               @RESULT_OUTPUT OUTPUT;
      END
      ELSE
      BEGIN
          SET NOCOUNT ON;

          DECLARE @NO_V_FACTURE		INT
      	DECLARE @ID_ECHEANCE		INT
      	DECLARE @RELIQUAT_A_PAYER	NUMERIC(18,5)
      	DECLARE @MTT_TTC			NUMERIC(18,5)
      	DECLARE @MTT_RGLT			NUMERIC(18,5) = 0
      	DECLARE @NO_REGLEMENT		INT
      	DECLARE @COD_DEVISE			VARCHAR(10)
      	DECLARE @AVOIR				BIT
      	DECLARE @DATE_RGLT_FRANCAISE VARCHAR(10) = CONVERT(VARCHAR(10), GETDATE(), 103);  -- Format français (jour/mois/année)
      	DECLARE @MTT_REGLE			NUMERIC(18,5)

      	SELECT TOP 1
      		@NO_V_FACTURE	= NO_V_FACTURE,
      		@AVOIR			= CASE WHEN V_FACTURE.NETAPAYER >= 0 THEN 0 ELSE 1 END
      	FROM V_FACTURE
      	WHERE PENNYLANE_ID = @INVOICE_ID
      	/*
      		SET @MTT_REGLE = (SELECT SUM(ECHEANCE.MTT_RGLT)
      		FROM ECHEANCE
      			INNER JOIN V_FACTURE ON V_FACTURE.NO_V_FACTURE = ECHEANCE.NO_V_FACTURE
      		WHERE V_FACTURE.NO_V_FACTURE = @NO_V_FACTURE)
      	*/
      	SELECT @RELIQUAT_A_PAYER = isnull(@CURRENCY_AMOUNT, 0) - isnull(@REMAINING_AMOUNT, 0) - isnull(SUM(ECHEANCE.MTT_RGLT), 0)
      	FROM ECHEANCE
      		INNER JOIN V_FACTURE ON V_FACTURE.NO_V_FACTURE = ECHEANCE.NO_V_FACTURE
      	WHERE V_FACTURE.NO_V_FACTURE = @NO_V_FACTURE

          BEGIN TRY
              -- Mise à jour du statut de la facture en "partially_paid"
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

      		-- Création des règlements
      		DECLARE MAJ_RGLT_FACTURE CURSOR LOCAL FOR
      		SELECT ECHEANCE.NO_ECHEANCE, ABS(ECHEANCE.MTT_TTC), ABS(ECHEANCE.MTT_RGLT), V_FACTURE.COD_DEVISE
      		FROM ECHEANCE
      			INNER JOIN V_FACTURE ON V_FACTURE.NO_V_FACTURE = ECHEANCE.NO_V_FACTURE
      		WHERE V_FACTURE.NO_V_FACTURE = @NO_V_FACTURE
      			AND isnull(ECHEANCE.SOLDEE, 0) = 0
      		ORDER BY DATE_ECH ASC
      		OPEN MAJ_RGLT_FACTURE
      		FETCH NEXT FROM MAJ_RGLT_FACTURE INTO @ID_ECHEANCE, @MTT_TTC, @MTT_REGLE, @COD_DEVISE

      		WHILE @@FETCH_STATUS = 0
      		BEGIN

      			-- Test d'existence d'un règlement, pour le cas d'une facture client
      			IF(@RELIQUAT_A_PAYER > 0) -- AND NOT EXISTS(SELECT * FROM REGLEMENT WHERE NO_ECHEANCE = @ID_ECHEANCE AND ABS(MTT_TTC) = @MTT_TTC))
      			BEGIN

      				SET @MTT_RGLT = CASE WHEN @RELIQUAT_A_PAYER < @MTT_TTC - @MTT_REGLE THEN @RELIQUAT_A_PAYER ELSE @MTT_TTC - @MTT_REGLE END
      								* CASE WHEN @AVOIR = 1 THEN -1 ELSE 1 END

      				-- Tirage de compteur unique
      				EXEC sp_COMPTEUR 'NO_REGLEMENT', @NO_REGLEMENT OUT

      				-- Enregistrement du règlement
      				INSERT INTO REGLEMENT(
      				NO_REGLEMENT, NO_ECHEANCE, NO_V_FACTURE, COD_DEVISE, CREER_LE, CREER_PAR, DATE_RGLT,LIBELLE,ROWGUID, MTT_TTC, MTT_TTC_CUR)
      				VALUES(
      				@NO_REGLEMENT, @ID_ECHEANCE, @NO_V_FACTURE, @COD_DEVISE, GETDATE(), 'PENNYLANE', @DATE_RGLT_FRANCAISE, 'Règlement automatique', NEWID(), @MTT_RGLT, @MTT_RGLT)

      				-- Mise à jour de l'échéance, et par rebond de la Facture client, et des encours
      				EXEC SP_ECHEANCE_CALCULER @ID_ECHEANCE

      				SET @RELIQUAT_A_PAYER = @RELIQUAT_A_PAYER - @MTT_RGLT
      				SET @MTT_RGLT = 0
      			END

      			FETCH NEXT FROM MAJ_RGLT_FACTURE INTO @ID_ECHEANCE, @MTT_TTC, @MTT_REGLE, @COD_DEVISE
      		END
      		CLOSE MAJ_RGLT_FACTURE
      		DEALLOCATE MAJ_RGLT_FACTURE

              IF @@ROWCOUNT > 0
              BEGIN
                  SET @RESULT_OUTPUT = 1; -- Succès
              END
              ELSE
              BEGIN
                  SET @RESULT_OUTPUT = -1; -- Aucune ligne mise à jour
              END
          END TRY
          BEGIN CATCH
            SET @RESULT_OUTPUT = -99;

            DECLARE @PARAMS_LOG NVARCHAR(MAX);

            SET @PARAMS_LOG =
                'PAID=' + ISNULL(CONVERT(VARCHAR, @PAID), 'NULL') +
                ', PAYMENT_STATUS=' + ISNULL(@PAYMENT_STATUS, 'NULL') +
                ', REMAINING_AMOUNT=' + ISNULL(CONVERT(VARCHAR, @REMAINING_AMOUNT), 'NULL') +
                ', FULLY_PAID_AT=' + ISNULL(@FULLY_PAID_AT, 'NULL') +
                ', CURRENCY_AMOUNT=' + ISNULL(CONVERT(VARCHAR, @CURRENCY_AMOUNT), 'NULL') +
                ', INVOICE_ID=' + ISNULL(@INVOICE_ID, 'NULL');

            EXEC SP_INSERT_ERROR_LOG
                @PROCEDURE_NAME = 'SP_PENNYLANE_SUPPLIER_INVOICE_MAJ_REGLEMENTS',
                @PARAMETERS = @PARAMS_LOG;
                          END CATCH
      END;

