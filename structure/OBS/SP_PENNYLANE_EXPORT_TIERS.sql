if exists (select * from sys.objects where object_id = object_id(N'[SP_PENNYLANE_EXPORT_TIERS]') and is_ms_shipped = 0 and [type] in ('P'))
drop procedure [SP_PENNYLANE_EXPORT_TIERS]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procedure stockee : SP_PENNYLANE_EXPORT_TIERS

No Version : 001

Description :

Procedure appelee par :
Interface PENNYLANE.

Historique des mises a jour :

> v001 - VABE - 14/06/2024 - Creation
> v002 - VABE - 17/07/2024 - Passage par synchro_marquage

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_PENNYLANE_EXPORT_TIERS]
    @COD_SITE VARCHAR
AS
IF exists (select * from sysobjects
where id = object_id(N'[spe_SP_PENNYLANE_EXPORT_TIERS]')
and OBJECTPROPERTY(id, N'IsProcedure') = 1)
BEGIN
	EXEC spe_SP_PENNYLANE_EXPORT_TIERS
END
ELSE
BEGIN

-- Clients
SELECT SOCIETE.NO_SOCIETE AS idUnique,
              TRIM(SOCIETE.CODE_CPTA) AS compteComptable,
              SOCIETE.NOM AS raisonSociale,
              '0' AS typeTiers,
              isnull(SOCIETE.ADRESSE1, '') AS adresse1,
              isnull(SOCIETE.ADRESSE2, '') + isnull(' ' + SOCIETE.ADRESSE3, '') AS adresse2,
              isnull(SOCIETE.CP, '00000') AS cp,
              isnull(SOCIETE.VILLE, '') AS ville,
              T_REGION.LIBELLE AS pays,
              SOCIETE.TELEPHONE AS telephone,
              SOCIETE.FAX AS fax,
              SOCIETE.E_MAIL AS email,
              SOCIETE.CODEAPE AS codeApe,
              SOCIETE.SIRET AS siret,
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
	left join RIB_TIERS	on RIB_TIERS.NO_SOCIETE = SOCIETE.NO_SOCIETE AND RIB_TIERS.RIB_DEFAUT = 1
where (SYNCHRO_MARQUAGE.DATE_SYNCHRO < SOCIETE.MODIF_LE or SYNCHRO_MARQUAGE.DATE_SYNCHRO is null)
        and isnull(SOCIETE.CODE_CPTA,'') <> ''

UNION ALL

 -- Fournisseurs
SELECT SOCIETE.NO_SOCIETE AS idUnique,
              TRIM(SOCIETE.CODE_CPTA) AS compteComptable,
              SOCIETE.NOM AS raisonSociale,
              '1' AS typeTiers,
              isnull(SOCIETE.ADRESSE1, '') AS adresse1,
              isnull(SOCIETE.ADRESSE2, '') + isnull(' ' + SOCIETE.ADRESSE3, '') AS adresse2,
              isnull(SOCIETE.CP, '00000') AS cp,
              isnull(SOCIETE.VILLE, '') AS ville,
              T_REGION.LIBELLE AS pays,
              SOCIETE.TELEPHONE AS telephone,
              SOCIETE.FAX AS fax,
              SOCIETE.E_MAIL AS email,
              SOCIETE.CODEAPE AS codeApe,
              SOCIETE.SIRET AS siret,
              RIB_TIERS.DOM1 AS intituleBanque,
              '0' AS structureBanque,
              RIB_TIERS.AGENCE AS codeBanque,
              RIB_TIERS.GUICHET AS guichetBanque,
              RIB_TIERS.COMPTE AS compteBanque,
              RIB_TIERS.CLE AS cleBanque,
              RIB_TIERS.BIC AS bicBanque,
              'EUR' AS codeIso
FROM SOCIETE
	-- left join SOCIETE_EXPORT	on SOCIETE_EXPORT.NO_SOCIETE = SOCIETE.NO_SOCIETE and isnull(SOCIETE_EXPORT.FOURNISSEUR,0) = 1
	left join SYNCHRO_MARQUAGE	on SYNCHRO_MARQUAGE.NO_ENTITE = SOCIETE.NO_SOCIETE  and isnull(SYNCHRO_MARQUAGE.NOM_ENTITE,'')='SOCIETE_F' and SYNCHRO_MARQUAGE.INFO = 'S01'
	left join T_REGION		on T_REGION.CODE = SOCIETE.COD_REGION
	left join RIB_TIERS		on RIB_TIERS.NO_SOCIETE = SOCIETE.NO_SOCIETE AND RIB_TIERS.RIB_DEFAUT = 1
where (SYNCHRO_MARQUAGE.DATE_SYNCHRO < SOCIETE.MODIF_LE or SYNCHRO_MARQUAGE.DATE_SYNCHRO is null)
	and SOCIETE.FOURNISSEUR = 1
	and isnull(SOCIETE.CODE_CPTA_FOUR,'') <> ''


END

GO

