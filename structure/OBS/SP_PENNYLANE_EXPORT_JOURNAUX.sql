if exists (select * from sys.objects where object_id = object_id(N'[SP_PENNYLANE_EXPORT_JOURNAUX]') and is_ms_shipped = 0 and [type] in ('P'))
drop procedure [SP_PENNYLANE_EXPORT_JOURNAUX]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procédure stockée : SP_PENNYLANE_EXPORT_JOURNAUX

Description :

Procédure appelée par :
Interface PENNYLANE.

Historique des mises à jour :

> v001 - VABE - 17/07/2024

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_PENNYLANE_EXPORT_JOURNAUX]
    @COD_SITE VARCHAR
AS
IF exists (select * from sysobjects 
where id = object_id(N'[spe_SP_PENNYLANE_EXPORT_JOURNAUX]')
and OBJECTPROPERTY(id, N'IsProcedure') = 1)
BEGIN
	EXEC spe_SP_PENNYLANE_EXPORT_JOURNAUX
END
ELSE
BEGIN

SELECT C.NO_CPTA_JOURNAL
FROM CPTA_JOURNAL C
LEFT JOIN SYNCHRO_MARQUAGE S ON S.NOM_ENTITE = 'CPTA_JOURNAL' AND S.NO_ENTITE = C.NO_CPTA_JOURNAL AND S.DATE_SYNCHRO >= COALESCE(C.MODIF_LE, C.CREER_LE) AND S.INFO = @COD_SITE
WHERE
    S.NO_SYNCHRO_MARQUAGE IS NULL
    AND (C.MODIF_LE IS NOT NULL OR C.CREER_LE IS NOT NULL)
    -- REMOVE ENSUITE AND COD_SITE = @COD_SITE
END

GO

