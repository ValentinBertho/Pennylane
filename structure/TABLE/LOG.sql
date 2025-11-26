-- Création de la table LOG
CREATE TABLE [dbo].[LOG] (
    [ID] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY, -- Correspond à @Id et @GeneratedValue
    [DATE_LOG] DATETIME NOT NULL,                  -- Correspond à dateLog
    [NIVEAU] NVARCHAR(50) NULL,                   -- INFO, DEBUG, WARN, ERROR, FATAL
    [TRAITEMENT] NVARCHAR(100) NULL,
    [INITIATEUR] NVARCHAR(100) NULL,
    [ID_SESSION_SQL] SMALLINT NULL,
    [MESSAGE] NVARCHAR(MAX) NULL,                 -- Correspond à TEXT
    [CLASSE] NVARCHAR(200) NULL,
    [METHODE] NVARCHAR(100) NULL,
    [STACK_TRACE] NVARCHAR(MAX) NULL,            -- Correspond à TEXT
    [IP_SOURCE] NVARCHAR(45) NULL,
    [URL_APPELLEE] NVARCHAR(500) NULL,
    [METHODE_HTTP] NVARCHAR(10) NULL,
    [CODE_RETOUR_HTTP] INT NULL,
    [DUREE_MS] BIGINT NULL,
    [TRAME_REQUETE] NVARCHAR(MAX) NULL,          -- Correspond à TEXT
    [TRAME_REPONSE] NVARCHAR(MAX) NULL,          -- Correspond à TEXT
    [ENVIRONNEMENT] NVARCHAR(50) NULL,           -- DEV, INT, PROD
    [APPLICATION] NVARCHAR(100) NULL
);

-- Création des index
CREATE INDEX idx_date_log ON [dbo].[LOG]([DATE_LOG]);
CREATE INDEX idx_niveau ON [dbo].[LOG]([NIVEAU]);
CREATE INDEX idx_traitement ON [dbo].[LOG]([TRAITEMENT]);
