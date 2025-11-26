IF EXISTS (
    SELECT 1
    FROM sys.objects
    WHERE object_id = OBJECT_ID(N'[SP_PENNYLANE_AJOUT_FORUM_LIGNE]')
      AND is_ms_shipped = 0
      AND [type] = 'P'
)
DROP PROCEDURE [SP_PENNYLANE_AJOUT_FORUM_LIGNE];
GO

SET QUOTED_IDENTIFIER ON;
GO
SET ANSI_NULLS ON;
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procédure stockée : SP_PENNYLANE_AJOUT_FORUM_LIGNE
Version : 001

Description :
Procédure permettant la synchronisation des marquages pour l'interface PENNYLANE.

Historique des mises à jour :
> v001 - VABE - 22/11/2024 - Création

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_PENNYLANE_AJOUT_FORUM_LIGNE]
    @ENTITE        VARCHAR(100),
    @NO_ENTITE     VARCHAR(100),
    @MESSAGE       VARCHAR(MAX),
    @NIVEAU        INT = 5
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @NO_FORUM INT;
    DECLARE @_NO_ENTITE INT;

    -- 1. Détermination de la valeur réelle de NO_ENTITE en fonction de l'entité
    IF @ENTITE = 'V_FACTURE'
    BEGIN
        SELECT TOP 1 @_NO_ENTITE = NO_V_FACTURE
        FROM V_FACTURE
        WHERE PENNYLANE_ID = @NO_ENTITE;
    END
    ELSE IF @ENTITE = 'A_FACTURE'
    BEGIN
        SELECT TOP 1 @_NO_ENTITE = NO_A_FACTURE
        FROM A_FACTURE
        WHERE PENNYLANE_ID = @NO_ENTITE;
    END
    ELSE
    BEGIN
        SET @_NO_ENTITE = NULL;
    END

    -- 2. Recherche du forum existant en fonction de l'entité
    SELECT @NO_FORUM =
        CASE
            WHEN @ENTITE = 'V_FACTURE' THEN F.NO_FORUM
            WHEN @ENTITE = 'A_FACTURE' THEN F.NO_FORUM
            ELSE NULL
        END
    FROM FORUM F
    WHERE (@ENTITE = 'V_FACTURE' AND F.NO_V_FACTURE = @_NO_ENTITE)
       OR (@ENTITE = 'A_FACTURE' AND F.NO_A_FACTURE = @_NO_ENTITE);

    -- 3. Création du forum s’il n’existe pas
    IF @NO_FORUM IS NULL
    BEGIN
        EXEC sp_COMPTEUR 'NO_FORUM', @NO_FORUM OUTPUT;

        INSERT INTO FORUM (
            NO_FORUM,
            NO_V_FACTURE,
            NO_A_FACTURE,
            CREER_LE,
            CREER_PAR
        )
        VALUES (
            @NO_FORUM,
            CASE WHEN @ENTITE = 'V_FACTURE' THEN @_NO_ENTITE ELSE NULL END,
            CASE WHEN @ENTITE = 'A_FACTURE' THEN @_NO_ENTITE ELSE NULL END,
            GETDATE(),
            'PENNYLANE'
        );
    END

    -- 4. Ajout de la ligne de message dans le forum
    DECLARE @NO_FORUM_LIGNE INT;
    EXEC sp_COMPTEUR 'NO_FORUM_LIGNE', @NO_FORUM_LIGNE OUTPUT;

    INSERT INTO FORUM_LIGNE (
        NO_FORUM_LIGNE,
        NO_FORUM,
        CREER_LE,
        CREER_PAR,
        COD_USER,
        TYPE_MESSAGE,
        NIVEAU,
        CONTENU_MESSAGE
    )
    VALUES (
        @NO_FORUM_LIGNE,
        @NO_FORUM,
        GETDATE(),
        'PENNYLANE',
        'PENNYLANE',
        'message',
        @NIVEAU,
        @MESSAGE
    );
END;
GO
