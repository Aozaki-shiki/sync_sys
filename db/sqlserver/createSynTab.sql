USE sss_db;
GO

IF OBJECT_ID('dbo.sync_checkpoint','U') IS NOT NULL DROP TABLE dbo.sync_checkpoint;
GO

CREATE TABLE dbo.sync_checkpoint (
                                     checkpoint_id  BIGINT IDENTITY(1,1) PRIMARY KEY,
                                     source_db      NVARCHAR(20) NOT NULL UNIQUE, -- MYSQL / POSTGRES
                                     last_change_id BIGINT NOT NULL DEFAULT(0),
                                     updated_at     DATETIME2(3) NOT NULL DEFAULT SYSDATETIME()
);
GO

-- Initialize required rows (avoid MERGE to prevent parser/version issues)
IF NOT EXISTS (SELECT 1 FROM dbo.sync_checkpoint WHERE source_db = N'MYSQL')
  INSERT INTO dbo.sync_checkpoint(source_db, last_change_id) VALUES (N'MYSQL', 0);

IF NOT EXISTS (SELECT 1 FROM dbo.sync_checkpoint WHERE source_db = N'POSTGRES')
  INSERT INTO dbo.sync_checkpoint(source_db, last_change_id) VALUES (N'POSTGRES', 0);
GO
GO