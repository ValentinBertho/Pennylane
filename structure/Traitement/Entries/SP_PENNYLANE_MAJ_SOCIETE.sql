if exists (select * from sys.objects where object_id = object_id(N'[SP_PENNYLANE_MAJ_SOCIETE]') and is_ms_shipped = 0 and [type] in ('P'))
drop procedure [SP_PENNYLANE_MAJ_SOCIETE]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procedure stockee : SP_PENNYLANE_MAJ_SOCIETE

No Version : 001

Description :

Procedure appelee par :
Interface PENNYLANE.

Historique des mises a jour :

> v001 - VABE - 22/11/2024 - Creation

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_PENNYLANE_MAJ_SOCIETE]
    @CODE_COMPTABLE varchar(MAX),
    @ID_PENNYLANE varchar(MAX),
    @ID_PENNYLANE_V2 varchar(MAX),
    @COD_SITE varchar(50)
AS
IF exists (select * from sysobjects
where id = object_id(N'[spe_SP_PENNYLANE_MAJ_SOCIETE]')
and OBJECTPROPERTY(id, N'IsProcedure') = 1)
BEGIN
	EXEC spe_SP_PENNYLANE_MAJ_SOCIETE @CODE_COMPTABLE, @ID_PENNYLANE, @ID_PENNYLANE_V2
END
ELSE
BEGIN

DECLARE @NO_SOCIETE INT;

-- Récupérer NO_SOCIETE depuis SOCIETE via @CODE_COMPTABLE
SELECT @NO_SOCIETE = NO_SOCIETE
FROM SOCIETE
WHERE CODE_CPTA = @CODE_COMPTABLE;

IF @NO_SOCIETE IS NULL
BEGIN
    RAISERROR('Code comptable inconnu: %s', 16, 1, @CODE_COMPTABLE);
    RETURN;
END

-- Vérifier si un enregistrement existe déjà dans SOCIETE_EXPORT avec ce NO_SOCIETE et @COD_SITE (ici COD_SITE à adapter si nécessaire)
IF EXISTS (SELECT 1 FROM SOCIETE_EXPORT WHERE NO_SOCIETE = @NO_SOCIETE AND COD_SITE = @COD_SITE)
BEGIN
    -- Mise à jour du champ ID_PENNYLANE
    UPDATE SOCIETE_EXPORT
    SET PENNYLANE_ID = @ID_PENNYLANE
    WHERE NO_SOCIETE = @NO_SOCIETE
      AND COD_SITE = @COD_SITE;
END
ELSE
BEGIN
    -- Insertion d'une nouvelle ligne
    INSERT INTO SOCIETE_EXPORT (NO_SOCIETE, COD_SITE, PENNYLANE_ID)
    VALUES (@NO_SOCIETE, @COD_SITE, @ID_PENNYLANE);
END

--UPDATE SOCIETE
--set
--	MODIF_LE = GETDATE(),
--	MODIF_PAR = 'PENNYLANE',
--	PENNYLANE_ID = @ID_PENNYLANE,
--	PENNYLANE_ID_V2 = @ID_PENNYLANE_V2
-- where CODE_CPTA = @CODE_COMPTABLE

END

GO

