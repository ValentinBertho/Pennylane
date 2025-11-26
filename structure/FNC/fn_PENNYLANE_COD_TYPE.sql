CREATE FUNCTION dbo.fn_PENNYLANE_COD_TYPE()
RETURNS VARCHAR(50)
AS
BEGIN
    DECLARE @COD_TYPE VARCHAR(50)= null;

    -- Logique de sélection pour COD_TYPE
    -- SELECT @COD_TYPE = COD_TYPE
    -- FROM VotreTable4
    -- WHERE Condition = 'Condition pour COD_TYPE';

    RETURN @COD_TYPE;
END;
GO
