if exists (select * from sys.objects where object_id = object_id(N'[SP_PENNYLANE_SUPPLIER_INVOICE_REGLEMENT]') and is_ms_shipped = 0 and [type] in ('P'))
drop procedure [SP_PENNYLANE_SUPPLIER_INVOICE_REGLEMENT]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procedure stockee : SP_PENNYLANE_SUPPLIER_INVOICE_REGLEMENT

No Version : 001

Description :

Procedure appelee par :
Interface PENNYLANE.

Historique des mises a jour :

> v001 - VABE - 22/11/2024 - Creation

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_PENNYLANE_SUPPLIER_INVOICE_REGLEMENT]
    @COD_SITE VARCHAR(50)
AS
IF exists (select * from sysobjects
where id = object_id(N'[spe_SP_PENNYLANE_SUPPLIER_INVOICE_REGLEMENT]')
and OBJECTPROPERTY(id, N'IsProcedure') = 1)
BEGIN
	EXEC spe_SP_PENNYLANE_SUPPLIER_INVOICE_REGLEMENT
END
ELSE
BEGIN

SELECT PENNYLANE_ID
FROM V_FACTURE
WHERE 1 = 1
    AND V_FACTURE.COD_STATUT != 3
	AND PENNYLANE_ID is not null
	AND COD_SITE = @COD_SITE
END

GO

