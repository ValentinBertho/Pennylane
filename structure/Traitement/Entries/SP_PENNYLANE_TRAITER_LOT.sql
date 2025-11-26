if exists (select * from sys.objects where object_id = object_id(N'[SP_PENNYLANE_TRAITER_LOT]') and is_ms_shipped = 0 and [type] in ('P'))
drop procedure [SP_PENNYLANE_TRAITER_LOT]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procedure stockee : SP_PENNYLANE_TRAITER_LOT

No Version : 001

Description :

Procedure appelee par :
Interface PENNYLANE.

Historique des mises a jour :

> v001 - VABE - 22/11/2024 - Creation

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_PENNYLANE_TRAITER_LOT]
    @NO_ECRITURE_LOT INT,
    @MESSAGE varchar(MAX),
    @SUCCES bit

AS
IF exists (select * from sysobjects
where id = object_id(N'[spe_SP_PENNYLANE_TRAITER_LOT]')
and OBJECTPROPERTY(id, N'IsProcedure') = 1)
BEGIN
	EXEC spe_SP_PENNYLANE_TRAITER_LOT @NO_ECRITURE_LOT , @MESSAGE, @SUCCES
END
ELSE
BEGIN

UPDATE ECRITURE_LOT
SET MODIF_LE = GETDATE(),
    MODIF_PAR = 'PENNYLANE',
    EXPORTER_LE = GETDATE(),
    EXPORTER = @SUCCES,
    MEMO = @MESSAGE,
    RETRY_COUNT = CASE WHEN @SUCCES = 0 THEN RETRY_COUNT + 1 ELSE 0 END,
    PENNYLANE_STATUS = CASE WHEN @SUCCES = 0 THEN 'ERREUR' ELSE 'SUCCES' END
WHERE NO_ECRITURE_LOT = @NO_ECRITURE_LOT



END

GO

