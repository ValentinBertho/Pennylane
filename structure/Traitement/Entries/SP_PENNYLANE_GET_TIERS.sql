if exists (select * from sys.objects where object_id = object_id(N'[SP_PENNYLANE_GET_TIERS]') and is_ms_shipped = 0 and [type] in ('P'))
drop procedure [SP_PENNYLANE_GET_TIERS]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procedure stockee : SP_PENNYLANE_GET_TIERS

No Version : 001

Description :

Procedure appelee par :
Interface PENNYLANE.

Historique des mises a jour :

> v001 - VABE - 14/06/2024 - Creation

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_PENNYLANE_GET_TIERS]
    @NO_SOCIETE int,
    @COD_SITE varchar(50)
AS
IF exists (select * from sysobjects
where id = object_id(N'[spe_SP_PENNYLANE_GET_TIERS]')
and OBJECTPROPERTY(id, N'IsProcedure') = 1)
BEGIN
	EXEC spe_SP_PENNYLANE_GET_TIERS @NO_SOCIETE
END
ELSE
BEGIN

SELECT DISTINCT SOCIETE_EXPORT.PENNYLANE_ID AS idUnique,
       TRIM(SOCIETE.CODE_CPTA) AS compteComptable,
       SOCIETE.NOM AS raisonSociale,
       '0' AS typeTiers,
       isnull(SOCIETE.ADRESSE1, '') AS adresse1,
       isnull(SOCIETE.ADRESSE2, '') + isnull(' ' + SOCIETE.ADRESSE3, '') AS adresse2,
       isnull(SOCIETE.CP, '00000') AS cp,
       isnull(SOCIETE.VILLE, '') AS ville,
       isnull(T_REGION.LIBELLE, 'FR') AS pays,
       SOCIETE.TELEPHONE AS telephone,
       SOCIETE.FAX AS fax,
       SOCIETE.E_MAIL AS email,
       SOCIETE.CODEAPE AS codeApe,
       SOCIETE.SIRET AS siret,
       SOCIETE.C11 AS emailRelance,
       SOCIETE.TVA_INTRA AS tva,
       SOCIETE.COD_REGION AS codRegion,
       CASE
          WHEN SOCIETE.COD_RGLT LIKE '%60%' THEN '60_days'
          WHEN SOCIETE.COD_RGLT LIKE '%45%' THEN '45_days'
          WHEN SOCIETE.COD_RGLT LIKE '%30%' THEN '30_days'
          WHEN SOCIETE.COD_RGLT LIKE '%15%' THEN '15_days'
          ELSE 'custom'
       END AS codRglt,
       RIB_TIERS.DOM1 AS intituleBanque,
       '0' AS structureBanque,
       RIB_TIERS.AGENCE AS codeBanque,
       RIB_TIERS.GUICHET AS guichetBanque,
       RIB_TIERS.COMPTE AS compteBanque,
       RIB_TIERS.CLE AS cleBanque,
       RIB_TIERS.BIC AS bicBanque,
       'EUR' AS codeIso
FROM SOCIETE
	-- left join SOCIETE_EXPORT	on SOCIETE_EXPORT.NO_SOCIETE = SOCIETE.NO_SOCIETE  and isnull(SOCIETE_EXPORT.FOURNISSEUR,0)=0
    left join SYNCHRO_MARQUAGE	on SYNCHRO_MARQUAGE.NO_ENTITE = SOCIETE.NO_SOCIETE  and isnull(SYNCHRO_MARQUAGE.NOM_ENTITE,'')='SOCIETE_C' and SYNCHRO_MARQUAGE.INFO = 'S01'
	left join T_REGION	on T_REGION.CODE = SOCIETE.COD_REGION
    left join SOCIETE_EXPORT ON SOCIETE_EXPORT.NO_SOCIETE = SOCIETE.NO_SOCIETE AND SOCIETE_EXPORT.COD_SITE = @COD_SITE
	left join RIB_TIERS	on RIB_TIERS.NO_SOCIETE = SOCIETE.NO_SOCIETE AND RIB_TIERS.RIB_DEFAUT = 1
where
	SOCIETE.NO_SOCIETE = @NO_SOCIETE

END

GO

