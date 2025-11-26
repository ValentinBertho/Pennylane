if exists (select * from sys.objects where object_id = object_id(N'[SP_PENNYLANE_SUPPLIER_INVOICE_CREER]') and is_ms_shipped = 0 and [type] in ('P'))
drop procedure [SP_PENNYLANE_SUPPLIER_INVOICE_CREER]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procédure stockée : SP_PENNYLANE_SUPPLIER_INVOICE_CREER

No Version : 001

Description :

Procédure utilisée pour insérer une nouvelle facture fournisseur dans la table A_FACTURE
depuis les informations fournies par l'interface Pennylane.

Historique des mises à jour :

> v001 - VABE - 16/01/2025 - Création
> v002 - VABE - 20/03/2025 - Adaptation / corrections + ajout cartouche SPE

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_PENNYLANE_SUPPLIER_INVOICE_CREER]
    @NO_SOCIETE VARCHAR(50),
    @ID_PENNYLANE_FOURN varchar(MAX),
    @ID_PENNYLANE_FOURN_V2 varchar(MAX),
    @INVOICE_ID VARCHAR(50),
    @INVOICE_ID_V2 VARCHAR(50),
    @OBJET NVARCHAR(255),
    @DATE_FACTURE DATE,
    @COD_SITE VARCHAR(50),
    @COD_DIRECTION VARCHAR(50),
    @COD_AGENCE VARCHAR(50),
    @COD_ETAT VARCHAR(50),
    @TOTAL_HT VARCHAR(50),
    @TOTAL_TTC VARCHAR(50),
    @TOTAL_TVA VARCHAR(50),
    @INVOICE_NUMBER VARCHAR(50),
    @DEVISE VARCHAR(50),
    @NOM_SOCIETE VARCHAR(50),
    @IMPORT_MESSAGE VARCHAR(MAX),
    @RESULT_OUTPUT INT OUTPUT  -- Paramètre pour renvoyer le résultat

AS
IF exists (select * from sysobjects
where id = object_id(N'[spe_SP_PENNYLANE_SUPPLIER_INVOICE_CREER]')
and OBJECTPROPERTY(id, N'IsProcedure') = 1)
BEGIN
EXEC spe_SP_PENNYLANE_SUPPLIER_INVOICE_CREER
        @NO_SOCIETE,
        @ID_PENNYLANE_FOURN,
        @ID_PENNYLANE_FOURN_V2,
        @INVOICE_ID,
        @INVOICE_ID_V2,
        @OBJET,
        @DATE_FACTURE,
        @COD_SITE,
        @COD_DIRECTION,
        @COD_AGENCE,
        @COD_ETAT,
        @TOTAL_HT,
        @TOTAL_TTC,
        @TOTAL_TVA,
        @INVOICE_NUMBER,
        @DEVISE,
        @NOM_SOCIETE,
        @RESULT_OUTPUT OUTPUT;
END
ELSE
BEGIN
SET NOCOUNT ON;

    DECLARE @NO_A_FACTURE INT;
    DECLARE @COD_COM VARCHAR(50);
    DECLARE @COD_SERVICE VARCHAR(50);
    DECLARE @_COD_DIRECTION VARCHAR(50);
    DECLARE @_COD_AGENCE VARCHAR(50);
    DECLARE @COD_TYPE VARCHAR(50);
    DECLARE @_COD_ETAT VARCHAR(50);
    DECLARE @_COD_STATUT VARCHAR(50);
    DECLARE @_NO_INTERLO VARCHAR(50);
	DECLARE @_NO_SOCIETE INT;
	DECLARE @_COD_RGLT_FOUR VARCHAR(50);
	DECLARE @_NO_TIERS_PAYE INT;


    SET @NO_A_FACTURE = -1;

    BEGIN TRY
        -- Générez un nouveau numéro de facture
        EXEC sp_COMPTEUR 'NO_A_FACTURE', @NO_A_FACTURE OUTPUT;

        -- Récupérer les valeurs ou leurs valeurs par défaut si elles sont NULL
        SET @COD_COM = ISNULL(dbo.fn_PENNYLANE_COD_COM(), (SELECT DEFAULT_VALUE FROM PENNYLANE_DEFAULT_VALUES WHERE CODE = 'COD_COM'));
        SET @COD_SERVICE = ISNULL(dbo.fn_PENNYLANE_COD_SERVICE(), (SELECT DEFAULT_VALUE FROM PENNYLANE_DEFAULT_VALUES WHERE CODE = 'COD_SERVICE'));
        SET @_COD_DIRECTION = ISNULL(@COD_DIRECTION, (SELECT DEFAULT_VALUE FROM PENNYLANE_DEFAULT_VALUES WHERE CODE = 'COD_DIRECTION'));
        SET @_COD_AGENCE = ISNULL(@COD_AGENCE, (SELECT DEFAULT_VALUE FROM PENNYLANE_DEFAULT_VALUES WHERE CODE = 'COD_AGENCE'));
        SET @COD_TYPE = ISNULL(dbo.fn_PENNYLANE_COD_TYPE(), (SELECT DEFAULT_VALUE FROM PENNYLANE_DEFAULT_VALUES WHERE CODE = 'COD_TYPE'));
        SET @_COD_ETAT = ISNULL(@COD_ETAT, (SELECT DEFAULT_VALUE FROM PENNYLANE_DEFAULT_VALUES WHERE CODE = 'COD_ETAT'));
        SET @_COD_STATUT = (SELECT DEFAULT_VALUE FROM PENNYLANE_DEFAULT_VALUES WHERE CODE = 'COD_STATUT');
        SET @_NO_INTERLO = (SELECT DEFAULT_VALUE FROM PENNYLANE_DEFAULT_VALUES WHERE CODE = 'NO_INTERLO');
		SET @_NO_SOCIETE = (SELECT TOP 1 NO_SOCIETE FROM SOCIETE WHERE CODE_CPTA_FOUR = @NO_SOCIETE);
		SET @_COD_RGLT_FOUR = (SELECT TOP 1 COD_RGLT_FOUR FROM SOCIETE WHERE CODE_CPTA_FOUR = @NO_SOCIETE);
		SET @_NO_TIERS_PAYE = (SELECT TOP 1 NO_PAYEUR_FOUR FROM SOCIETE WHERE CODE_CPTA_FOUR = @NO_SOCIETE);

        -- Insérer une nouvelle facture dans A_FACTURE
        INSERT INTO A_FACTURE (
            NO_SOCIETE,
			NO_TIERS_PAYE,
            NO_A_FACTURE,
            OBJET,
            COD_COM,
            DATE_FACTURE,
            COD_SITE,
            COD_DIRECTION,
            COD_AGENCE,
            COD_SERVICE,
            COD_ETAT,
            COD_STATUT,
            COD_TYPE,
            C1,
            TOTALHT_CONTROLE,
            TOTALTTC_CONTROLE,
            TOTALTVA_CONTROLE,
            CREER_LE,
            CREER_PAR,
            NO_SITE,
            NO_INTERLO,
            PENNYLANE_ID,
            PENNYLANE_ID_V2,
            PARITE,
            COD_DEV_FOUR,
            COD_RGLT,
            M5
        )
        VALUES (
            @_NO_SOCIETE,
			@_NO_TIERS_PAYE,
            @NO_A_FACTURE,
            @OBJET,
            @COD_COM,
            @DATE_FACTURE,
            @COD_SITE,
            @_COD_DIRECTION,
            @_COD_AGENCE,
            @COD_SERVICE,
            1,
            @_COD_STATUT,
            @COD_TYPE,
			@INVOICE_NUMBER,
            @TOTAL_HT,
            @TOTAL_TTC,
            @TOTAL_TVA,
            GETDATE(),
            'PENNYLANE',
            '-1',
            @_NO_INTERLO,
            @INVOICE_ID,
            @INVOICE_ID_V2,
            1,
            @DEVISE,
            @_COD_RGLT_FOUR,
            @IMPORT_MESSAGE
        );

        -- Assigner la valeur générée au paramètre OUTPUT
        SET @RESULT_OUTPUT = @NO_A_FACTURE;

        EXEC SP_PENNYLANE_SYNCHRO_MARQUAGE
            @NO_ENTITE = @NO_A_FACTURE,
            @ENTITE = 'A_FACTURE',
            @INFO = 'CREATE',
            @REF_EXT = @INVOICE_ID;

    END TRY
    BEGIN CATCH
    -- En cas d'erreur, renvoyer -1
    SET @RESULT_OUTPUT = -1;

    -- Format JSON des paramètres importants pour le diagnostic
    DECLARE @PARAMS_JSON NVARCHAR(MAX) = CONCAT(
        '{',
        '"NO_SOCIETE":"', @NO_SOCIETE, '",',
        '"INVOICE_ID":"', @INVOICE_ID, '",',
        '"OBJET":"', REPLACE(ISNULL(@OBJET, ''), '"', '\"'), '"',
        '}'
    );

    -- Journaliser l'erreur
    EXEC dbo.SP_INSERT_ERROR_LOG
        @PROCEDURE_NAME = 'SP_PENNYLANE_SUPPLIER_INVOICE_CREER',
        @PARAMETERS = @PARAMS_JSON;
    END CATCH
END;

