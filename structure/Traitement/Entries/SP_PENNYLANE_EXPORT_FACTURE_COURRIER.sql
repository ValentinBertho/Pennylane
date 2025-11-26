/****** Object:  StoredProcedure [dbo].[SP_PENNYLANE_EXPORT_FACTURE_COURRIER]    Script Date: 31/07/2024 17:17:56 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procédure stockée : [SP_PENNYLANE_EXPORT_FACTURE_COURRIER]

Description :

Procédure appelée par :
Interface PENNYLANE.

Historique des mises à jour :

> v001 - VABE - 17/07/2024

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [dbo].[SP_PENNYLANE_EXPORT_FACTURE_COURRIER]
    @NO_V_FACTURE INT
AS
IF exists (select * from sysobjects
where id = object_id(N'[spe_SP_PENNYLANE_EXPORT_FACTURE_COURRIER]')
and OBJECTPROPERTY(id, N'IsProcedure') = 1)
BEGIN
	EXEC spe_SP_PENNYLANE_EXPORT_FACTURE_COURRIER @NO_V_FACTURE
END
ELSE
    BEGIN
        SELECT TOP 1 *
        FROM COURRIER C
        WHERE C.NO_V_FACTURE = @NO_V_FACTURE
END

