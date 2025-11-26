if exists (select * from sys.objects where object_id = object_id(N'[SP_LOG_PURGER]') and is_ms_shipped = 0 and [type] in ('P'))
drop procedure [SP_LOG_PURGER]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procedure stockee : SP_LOG_PURGER

No Version : 001

Description :
Purge des logs de l’application (ex : suppression des logs anciens).

Procedure appelee par :
Application FactureX via scheduler + JPA repository.

Historique des mises a jour :

> v001 - VABE - 26/11/2025 - Creation

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_LOG_PURGER]
AS
IF exists (select * from sysobjects
           where id = object_id(N'[spe_SP_LOG_PURGER]')
             and OBJECTPROPERTY(id, N'IsProcedure') = 1)
BEGIN
    EXEC spe_SP_LOG_PURGER
END
ELSE
BEGIN

    -- Exemple : purge des logs de plus de 30 jours
    DELETE LOG
    WHERE DATE_CREATION < DATEADD(day, -30, GETDATE());

END
GO
