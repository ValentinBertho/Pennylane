if exists (select * from sys.objects where object_id = object_id(N'[SP_PENNYLANE_MAJ_PRODUITS]') and is_ms_shipped = 0 and [type] in ('P'))
drop procedure [SP_PENNYLANE_MAJ_PRODUITS]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procedure stockee : SP_PENNYLANE_MAJ_PRODUITS

No Version : 001

Description :

Procedure appelee par :
Interface PENNYLANE.

Historique des mises a jour :

> v001 - VABE - 13/06/2025 - Creation

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_PENNYLANE_MAJ_PRODUITS]
    @NO_PRODUIT int,
    @ID_PENNYLANE varchar(MAX)
AS
IF exists (select * from sysobjects
where id = object_id(N'[spe_SP_PENNYLANE_MAJ_PRODUITS]')
and OBJECTPROPERTY(id, N'IsProcedure') = 1)
BEGIN
	EXEC spe_SP_PENNYLANE_MAJ_PRODUITS @NO_PRODUIT, @ID_PENNYLANE
END
ELSE
BEGIN

UPDATE PRODUITS
set
	MODIF_LE = GETDATE(),
	MODIF_PAR = 'PENNYLANE',
	PENNYLANE_ID = @ID_PENNYLANE
where NO_PRODUIT = @NO_PRODUIT

END

GO

