if exists (select * from sys.objects where object_id = object_id(N'[SP_PENNYLANE_TRAITER_PIECE]') and is_ms_shipped = 0 and [type] in ('P'))
drop procedure [SP_PENNYLANE_TRAITER_PIECE]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procedure stockee : SP_PENNYLANE_TRAITER_PIECE

No Version : 001

Description :

Procedure appelee par :
Interface PENNYLANE.

Historique des mises a jour :

> v001 - VABE - 22/11/2024 - Creation

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_PENNYLANE_TRAITER_PIECE]
    @NO_ECRITURE_PIECE INT,
    @MESSAGE varchar(MAX),
    @SUCCES bit
AS
IF exists (select * from sysobjects
where id = object_id(N'[spe_SP_PENNYLANE_TRAITER_PIECE]')
and OBJECTPROPERTY(id, N'IsProcedure') = 1)
BEGIN
	EXEC spe_SP_PENNYLANE_TRAITER_PIECE
END
ELSE
BEGIN

UPDATE ECRITURE_PIECE
set
	MODIF_LE = GETDATE(),
	MODIF_PAR = 'PENNYLANE'
where NO_ECRITURE_PIECE = @NO_ECRITURE_PIECE

END

GO

