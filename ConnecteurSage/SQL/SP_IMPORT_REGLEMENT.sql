IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[SP_IMPORT_REGLEMENT]') AND is_ms_shipped = 0 AND [type] IN ('P'))
    DROP PROCEDURE [SP_IMPORT_REGLEMENT]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procédure stockée : SP_IMPORT_REGLEMENT

Version : 001

Description :
Importe un règlement depuis Sage vers Athénéo

Procédure appelée par :
Interface SAGE - Import règlements

Historique des mises à jour :

> v001 - VABE - 04/12/2025 - Création

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_IMPORT_REGLEMENT]
    @NUMERO_REGLEMENT VARCHAR(50),
    @NUMERO_FACTURE VARCHAR(50),
    @CODE_TIERS VARCHAR(50),
    @NOM_TIERS NVARCHAR(255),
    @DATE_REGLEMENT DATETIME,
    @MONTANT_REGLEMENT DECIMAL(18,2),
    @MODE_REGLEMENT VARCHAR(50),
    @REFERENCE_REGLEMENT VARCHAR(100),
    @COMPTE_COMPTABLE VARCHAR(20),
    @JOURNAL VARCHAR(10),
    @TYPE_REGLEMENT VARCHAR(20),
    @STATUT VARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT * FROM sysobjects
        WHERE id = OBJECT_ID(N'[spe_SP_IMPORT_REGLEMENT]')
        AND OBJECTPROPERTY(id, N'IsProcedure') = 1
    )
    BEGIN
        EXEC spe_SP_IMPORT_REGLEMENT
            @NUMERO_REGLEMENT,
            @NUMERO_FACTURE,
            @CODE_TIERS,
            @NOM_TIERS,
            @DATE_REGLEMENT,
            @MONTANT_REGLEMENT,
            @MODE_REGLEMENT,
            @REFERENCE_REGLEMENT,
            @COMPTE_COMPTABLE,
            @JOURNAL,
            @TYPE_REGLEMENT,
            @STATUT
    END
    ELSE
    BEGIN
        DECLARE @NO_REGLEMENT INT;
        DECLARE @NO_FACTURE INT = NULL;

        -- Recherche du numéro de facture interne si référence fournie
        IF @TYPE_REGLEMENT = 'Fournisseur'
        BEGIN
            SELECT TOP 1 @NO_FACTURE = NO_A_FACTURE
            FROM A_FACTURE
            WHERE REFERENCE_EXTERNE = @NUMERO_FACTURE
               OR CHRONO_A_FACTURE = @NUMERO_FACTURE;
        END
        ELSE IF @TYPE_REGLEMENT = 'Client'
        BEGIN
            SELECT TOP 1 @NO_FACTURE = NO_V_FACTURE
            FROM V_FACTURE
            WHERE CHRONO_V_FACTURE = @NUMERO_FACTURE;
        END

        -- Vérification si le règlement existe déjà
        IF EXISTS (
            SELECT 1 FROM REGLEMENT
            WHERE NUMERO_REGLEMENT_EXTERNE = @NUMERO_REGLEMENT
        )
        BEGIN
            -- Mise à jour du règlement existant
            UPDATE REGLEMENT
            SET
                DATE_REGLEMENT = @DATE_REGLEMENT,
                MONTANT = @MONTANT_REGLEMENT,
                MODE_REGLEMENT = @MODE_REGLEMENT,
                REFERENCE = @REFERENCE_REGLEMENT,
                STATUT = @STATUT,
                DATE_MODIFICATION = GETDATE()
            WHERE NUMERO_REGLEMENT_EXTERNE = @NUMERO_REGLEMENT;

            SELECT @NO_REGLEMENT = NO_REGLEMENT
            FROM REGLEMENT
            WHERE NUMERO_REGLEMENT_EXTERNE = @NUMERO_REGLEMENT;

            SELECT @NO_REGLEMENT AS NO_REGLEMENT, 'UPDATE' AS OPERATION;
        END
        ELSE
        BEGIN
            -- Insertion d'un nouveau règlement
            INSERT INTO REGLEMENT (
                NUMERO_REGLEMENT_EXTERNE,
                NO_FACTURE,
                TYPE_FACTURE,
                CODE_TIERS,
                NOM_TIERS,
                DATE_REGLEMENT,
                MONTANT,
                MODE_REGLEMENT,
                REFERENCE,
                COMPTE_COMPTABLE,
                JOURNAL,
                STATUT,
                DATE_CREATION,
                SOURCE_IMPORT
            )
            VALUES (
                @NUMERO_REGLEMENT,
                @NO_FACTURE,
                @TYPE_REGLEMENT,
                @CODE_TIERS,
                @NOM_TIERS,
                @DATE_REGLEMENT,
                @MONTANT_REGLEMENT,
                @MODE_REGLEMENT,
                @REFERENCE_REGLEMENT,
                @COMPTE_COMPTABLE,
                @JOURNAL,
                @STATUT,
                GETDATE(),
                'SAGE'
            );

            SET @NO_REGLEMENT = SCOPE_IDENTITY();

            SELECT @NO_REGLEMENT AS NO_REGLEMENT, 'INSERT' AS OPERATION;
        END

        -- Mise à jour du statut de paiement de la facture si applicable
        IF @NO_FACTURE IS NOT NULL
        BEGIN
            IF @TYPE_REGLEMENT = 'Fournisseur'
            BEGIN
                UPDATE A_FACTURE
                SET COD_ETAT_PDP = CASE
                    WHEN @MONTANT_REGLEMENT >= NETAPAYER THEN 'PAYE'
                    ELSE 'PARTIEL'
                END
                WHERE NO_A_FACTURE = @NO_FACTURE;
            END
            ELSE IF @TYPE_REGLEMENT = 'Client'
            BEGIN
                UPDATE V_FACTURE
                SET COD_ETAT_PDP = CASE
                    WHEN @MONTANT_REGLEMENT >= NETAPAYER THEN 'PAYE'
                    ELSE 'PARTIEL'
                END
                WHERE NO_V_FACTURE = @NO_FACTURE;
            END
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
            'REGLEMENT',
            @NO_REGLEMENT,
            'SYNCHRONISE',
            GETDATE(),
            'Import depuis Sage - Ref: ' + @NUMERO_REGLEMENT
        );
    END
END
GO
