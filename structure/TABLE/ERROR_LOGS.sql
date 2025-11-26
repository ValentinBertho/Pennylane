-- Script de création de la table ERROR_LOGS
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[ERROR_LOGS]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[ERROR_LOGS](
        [LOG_ID] [int] IDENTITY(1,1) NOT NULL,
        [PROCEDURE_NAME] [varchar](255) NOT NULL,
        [ERROR_NUMBER] [int] NULL,
        [ERROR_SEVERITY] [int] NULL,
        [ERROR_STATE] [int] NULL,
        [ERROR_LINE] [int] NULL,
        [ERROR_MESSAGE] [nvarchar](max) NULL,
        [ERROR_DATETIME] [datetime] NOT NULL,
        [PARAMETERS] [nvarchar](max) NULL,
        [USERNAME] [varchar](255) NULL DEFAULT (SUSER_SNAME()),
        [HOSTNAME] [varchar](255) NULL DEFAULT (HOST_NAME()),
        [APPLICATION_NAME] [varchar](255) NULL DEFAULT (APP_NAME()),
        CONSTRAINT [PK_ERROR_LOGS] PRIMARY KEY CLUSTERED
        (
            [LOG_ID] ASC
        ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
    ) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

    PRINT 'Table ERROR_LOGS créée avec succès.'
END
ELSE
BEGIN
    PRINT 'La table ERROR_LOGS existe déjà.'
END
GO

-- Création d'un index sur la date pour améliorer les performances des requêtes
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_ERROR_LOGS_ERROR_DATETIME' AND object_id = OBJECT_ID('ERROR_LOGS'))
BEGIN
    CREATE NONCLUSTERED INDEX [IX_ERROR_LOGS_ERROR_DATETIME] ON [dbo].[ERROR_LOGS]
    (
        [ERROR_DATETIME] DESC
    ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]

    PRINT 'Index sur ERROR_DATETIME créé avec succès.'
END
GO

-- Création d'un index sur le nom de la procédure pour améliorer les recherches par procédure
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_ERROR_LOGS_PROCEDURE_NAME' AND object_id = OBJECT_ID('ERROR_LOGS'))
BEGIN
    CREATE NONCLUSTERED INDEX [IX_ERROR_LOGS_PROCEDURE_NAME] ON [dbo].[ERROR_LOGS]
    (
        [PROCEDURE_NAME] ASC
    ) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]

    PRINT 'Index sur PROCEDURE_NAME créé avec succès.'
END
GO

-- Ajout d'une procédure stockée pour faciliter l'insertion des erreurs
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[SP_INSERT_ERROR_LOG]') AND type in (N'P'))
    DROP PROCEDURE [dbo].[SP_INSERT_ERROR_LOG]
GO

CREATE PROCEDURE [dbo].[SP_INSERT_ERROR_LOG]
    @PROCEDURE_NAME VARCHAR(255),
    @PARAMETERS NVARCHAR(MAX) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO ERROR_LOGS (
        PROCEDURE_NAME,
        ERROR_NUMBER,
        ERROR_SEVERITY,
        ERROR_STATE,
        ERROR_LINE,
        ERROR_MESSAGE,
        ERROR_DATETIME,
        PARAMETERS
    )
    VALUES (
        @PROCEDURE_NAME,
        ERROR_NUMBER(),
        ERROR_SEVERITY(),
        ERROR_STATE(),
        ERROR_LINE(),
        ERROR_MESSAGE(),
        GETDATE(),
        @PARAMETERS
    );
END
GO

PRINT 'Procédure SP_INSERT_ERROR_LOG créée avec succès.'
GO