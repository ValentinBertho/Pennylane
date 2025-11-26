if exists (select * from sys.objects where object_id = object_id(N'[SP_PENNYLANE_SUPPLIER_INVOICE_MAJ]') and is_ms_shipped = 0 and [type] in ('P'))
drop procedure [SP_PENNYLANE_SUPPLIER_INVOICE_MAJ]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procedure stockee : SP_PENNYLANE_SUPPLIER_INVOICE_MAJ

No Version : 001

Description :

Procedure appelee par :
Interface PENNYLANE.

Historique des mises a jour :

> v001 - VABE - 22/11/2024 - Creation

//////////////////////////////////////////////////////////////////////// */


CREATE PROCEDURE [SP_PENNYLANE_SUPPLIER_INVOICE_MAJ]
    @NO_SOCIETE VARCHAR(50),
    @INVOICE_ID VARCHAR(50),
    @OBJET NVARCHAR(255),
    @DATE_FACTURE DATE,
    @COD_SITE VARCHAR(50),
    @TOTAL_HT VARCHAR(50),
    @TOTAL_TTC VARCHAR(50),
    @TOTAL_TVA VARCHAR(50),
    @INVOICE_NUMBER VARCHAR(50),
    @NOM_SOCIETE VARCHAR(50),
    @RESULT_OUTPUT INT OUTPUT  -- Paramètre pour renvoyer le résultat
AS
IF exists (select * from sysobjects
where id = object_id(N'[spe_SP_PENNYLANE_SUPPLIER_INVOICE_MAJ]')
and OBJECTPROPERTY(id, N'IsProcedure') = 1)
BEGIN
EXEC spe_SP_PENNYLANE_SUPPLIER_INVOICE_MAJ
    @INVOICE_ID,
    @OBJET,
    @DATE_FACTURE,
    @COD_SITE,
    @TOTAL_HT,
    @TOTAL_TTC,
    @TOTAL_TVA,
    @INVOICE_NUMBER,
    @NOM_SOCIETE,
    @RESULT_OUTPUT OUTPUT;
END
ELSE
BEGIN
    SET NOCOUNT ON;
    DECLARE @NO_A_FACTURE INT;
	DECLARE @_NO_SOCIETE INT;
	DECLARE @_NO_TIERS_PAYE INT;
	SET @NO_A_FACTURE = (SELECT NO_A_FACTURE FROM A_FACTURE WHERE PENNYLANE_ID = @INVOICE_ID AND COD_ETAT < 2);
	SET @_NO_SOCIETE = (SELECT TOP 1 NO_SOCIETE FROM SOCIETE WHERE CODE_CPTA_FOUR = @NO_SOCIETE);
	SET @_NO_TIERS_PAYE = (SELECT TOP 1 NO_PAYEUR_FOUR FROM SOCIETE WHERE CODE_CPTA_FOUR = @NO_SOCIETE);


    BEGIN TRY
        -- Vérifier si la facture existe avant de la mettre à jour
        IF EXISTS (SELECT 1 FROM A_FACTURE WHERE PENNYLANE_ID = @INVOICE_ID)
        BEGIN
            UPDATE A_FACTURE
            SET OBJET = @OBJET,
                DATE_FACTURE = @DATE_FACTURE,
                COD_SITE = @COD_SITE,
                TOTALHT_CONTROLE = @TOTAL_HT,
                TOTALTTC_CONTROLE = @TOTAL_TTC,
                TOTALTVA_CONTROLE = @TOTAL_TVA,
                MODIF_LE = GETDATE(),
                MODIF_PAR = 'PENNYLANE',
                C1 = @INVOICE_NUMBER,
                NO_SOCIETE = @_NO_SOCIETE,
                NO_TIERS_PAYE = @_NO_TIERS_PAYE
            WHERE PENNYLANE_ID = @INVOICE_ID;

            -- Vérifier si la mise à jour a bien été effectuée
            IF @@ROWCOUNT > 0
            BEGIN
                SET @RESULT_OUTPUT = 1; -- Succès

                EXEC SP_PENNYLANE_SYNCHRO_MARQUAGE
                    @NO_ENTITE = @NO_A_FACTURE,
                    @ENTITE = 'A_FACTURE',
                    @INFO = 'MAJ',
                    @REF_EXT = @INVOICE_ID;
            END
            ELSE
            BEGIN
                SET @RESULT_OUTPUT = -1; -- Aucune ligne mise à jour
            END
        END
        ELSE
        BEGIN
            SET @RESULT_OUTPUT = -2; -- Facture non trouvée
        END
    END TRY
    BEGIN CATCH
        SET @RESULT_OUTPUT = -99; -- Erreur inconnue
    END CATCH
END;


