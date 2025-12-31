USE sss_db;
GO

-- 创建登录与用户（演示）
IF NOT EXISTS (SELECT * FROM sys.server_principals WHERE name = 'sss_app')
BEGIN
  CREATE LOGIN sss_app WITH PASSWORD = 'AppPassWord++';
END
GO
IF NOT EXISTS (SELECT * FROM sys.server_principals WHERE name = 'sss_ro')
BEGIN
  CREATE LOGIN sss_ro WITH PASSWORD = 'ReadOnlyPassWord++';
END
GO

IF NOT EXISTS (SELECT * FROM sys.database_principals WHERE name = 'sss_app')
  CREATE USER sss_app FOR LOGIN sss_app;
IF NOT EXISTS (SELECT * FROM sys.database_principals WHERE name = 'sss_ro')
  CREATE USER sss_ro FOR LOGIN sss_ro;
GO

-- 授权
EXEC sp_addrolemember 'db_datareader', 'sss_ro';
EXEC sp_addrolemember 'db_datareader', 'sss_app';
EXEC sp_addrolemember 'db_datawriter', 'sss_app';
GO