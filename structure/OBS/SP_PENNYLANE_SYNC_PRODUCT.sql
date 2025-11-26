if exists (select * from sys.objects where object_id = object_id(N'[SP_PENNYLANE_SYNC_PRODUCT]') and is_ms_shipped = 0 and [type] in ('P'))
drop procedure [SP_PENNYLANE_SYNC_PRODUCT]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procedure stockee : SP_PENNYLANE_SYNC_PRODUCT

No Version : 001

Description :

Procedure appelee par :
Interface PENNYLANE.

Historique des mises a jour :

> v001 - VABE - 14/06/2024 - Creation

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_PENNYLANE_SYNC_PRODUCT]
AS
IF exists (select * from sysobjects
where id = object_id(N'[spe_SP_PENNYLANE_SYNC_PRODUCT]')
and OBJECTPROPERTY(id, N'IsProcedure') = 1)
BEGIN
	EXEC spe_SP_PENNYLANE_SYNC_PRODUCT
END
ELSE
BEGIN

select DISTINCT(NO_PRODUIT) 	FROM PRODUITS P
                            	LEFT JOIN SYNCHRO_MARQUAGE SM
                            		ON SM.NOM_ENTITE = 'PRODUITS'
                            		AND SM.NO_ENTITE = P.NO_PRODUIT
                            		AND SM.DATE_SYNCHRO >= COALESCE(P.MODIF_LE, P.CREER_LE)
                            		AND SM.CREER_PAR = 'INTERFACE-PENNYLANE'
                            	WHERE
                            		SM.NO_SYNCHRO_MARQUAGE IS NULL
                            		AND L5 = 1;
END

GO

