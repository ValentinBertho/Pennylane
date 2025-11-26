IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[SP_PENNYLANE_EXPORT_ECRITURES]') AND is_ms_shipped = 0 AND [type] IN ('P'))
DROP PROCEDURE [SP_PENNYLANE_EXPORT_ECRITURES]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO

/* ////////////////////////////////////////////////////////////////////////

Nom de la procedure stockee : SP_PENNYLANE_EXPORT_ECRITURES

No Version : 001

Description :

Procedure appelee par :
Interface PENNYLANE.

Historique des mises a jour :

> v001 - VABE - 14/06/2024 - Creation

//////////////////////////////////////////////////////////////////////// */

CREATE PROCEDURE [SP_PENNYLANE_EXPORT_ECRITURES]
    @NO_ECRITURE_LOT INT
AS
BEGIN
    IF EXISTS (SELECT * FROM sysobjects
               WHERE id = OBJECT_ID(N'[spe_SP_PENNYLANE_EXPORT_ECRITURES]')
                 AND OBJECTPROPERTY(id, N'IsProcedure') = 1)
    BEGIN
        EXEC spe_SP_PENNYLANE_EXPORT_ECRITURES @NO_ECRITURE_LOT
    END
    ELSE
    BEGIN
        -- Première requête
        SELECT
            ECRITURE_PIECE.NO_ECRITURE_PIECE AS noEcriturePiece,                  -- noEcriturePiece
            ECRITURE_LIGNE.NO_ECRITURE_LIGNE AS noEcritureLigne,                    -- noEcritureLigne
            DATE_ECRITURE AS dateEcriture,                                          -- dateEcriture
            CPTA_JOURNAL.CODE AS codeJournal,                                       -- codeJournal
            CPTA_CPTE.COMPTE AS compteGeneral,                                     -- compteGeneral
            RIGHT(REPLACE(ECRITURE_PIECE.NUMERO_DE_PIECE, '-', ''), 12) AS num,    -- num
            ECRITURE_PIECE.NUMERO_DE_PIECE AS numeroDePiece,                       -- numeroDePiece
            ECRITURE_PIECE.NUMERO_DE_PIECE AS reference,                           -- reference
            ECRITURE_LIGNE.COMPTE_TIERS AS compteTiers,                             -- compteTiers
            ECRITURE_LIGNE.LIBELLE AS libelle,                                      -- libelle
            ECRITURE_PIECE.DATE_ECHEANCE AS dateEcheance,                           -- dateEcheance
            ECRITURE_LIGNE.SENS AS sens,                                            -- sens
            ISNULL(ECRITURE_LIGNE.MONTANT, 0) AS montant,                           -- montant
            'G' AS type,                                                            -- type
            '' AS sectionAxe,                                                       -- sectionAxe
            '' AS section,                                                           -- section
            ECRITURE_PIECE.NO_V_FACTURE AS noVFacture ,                              -- no v facture
			ECRITURE_PIECE.NO_A_FACTURE AS noAFacture                               -- no v facture
        FROM ECRITURE_LOT
        INNER JOIN ECRITURE_PIECE ON ECRITURE_LOT.NO_ECRITURE_LOT = ECRITURE_PIECE.NO_ECRITURE_LOT
        INNER JOIN ECRITURE_LIGNE ON ECRITURE_PIECE.NO_ECRITURE_PIECE = ECRITURE_LIGNE.NO_ECRITURE_PIECE
        LEFT JOIN CPTA_JOURNAL ON CPTA_JOURNAL.NO_CPTA_JOURNAL = ECRITURE_PIECE.NO_CPTA_JOURNAL
        LEFT JOIN CPTA_CPTE ON CPTA_CPTE.NO_CPTA_CPTE = ECRITURE_LIGNE.NO_CPTA_CPTE
        WHERE ECRITURE_LOT.NO_ECRITURE_LOT = @NO_ECRITURE_LOT
          AND COALESCE(ECRITURE_PIECE.NO_A_REMISE, 0) = 0
          AND COALESCE(ECRITURE_PIECE.NO_DECAISSEMENT, 0) = 0
          AND COALESCE(ECRITURE_PIECE.NO_V_REMISE, 0) = 0
          AND COALESCE(ECRITURE_PIECE.NO_ENCAISSEMENT, 0) <> 0
		 AND EXISTS (
              SELECT 1
              FROM COURRIER
              WHERE NO_V_FACTURE = ECRITURE_PIECE.NO_V_FACTURE
          )

        UNION ALL

        -- Deuxième requête
        SELECT
            ECRITURE_PIECE.NO_ECRITURE_PIECE AS noEcriturePiece,                  -- noEcriturePiece
            ECRITURE_LIGNE.NO_ECRITURE_LIGNE AS noEcritureLigne,                    -- noEcritureLigne
            DATE_ECRITURE AS dateEcriture,                                          -- dateEcriture
            CPTA_JOURNAL.CODE AS codeJournal,                                       -- codeJournal
            CPTA_CPTE.COMPTE AS compteGeneral,                                     -- compteGeneral
            RIGHT(REPLACE(ECRITURE_PIECE.NUMERO_DE_PIECE, '-', ''), 12) AS num,    -- num
            ECRITURE_PIECE.NUMERO_DE_PIECE AS numeroDePiece,                       -- numeroDePiece
            ECRITURE_PIECE.NUMERO_DE_PIECE AS reference,                           -- reference
            ECRITURE_LIGNE.COMPTE_TIERS AS compteTiers,                             -- compteTiers
            ECRITURE_LIGNE.LIBELLE AS libelle,                                      -- libelle
            ECRITURE_PIECE.DATE_ECHEANCE AS dateEcheance,                           -- dateEcheance
            ECRITURE_LIGNE.SENS AS sens,                                            -- sens
            ISNULL(ECRITURE_LIGNE.MONTANT, 0) AS montant,                           -- montant
            'G' AS type,                                                            -- type
            '' AS sectionAxe,                                                      -- sectionAxe
            '' AS section,                                                           -- section
            ECRITURE_PIECE.NO_V_FACTURE AS noVFacture,                               -- no v facture
			ECRITURE_PIECE.NO_A_FACTURE AS noAFacture                               -- no a facture
        FROM ECRITURE_LOT
        INNER JOIN ECRITURE_PIECE ON ECRITURE_LOT.NO_ECRITURE_LOT = ECRITURE_PIECE.NO_ECRITURE_LOT
        INNER JOIN ECRITURE_LIGNE ON ECRITURE_PIECE.NO_ECRITURE_PIECE = ECRITURE_LIGNE.NO_ECRITURE_PIECE
        LEFT JOIN CPTA_JOURNAL ON CPTA_JOURNAL.NO_CPTA_JOURNAL = ECRITURE_PIECE.NO_CPTA_JOURNAL
        LEFT JOIN CPTA_CPTE ON CPTA_CPTE.NO_CPTA_CPTE = ECRITURE_LIGNE.NO_CPTA_CPTE
        WHERE ECRITURE_LOT.NO_ECRITURE_LOT = @NO_ECRITURE_LOT
          AND COALESCE(ECRITURE_PIECE.NO_A_REMISE, 0) = 0
          AND COALESCE(ECRITURE_PIECE.NO_DECAISSEMENT, 0) = 0
          AND COALESCE(ECRITURE_PIECE.NO_V_REMISE, 0) = 0
          AND COALESCE(ECRITURE_PIECE.NO_ENCAISSEMENT, 0) = 0
		  AND EXISTS (
              SELECT 1
              FROM COURRIER
              WHERE NO_V_FACTURE = ECRITURE_PIECE.NO_V_FACTURE
          );
    END
END
GO
