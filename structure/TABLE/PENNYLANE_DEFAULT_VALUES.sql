-- Supprime la table si elle existe déjà
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'PENNYLANE_DEFAULT_VALUES')
    DROP TABLE PENNYLANE_DEFAULT_VALUES;

-- Création de la table des valeurs par défaut
CREATE TABLE PENNYLANE_DEFAULT_VALUES
(
    CODE VARCHAR(100) PRIMARY KEY,       -- Identifiant pour chaque champ
    DEFAULT_VALUE VARCHAR(200) NOT NULL  -- Valeur par défaut associée
);

-- Insertion des valeurs par défaut
INSERT INTO PENNYLANE_DEFAULT_VALUES VALUES
('COD_COM', 'SIE_Cpta'),            -- Valeur par défaut pour COD_COM
('COD_SERVICE', 'ADM'),             -- Valeur par défaut pour COD_SERVICE
('COD_DIRECTION', 'ADM'),           -- Valeur par défaut pour COD_DIRECTION
('COD_AGENCE', 'DG'),               -- Valeur par défaut pour COD_AGENCE
('COD_TYPE', 'FAC'),                -- Valeur par défaut pour COD_TYPE
('COD_ETAT', '3'),                  -- Valeur par défaut pour COD_ETAT
('COD_STATUT', '1'),                -- Valeur par défaut pour COD_STATUT
('NO_INTERLO', '-1'),               -- Valeur par défaut pour NO_INTERLO
