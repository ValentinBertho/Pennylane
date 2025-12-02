if exists (select * from sys.objects where object_id = object_id(N'[SP_EXPORT_FACTURE]') and is_ms_shipped = 0 and [type] in ('P'))
drop procedure [SP_EXPORT_FACTURE]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procedure stockee : SP_EXPORT_FACTURE

No Version : 001

Description :

Procedure appelee par :
Interface SAGE.

Historique des mises a jour :

> v001 - VABE - 29/07/2025 - Creation

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_EXPORT_FACTURE]
AS
IF exists (select * from sysobjects
where id = object_id(N'[spe_SP_EXPORT_FACTURE]')
and OBJECTPROPERTY(id, N'IsProcedure') = 1)
BEGIN
	EXEC spe_SP_EXPORT_FACTURE
END
ELSE
BEGIN

	select
		NO_V_FACTURE
	from V_FACTURE
	where V_FACTURE.COD_ETAT = '2'


END

GO