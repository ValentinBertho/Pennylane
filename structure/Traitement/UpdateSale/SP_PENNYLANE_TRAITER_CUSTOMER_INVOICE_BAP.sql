if exists (select * from sys.objects where object_id = object_id(N'[SP_PENNYLANE_TRAITER_CUSTOMER_INVOICE_BAP]') and is_ms_shipped = 0 and [type] in ('P'))
drop procedure [SP_PENNYLANE_TRAITER_CUSTOMER_INVOICE_BAP]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procedure stockee : SP_PENNYLANE_TRAITER_CUSTOMER_INVOICE_BAP

No Version : 001

Description :

Procedure appelee par :
Interface PENNYLANE.

Historique des mises a jour :

> v001 - VABE - 22/11/2024 - Creation

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_PENNYLANE_TRAITER_CUSTOMER_INVOICE_BAP]
    @ID_A_FACTURE varchar(MAX),
    @MESSAGE varchar(MAX),
    @SUCCES bit

AS
IF exists (select * from sysobjects
where id = object_id(N'[spe_SP_PENNYLANE_TRAITER_CUSTOMER_INVOICE_BAP]')
and OBJECTPROPERTY(id, N'IsProcedure') = 1)
BEGIN
	EXEC spe_SP_PENNYLANE_TRAITER_CUSTOMER_INVOICE_BAP @ID_A_FACTURE, @MESSAGE, @SUCCES
END
ELSE
BEGIN
    DECLARE @NO_A_FACTURE INT;
    SET @NO_A_FACTURE = (SELECT NO_A_FACTURE FROM A_FACTURE WHERE PENNYLANE_ID = @ID_A_FACTURE);



                EXEC SP_PENNYLANE_SYNCHRO_MARQUAGE
                    @NO_ENTITE = @NO_A_FACTURE,
                    @ENTITE = 'A_FACTURE',
                    @INFO = 'MAJ',
                    @REF_EXT = @ID_A_FACTURE;


END

GO

