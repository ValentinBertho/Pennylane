if exists (select * from sys.objects where object_id = object_id(N'[SP_PENNYLANE_SYNCHRO_MARQUAGE]') and is_ms_shipped = 0 and [type] in ('P'))
drop procedure [SP_PENNYLANE_SYNCHRO_MARQUAGE]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procédure stockée : SP_PENNYLANE_SYNCHRO_MARQUAGE

No Version : 001

Description :

Procédure permettant la synchronisation des marquages pour l'interface PENNYLANE.

Historique des mises à jour :

> v001 - VABE - 22/11/2024 - Création

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_PENNYLANE_SYNCHRO_MARQUAGE]
    @NO_ENTITE varchar(MAX),
    @ENTITE varchar(MAX),
    @INFO varchar(MAX),
    @REF_EXT varchar(MAX)
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (SELECT * FROM sys.objects
               WHERE object_id = OBJECT_ID(N'[spe_SP_PENNYLANE_SYNCHRO_MARQUAGE]')
               AND type = 'P')
    BEGIN
        EXEC spe_SP_PENNYLANE_SYNCHRO_MARQUAGE @NO_ENTITE, @ENTITE, @INFO, @REF_EXT;
    END
    ELSE
    BEGIN
        DECLARE @NO_SYNCHRO_MARQUAGE INT;
        SET @NO_SYNCHRO_MARQUAGE = -1;

        IF @INFO = 'MAJ' or @INFO = 'CREATE'
        BEGIN
            -- Vérification de l'existence de l'entité dans la table SYNCHRO_MARQUAGE
            IF EXISTS (SELECT 1 FROM SYNCHRO_MARQUAGE WHERE NO_ENTITE = @NO_ENTITE AND NOM_ENTITE = @ENTITE)
            BEGIN
                -- Mise à jour des informations si l'enregistrement existe
                UPDATE SYNCHRO_MARQUAGE
                SET REF_EXT = @REF_EXT, DATE_SYNCHRO = GETDATE(), COD_STATUT_SYNCHRO_MARQUAGE = 'SYNCHRONISE', INFO = 'MAJ',
                    MODIF_LE = GETDATE(), MODIF_PAR = 'INTERFACE_PENNYLANE'
                WHERE NO_ENTITE = @NO_ENTITE AND NOM_ENTITE = @ENTITE;
            END
            ELSE
            BEGIN

                EXEC sp_COMPTEUR 'NO_SYNCHRO_MARQUAGE', @NO_SYNCHRO_MARQUAGE OUTPUT;

                -- Insertion d'un nouvel enregistrement si l'entité n'existe pas
                INSERT INTO SYNCHRO_MARQUAGE (NO_SYNCHRO_MARQUAGE, NO_ENTITE, NOM_ENTITE, REF_EXT, DATE_SYNCHRO, COD_STATUT_SYNCHRO_MARQUAGE,
                INFO, CREER_LE, CREER_PAR)
                VALUES (@NO_SYNCHRO_MARQUAGE, @NO_ENTITE, @ENTITE, @REF_EXT, GETDATE(), 'SYNCHRONISE', 'Création', GETDATE(), 'INTERFACE_PENNYLANE');
            END
        END
    END
END
GO
