if exists (select * from sys.objects where object_id = object_id(N'[SP_PENNYLANE_CUSTOMER_INVOICE_BAP]') and is_ms_shipped = 0 and [type] in ('P'))
drop procedure [SP_PENNYLANE_CUSTOMER_INVOICE_BAP]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procedure stockee : SP_PENNYLANE_CUSTOMER_INVOICE_BAP

No Version : 001

Description :

Procedure appelee par :
Interface PENNYLANE.

Historique des mises a jour :

> v001 - VABE - 22/11/2024 - Creation

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_PENNYLANE_CUSTOMER_INVOICE_BAP]
    @COD_SITE VARCHAR(50)

AS
IF exists (select * from sysobjects
where id = object_id(N'[spe_SP_PENNYLANE_CUSTOMER_INVOICE_BAP]')
and OBJECTPROPERTY(id, N'IsProcedure') = 1)
BEGIN
	EXEC spe_SP_PENNYLANE_CUSTOMER_INVOICE_BAP
END
ELSE
BEGIN

SELECT PENNYLANE_ID
FROM A_FACTURE
LEFT JOIN SYNCHRO_MARQUAGE SM
    ON SM.NOM_ENTITE = 'A_FACTURE'
    AND SM.NO_ENTITE = A_FACTURE.NO_A_FACTURE
    AND SM.DATE_SYNCHRO <= COALESCE(A_FACTURE.MODIF_LE, A_FACTURE.CREER_LE)
    AND SM.CREER_PAR = 'INTERFACE_PENNYLANE'
WHERE 1 = 1
    AND SM.NO_SYNCHRO_MARQUAGE IS NOT NULL
    AND (A_FACTURE.MODIF_LE IS NOT NULL OR A_FACTURE.CREER_LE IS NOT NULL)
	AND A_FACTURE.COD_ETAT = '2'
	AND A_FACTURE.COD_SITE = @COD_SITE

END

GO

