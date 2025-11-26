IF EXISTS (
    SELECT * FROM sys.objects
    WHERE object_id = OBJECT_ID(N'[SP_PENNYLANE_TRAITER_FACTURE]')
      AND is_ms_shipped = 0
      AND [type] = 'P'
)
DROP PROCEDURE [SP_PENNYLANE_TRAITER_FACTURE];
GO

SET QUOTED_IDENTIFIER ON;
GO
SET ANSI_NULLS ON;
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procédure stockée : SP_PENNYLANE_TRAITER_FACTURE

No Version : 002

Description :
Met à jour les informations PENNYLANE d’une facture. Si la procédure spécifique
`spe_SP_PENNYLANE_TRAITER_FACTURE` existe, elle est appelée. Sinon, mise à jour manuelle
avec enregistrement conditionnel de l'état si succès.

Historique des mises à jour :

> v001 - VABE - 05/03/2025 - Création
> v002 - VABE  - 16/05/2025 - MAJ COD_ETAT seulement si @SUCCES = 1

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_PENNYLANE_TRAITER_FACTURE]
    @NO_V_FACTURE INT,
    @ID_PENNYLANE VARCHAR(MAX),
    @ID_PENNYLANE_V2 VARCHAR(MAX),
    @SUCCES BIT
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT *
        FROM sysobjects
        WHERE id = OBJECT_ID(N'[spe_SP_PENNYLANE_TRAITER_FACTURE]')
          AND OBJECTPROPERTY(id, N'IsProcedure') = 1
    )
    BEGIN
        EXEC spe_SP_PENNYLANE_TRAITER_FACTURE
            @NO_V_FACTURE,
            @ID_PENNYLANE,
            @ID_PENNYLANE_V2,
            @SUCCES;
    END
    ELSE
    BEGIN
        UPDATE V_FACTURE
        SET
            MODIF_LE = GETDATE(),
            MODIF_PAR = 'PENNYLANE',
            PENNYLANE_ID = @ID_PENNYLANE,
            PENNYLANE_ID_V2 = @ID_PENNYLANE_V2,
            COD_ETAT = CASE
                           WHEN @SUCCES = 1 THEN
                               3
                           ELSE COD_ETAT
                       END
        WHERE NO_V_FACTURE = @NO_V_FACTURE;

        EXEC SP_PENNYLANE_SYNCHRO_MARQUAGE
            @NO_ENTITE = @NO_V_FACTURE,
            @ENTITE = 'V_FACTURE',
            @INFO = 'MAJ',
            @REF_EXT = @ID_PENNYLANE;
    END
END
GO
