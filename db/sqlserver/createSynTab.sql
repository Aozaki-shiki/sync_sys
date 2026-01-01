USE sss_db;
GO

IF OBJECT_ID('dbo.sync_checkpoint','U') IS NOT NULL DROP TABLE dbo.sync_checkpoint;
GO

CREATE TABLE dbo.sync_checkpoint (
    checkpoint_id BIGINT  PRIMARY KEY,
    source_db NVARCHAR(20) NOT NULL UNIQUE,
    last_change_id BIGINT NOT NULL DEFAULT(0),
    updated_at DATETIME2(3) NOT NULL DEFAULT SYSDATETIME()
);
GO

MERGE dbo.sync_checkpoint AS t
USING (
    SELECT 'MYSQL' AS source_db, CAST(0 AS BIGINT) AS last_change_id
    UNION ALL
    SELECT 'POSTGRES' AS source_db, CAST(0 AS BIGINT) AS last_change_id -- 修正列别名
) AS s
ON t.source_db = s.source_db
WHEN NOT MATCHED THEN
    INSERT (source_db, last_change_id) VALUES(s.source_db, s.last_change_id)
WHEN MATCHED THEN
    UPDATE SET last_change_id = s.last_change_id;
GO