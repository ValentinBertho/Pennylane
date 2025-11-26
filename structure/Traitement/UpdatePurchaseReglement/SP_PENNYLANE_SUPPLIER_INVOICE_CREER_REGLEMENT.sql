IF EXISTS (SELECT * FROM sys.objects WHERE object_id = object_id(N'[SP_PENNYLANE_SUPPLIER_INVOICE_CREER_REGLEMENT]') AND is_ms_shipped = 0 AND [type] IN ('P'))
DROP PROCEDURE [SP_PENNYLANE_SUPPLIER_INVOICE_CREER_REGLEMENT]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

CREATE PROCEDURE [SP_PENNYLANE_SUPPLIER_INVOICE_CREER_REGLEMENT]
    @INVOICE_ID VARCHAR(50),
    @TRANSACTION_ID BIGINT = NULL,
    @MONTANT DECIMAL(18, 2),
    @DATE_REGLEMENT VARCHAR(50),
    @RESULT_OUTPUT INT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        DECLARE @NO_V_FACTURE INT;
        DECLARE @ID_ECHEANCE INT;
        DECLARE @COD_DEVISE VARCHAR(10);
        DECLARE @NO_REGLEMENT INT;
        DECLARE @DATE_RGLT DATETIME;
        DECLARE @DATE_RGLT_FRANCAISE VARCHAR(10);
        DECLARE @SOLDE_REGLEMENT DECIMAL(18,2);
        DECLARE @MONTANT_RESTANT DECIMAL(18,2);

        -- Nettoyage et conversion de la date
        BEGIN TRY
            SET @DATE_RGLT = CONVERT(DATETIME, LEFT(REPLACE(@DATE_REGLEMENT, 'Z', ''), 19), 120);
        END TRY
        BEGIN CATCH
            SET @DATE_RGLT = GETDATE();
        END CATCH
        SET @DATE_RGLT_FRANCAISE = CONVERT(VARCHAR(10), @DATE_RGLT, 103);

        -- Récupération de la clé interne de la facture
        SET @NO_V_FACTURE = (SELECT NO_V_FACTURE FROM V_FACTURE WHERE PENNYLANE_ID = @INVOICE_ID);
        IF @NO_V_FACTURE IS NULL
        BEGIN
            SET @RESULT_OUTPUT = -2; -- Facture non trouvée
            RETURN;
        END

        -- Vérification de l'existence d'une transaction déjà synchronisée
        IF EXISTS (
            SELECT 1 FROM REGLEMENT
            WHERE ID_TRANSACTION_PENNYLANE = @TRANSACTION_ID
              AND NO_V_FACTURE = @NO_V_FACTURE
        )
        BEGIN
            SET @RESULT_OUTPUT = -3; -- Transaction déjà synchronisée
            RETURN;
        END

        -- Initialisation du montant restant à répartir
        SET @MONTANT_RESTANT = @MONTANT;

        -- Curseur sur les échéances non soldées, par ordre croissant
        DECLARE echeances_cursor CURSOR LOCAL FOR
            SELECT ECHEANCE.NO_ECHEANCE, V_FACTURE.COD_DEVISE,
                   ISNULL(ECHEANCE.MTT_TTC, 0) - ISNULL(ECHEANCE.MTT_RGLT, 0) AS SOLDE
            FROM ECHEANCE
            INNER JOIN V_FACTURE ON V_FACTURE.NO_V_FACTURE = ECHEANCE.NO_V_FACTURE
            WHERE V_FACTURE.NO_V_FACTURE = @NO_V_FACTURE
              AND ISNULL(ECHEANCE.SOLDEE, 0) = 0
            ORDER BY ECHEANCE.DATE_ECH ASC;

        OPEN echeances_cursor;
        FETCH NEXT FROM echeances_cursor INTO @ID_ECHEANCE, @COD_DEVISE, @SOLDE_REGLEMENT;

        WHILE @@FETCH_STATUS = 0 AND @MONTANT_RESTANT > 0
        BEGIN
            DECLARE @MONTANT_A_REGLER DECIMAL(18,2);

            -- On ne règle pas plus que le solde de l'échéance
            SET @MONTANT_A_REGLER = CASE
                WHEN @MONTANT_RESTANT < @SOLDE_REGLEMENT THEN @MONTANT_RESTANT
                ELSE @SOLDE_REGLEMENT
            END;

            -- Tirage de compteur unique
            EXEC sp_COMPTEUR 'NO_REGLEMENT', @NO_REGLEMENT OUT;

            -- Enregistrement du règlement
            INSERT INTO REGLEMENT(
                NO_REGLEMENT,
                NO_ECHEANCE,
                NO_V_FACTURE,
                COD_DEVISE,
                CREER_LE,
                CREER_PAR,
                DATE_RGLT,
                LIBELLE,
                ROWGUID,
                MTT_TTC,
                MTT_TTC_CUR,
                ID_TRANSACTION_PENNYLANE
            )
            VALUES(
                @NO_REGLEMENT,
                @ID_ECHEANCE,
                @NO_V_FACTURE,
                @COD_DEVISE,
                GETDATE(),
                'PENNYLANE',
                @DATE_RGLT_FRANCAISE,
                'Règlement automatique (PennyLane)',
                NEWID(),
                @MONTANT_A_REGLER,
                @MONTANT_A_REGLER,
                @TRANSACTION_ID
            );

            -- Mise à jour de l’échéance
            EXEC SP_ECHEANCE_CALCULER @ID_ECHEANCE;

            SET @MONTANT_RESTANT = @MONTANT_RESTANT - @MONTANT_A_REGLER;

            FETCH NEXT FROM echeances_cursor INTO @ID_ECHEANCE, @COD_DEVISE, @SOLDE_REGLEMENT;
        END

        CLOSE echeances_cursor;
        DEALLOCATE echeances_cursor;

        SET @RESULT_OUTPUT = 1; -- Succès
    END TRY
    BEGIN CATCH
        DECLARE @PARAMS_LOG NVARCHAR(MAX);

        SET @PARAMS_LOG =
            'INVOICE_ID=' + ISNULL(@INVOICE_ID, 'NULL') +
            ', TRANSACTION_ID=' + ISNULL(CONVERT(VARCHAR, @TRANSACTION_ID), 'NULL') +
            ', MONTANT=' + ISNULL(CONVERT(VARCHAR, @MONTANT), 'NULL') +
            ', DATE_REGLEMENT=' + ISNULL(@DATE_REGLEMENT, 'NULL');

        EXEC SP_INSERT_ERROR_LOG
            @PROCEDURE_NAME = 'SP_PENNYLANE_SUPPLIER_INVOICE_CREER_REGLEMENT',
            @PARAMETERS = @PARAMS_LOG;

        SET @RESULT_OUTPUT = -99;
    END CATCH
END
GO

