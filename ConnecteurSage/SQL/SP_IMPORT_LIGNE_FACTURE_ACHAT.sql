IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[SP_IMPORT_LIGNE_FACTURE_ACHAT]') AND is_ms_shipped = 0 AND [type] IN ('P'))
    DROP PROCEDURE [SP_IMPORT_LIGNE_FACTURE_ACHAT]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procédure stockée : SP_IMPORT_LIGNE_FACTURE_ACHAT

Version : 001

Description :
Importe une ligne de facture d'achat depuis Sage vers Athénéo

Procédure appelée par :
Interface SAGE - Import factures achats

Historique des mises à jour :

> v001 - VABE - 04/12/2025 - Création

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_IMPORT_LIGNE_FACTURE_ACHAT]
    @NO_A_FACTURE INT,
    @NO_LIGNE INT,
    @TYPE_LIGNE VARCHAR(20),
    @COD_PROD VARCHAR(50),
    @DES_COM NVARCHAR(500),
    @TAUX_TAXE DECIMAL(5,2),
    @QTE_FAC DECIMAL(18,3),
    @PUVB DECIMAL(18,6),
    @PU_NET DECIMAL(18,6),
    @TOTAL_HT DECIMAL(18,2),
    @COMPTE_COMPTABLE VARCHAR(20) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT * FROM sysobjects
        WHERE id = OBJECT_ID(N'[spe_SP_IMPORT_LIGNE_FACTURE_ACHAT]')
        AND OBJECTPROPERTY(id, N'IsProcedure') = 1
    )
    BEGIN
        EXEC spe_SP_IMPORT_LIGNE_FACTURE_ACHAT
            @NO_A_FACTURE,
            @NO_LIGNE,
            @TYPE_LIGNE,
            @COD_PROD,
            @DES_COM,
            @TAUX_TAXE,
            @QTE_FAC,
            @PUVB,
            @PU_NET,
            @TOTAL_HT,
            @COMPTE_COMPTABLE
    END
    ELSE
    BEGIN
        -- Vérification si la ligne existe déjà
        IF EXISTS (
            SELECT 1 FROM A_L_FACTURE
            WHERE NO_A_FACTURE = @NO_A_FACTURE
            AND NO_LIGNE = @NO_LIGNE
        )
        BEGIN
            -- Mise à jour de la ligne existante
            UPDATE A_L_FACTURE
            SET
                TYPE_LIGNE = @TYPE_LIGNE,
                COD_PROD = @COD_PROD,
                DES_COM = @DES_COM,
                TAUX_TAXE = @TAUX_TAXE,
                QTE_FAC = @QTE_FAC,
                PUVB = @PUVB,
                PU_NET = @PU_NET,
                TOTAL_HT = @TOTAL_HT,
                COMPTE_COMPTABLE = @COMPTE_COMPTABLE
            WHERE NO_A_FACTURE = @NO_A_FACTURE
            AND NO_LIGNE = @NO_LIGNE;
        END
        ELSE
        BEGIN
            -- Insertion d'une nouvelle ligne
            INSERT INTO A_L_FACTURE (
                NO_A_FACTURE,
                NO_LIGNE,
                TYPE_LIGNE,
                COD_PROD,
                DES_COM,
                TAUX_TAXE,
                QTE_FAC,
                PUVB,
                PU_NET,
                TOTAL_HT,
                COMPTE_COMPTABLE
            )
            VALUES (
                @NO_A_FACTURE,
                @NO_LIGNE,
                @TYPE_LIGNE,
                @COD_PROD,
                @DES_COM,
                @TAUX_TAXE,
                @QTE_FAC,
                @PUVB,
                @PU_NET,
                @TOTAL_HT,
                @COMPTE_COMPTABLE
            );
        END
    END
END
GO
