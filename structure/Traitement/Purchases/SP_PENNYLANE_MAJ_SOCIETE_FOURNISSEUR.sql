if exists (select * from sys.objects where object_id = object_id(N'[SP_PENNYLANE_MAJ_SOCIETE_FOURNISSEUR]') and is_ms_shipped = 0 and [type] in ('P'))
drop procedure [SP_PENNYLANE_MAJ_SOCIETE_FOURNISSEUR]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procedure stockee : SP_PENNYLANE_MAJ_SOCIETE_FOURNISSEUR

No Version : 001

Description :

Procedure appelee par :
Interface PENNYLANE.

Historique des mises a jour :

> v001 - VABE - 22/11/2024 - Creation

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_PENNYLANE_MAJ_SOCIETE_FOURNISSEUR]
    @CODE_COMPTABLE_FOUR varchar(MAX),
    @ID_PENNYLANE varchar(MAX),
    @ID_PENNYLANE_V2 varchar(MAX)
AS
IF exists (select * from sysobjects
where id = object_id(N'[spe_SP_PENNYLANE_MAJ_SOCIETE_FOURNISSEUR]')
and OBJECTPROPERTY(id, N'IsProcedure') = 1)
BEGIN
	EXEC spe_SP_PENNYLANE_MAJ_SOCIETE_FOURNISSEUR     @CODE_COMPTABLE_FOUR,
                                                      @ID_PENNYLANE,
                                                      @ID_PENNYLANE_V2
END
ELSE
BEGIN

UPDATE SOCIETE
set
	MODIF_LE = GETDATE(),
	MODIF_PAR = 'PENNYLANE',
	PENNYLANE_ID = @ID_PENNYLANE,
	PENNYLANE_ID_V2 = @ID_PENNYLANE_V2
where CODE_CPTA = @CODE_COMPTABLE_FOUR

END

GO

