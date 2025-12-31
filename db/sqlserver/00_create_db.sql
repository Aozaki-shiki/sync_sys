-- SQL Server 2019+
IF DB_ID('sss_db') IS NULL
BEGIN
  CREATE DATABASE sss_db;
END
GO

USE sss_db;
GO