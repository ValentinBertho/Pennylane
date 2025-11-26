/* ////////////////////////////////////////////////////////////////////////

Nom de la procedure stockee : SP_PENNYLANE_EXPORT_LOT

No Version : 001

Description :

Procedure appelee par :
Interface PENNYLANE.

Historique des mises a jour :

> v001 - VABE - 17/07/2024 - Creation

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [dbo].[SP_PENNYLANE_EXPORT_LOT]
    @NO_T_SITE INT
AS
IF exists (select * from sysobjects
where id = object_id(N'[spe_SP_PENNYLANE_EXPORT_LOT]')
and OBJECTPROPERTY(id, N'IsProcedure') = 1)
BEGIN
	EXEC spe_SP_PENNYLANE_EXPORT_LOT @NO_T_SITE
END
ELSE
BEGIN

DECLARE @MAX_RETRY INT,
        @RETRY_DELAY_MINUTES INT;

EXEC SP__ADMIN_LIRE_T_PARAMETRE @VALEUR = @MAX_RETRY OUTPUT, @VARIABLE='MAX_RETRY', @PARAGRAPHE='PROCESS_RETRY', @DEFAUT='5';
EXEC SP__ADMIN_LIRE_T_PARAMETRE @VALEUR = @RETRY_DELAY_MINUTES OUTPUT, @VARIABLE='RETRY_DELAY', @PARAGRAPHE='PROCESS_RETRY', @DEFAUT='60';

-- Ensuite, dans ta sélection de lots à retraiter :
SELECT NO_ECRITURE_LOT
FROM ECRITURE_LOT
	WHERE
	(
	   (EXPORTER = 1 AND EXPORTER_LE IS NULL)
	   OR (
		  EXPORTER = 0
		  AND PENNYLANE_STATUS = 'ERREUR'
		  AND RETRY_COUNT < @MAX_RETRY
		  AND DATEADD(MINUTE, @RETRY_DELAY_MINUTES, EXPORTER_LE) <= GETDATE()
	   )
	)
	AND NO_T_SITE = @NO_T_SITE

END

