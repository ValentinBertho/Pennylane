IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[SP_IMPORT_FACTURE_ACHAT]') AND is_ms_shipped = 0 AND [type] IN ('P'))
    DROP PROCEDURE [SP_IMPORT_FACTURE_ACHAT]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procédure stockée : SP_IMPORT_FACTURE_ACHAT

Version : 001

Description :
Importe une facture d'achat depuis Sage vers Athénéo (table A_FACTURE)

Procédure appelée par :
Interface SAGE - Import factures achats

Historique des mises à jour :

> v001 - VABE - 04/12/2025 - Création

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_IMPORT_FACTURE_ACHAT]
    @CHRONO_A_FACTURE VARCHAR(50),
    @COD_ETAT VARCHAR(10),
    @DATE_FACTURE DATETIME,
    @MTT_HT DECIMAL(18,2),
    @MTT_TTC DECIMAL(18,2),
    @NET_A_PAYER DECIMAL(18,2),
    @OBJET NVARCHAR(255),
    @DATE_ECHEANCE DATETIME,
    @CODE_FOURNISSEUR VARCHAR(50),
    @NOM_FOURNISSEUR NVARCHAR(255),
    @REFERENCE_EXTERNE VARCHAR(100),
    @REFERENCE_FOURNISSEUR VARCHAR(100)
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT * FROM sysobjects
        WHERE id = OBJECT_ID(N'[spe_SP_IMPORT_FACTURE_ACHAT]')
        AND OBJECTPROPERTY(id, N'IsProcedure') = 1
    )
    BEGIN
        EXEC spe_SP_IMPORT_FACTURE_ACHAT
            @CHRONO_A_FACTURE,
            @COD_ETAT,
            @DATE_FACTURE,
            @MTT_HT,
            @MTT_TTC,
            @NET_A_PAYER,
            @OBJET,
            @DATE_ECHEANCE,
            @CODE_FOURNISSEUR,
            @NOM_FOURNISSEUR,
            @REFERENCE_EXTERNE,
            @REFERENCE_FOURNISSEUR
    END
    ELSE
    BEGIN
        DECLARE @NO_A_FACTURE INT;

        -- Vérification si la facture existe déjà (par référence externe Sage)
        IF EXISTS (
            SELECT 1 FROM A_FACTURE
            WHERE REFERENCE_EXTERNE = @REFERENCE_EXTERNE
        )
        BEGIN
            -- Mise à jour de la facture existante
            UPDATE A_FACTURE
            SET
                COD_ETAT = @COD_ETAT,
                DATE_FACTURE = @DATE_FACTURE,
                MTT_HT = @MTT_HT,
                MTT_TTC = @MTT_TTC,
                NETAPAYER = @NET_A_PAYER,
                OBJET = @OBJET,
                DATE_ECHEANCE = @DATE_ECHEANCE,
                CODE_FOURNISSEUR = @CODE_FOURNISSEUR,
                NOM_FOURNISSEUR = @NOM_FOURNISSEUR,
                REFERENCE_FOURNISSEUR = @REFERENCE_FOURNISSEUR,
                DATE_MODIFICATION = GETDATE()
            WHERE REFERENCE_EXTERNE = @REFERENCE_EXTERNE;

            SELECT @NO_A_FACTURE = NO_A_FACTURE
            FROM A_FACTURE
            WHERE REFERENCE_EXTERNE = @REFERENCE_EXTERNE;

            SELECT @NO_A_FACTURE AS NO_A_FACTURE, 'UPDATE' AS OPERATION;
        END
        ELSE
        BEGIN
            -- Insertion d'une nouvelle facture
            INSERT INTO A_FACTURE (
                CHRONO_A_FACTURE,
                COD_ETAT,
                DATE_FACTURE,
                MTT_HT,
                MTT_TTC,
                NETAPAYER,
                OBJET,
                DATE_ECHEANCE,
                CODE_FOURNISSEUR,
                NOM_FOURNISSEUR,
                REFERENCE_EXTERNE,
                REFERENCE_FOURNISSEUR,
                DATE_CREATION,
                SOURCE_IMPORT
            )
            VALUES (
                @CHRONO_A_FACTURE,
                @COD_ETAT,
                @DATE_FACTURE,
                @MTT_HT,
                @MTT_TTC,
                @NET_A_PAYER,
                @OBJET,
                @DATE_ECHEANCE,
                @CODE_FOURNISSEUR,
                @NOM_FOURNISSEUR,
                @REFERENCE_EXTERNE,
                @REFERENCE_FOURNISSEUR,
                GETDATE(),
                'SAGE'
            );

            SET @NO_A_FACTURE = SCOPE_IDENTITY();

            SELECT @NO_A_FACTURE AS NO_A_FACTURE, 'INSERT' AS OPERATION;
        END

        -- Log de l'opération
        INSERT INTO SYNCHRO_MARQUAGE (
            NOM_ENTITE,
            NO_ENTITE,
            COD_STATUT_SYNCHRO_MARQUAGE,
            DATE_SYNCHRO,
            INFO
        )
        VALUES (
            'A_FACTURE',
            @NO_A_FACTURE,
            'SYNCHRONISE',
            GETDATE(),
            'Import depuis Sage - Ref: ' + @REFERENCE_EXTERNE
        );
    END
END
GO
