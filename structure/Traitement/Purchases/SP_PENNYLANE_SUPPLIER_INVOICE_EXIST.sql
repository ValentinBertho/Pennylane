if exists (select * from sys.objects where object_id = object_id(N'[SP_PENNYLANE_SUPPLIER_INVOICE_EXIST]') and is_ms_shipped = 0 and [type] in ('P'))
drop procedure [SP_PENNYLANE_SUPPLIER_INVOICE_EXIST]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procedure stockee : SP_PENNYLANE_SUPPLIER_INVOICE_EXIST

No Version : 001

Description :

Procedure appelee par :
Interface PENNYLANE.

Historique des mises a jour :

> v001 - VABE - 22/11/2024 - Creation

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_PENNYLANE_SUPPLIER_INVOICE_EXIST]
    @INVOICE_ID varchar(MAX)
AS
IF exists (select * from sysobjects
where id = object_id(N'[spe_SP_PENNYLANE_SUPPLIER_INVOICE_EXIST]')
and OBJECTPROPERTY(id, N'IsProcedure') = 1)
BEGIN
	EXEC spe_SP_PENNYLANE_SUPPLIER_INVOICE_EXIST @INVOICE_ID
END
ELSE
BEGIN

SELECT * FROM A_FACTURE
where PENNYLANE_ID = @INVOICE_ID

END

GO

