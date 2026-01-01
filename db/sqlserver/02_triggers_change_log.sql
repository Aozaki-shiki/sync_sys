USE sss_db;
GO

-- SQL Server：用 INSERTED/DELETED 表写 change_log
-- payload_json 用 FOR JSON PATH

-- user_info
IF OBJECT_ID('dbo.trg_user_info_log','TR') IS NOT NULL DROP TRIGGER dbo.trg_user_info_log;
GO
CREATE TRIGGER dbo.trg_user_info_log
    ON dbo.user_info
    AFTER INSERT, UPDATE, DELETE
    AS
BEGIN
  SET NOCOUNT ON;

  -- INSERT/UPDATE：inserted 有值
INSERT INTO dbo.change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
SELECT
    'SQLSERVER',
    'user_info',
    CASE WHEN d.user_id IS NULL THEN 'INSERT' ELSE 'UPDATE' END,
    CAST(i.user_id AS NVARCHAR(64)),
    i.version,
    i.updated_at,
    (SELECT i.* FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)
FROM inserted i
    LEFT JOIN deleted d ON d.user_id = i.user_id;

-- DELETE：deleted 有值，inserted 无值
INSERT INTO dbo.change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
SELECT
    'SQLSERVER',
    'user_info',
    'DELETE',
    CAST(d.user_id AS NVARCHAR(64)),
    d.version,
    d.updated_at,
    (SELECT d.* FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)
FROM deleted d
    LEFT JOIN inserted i ON i.user_id = d.user_id
WHERE i.user_id IS NULL;
END
GO

-- 其余表触发器（category/supplier/product/order）同理：为保证“完整”，我也给出
-- category_info
IF OBJECT_ID('dbo.trg_category_info_log','TR') IS NOT NULL DROP TRIGGER dbo.trg_category_info_log;
GO
CREATE TRIGGER dbo.trg_category_info_log ON dbo.category_info
    AFTER INSERT, UPDATE, DELETE
    AS
BEGIN
  SET NOCOUNT ON;

INSERT INTO dbo.change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
SELECT 'SQLSERVER','category_info',
       CASE WHEN d.category_id IS NULL THEN 'INSERT' ELSE 'UPDATE' END,
       CAST(i.category_id AS NVARCHAR(64)), i.version, i.updated_at,
       (SELECT i.* FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)
FROM inserted i LEFT JOIN deleted d ON d.category_id=i.category_id;

INSERT INTO dbo.change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
SELECT 'SQLSERVER','category_info','DELETE',
       CAST(d.category_id AS NVARCHAR(64)), d.version, d.updated_at,
       (SELECT d.* FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)
FROM deleted d LEFT JOIN inserted i ON i.category_id=d.category_id
WHERE i.category_id IS NULL;
END
GO

-- supplier_info
IF OBJECT_ID('dbo.trg_supplier_info_log','TR') IS NOT NULL DROP TRIGGER dbo.trg_supplier_info_log;
GO
CREATE TRIGGER dbo.trg_supplier_info_log ON dbo.supplier_info
    AFTER INSERT, UPDATE, DELETE
    AS
BEGIN
  SET NOCOUNT ON;

INSERT INTO dbo.change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
SELECT 'SQLSERVER','supplier_info',
       CASE WHEN d.supplier_id IS NULL THEN 'INSERT' ELSE 'UPDATE' END,
       CAST(i.supplier_id AS NVARCHAR(64)), i.version, i.updated_at,
       (SELECT i.* FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)
FROM inserted i LEFT JOIN deleted d ON d.supplier_id=i.supplier_id;

INSERT INTO dbo.change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
SELECT 'SQLSERVER','supplier_info','DELETE',
       CAST(d.supplier_id AS NVARCHAR(64)), d.version, d.updated_at,
       (SELECT d.* FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)
FROM deleted d LEFT JOIN inserted i ON i.supplier_id=d.supplier_id
WHERE i.supplier_id IS NULL;
END
GO

-- product_info
IF OBJECT_ID('dbo.trg_product_info_log','TR') IS NOT NULL DROP TRIGGER dbo.trg_product_info_log;
GO
CREATE TRIGGER dbo.trg_product_info_log ON dbo.product_info
    AFTER INSERT, UPDATE, DELETE
    AS
BEGIN
  SET NOCOUNT ON;

INSERT INTO dbo.change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
SELECT 'SQLSERVER','product_info',
       CASE WHEN d.product_id IS NULL THEN 'INSERT' ELSE 'UPDATE' END,
       CAST(i.product_id AS NVARCHAR(64)), i.version, i.updated_at,
       (SELECT i.* FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)
FROM inserted i LEFT JOIN deleted d ON d.product_id=i.product_id;

INSERT INTO dbo.change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
SELECT 'SQLSERVER','product_info','DELETE',
       CAST(d.product_id AS NVARCHAR(64)), d.version, d.updated_at,
       (SELECT d.* FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)
FROM deleted d LEFT JOIN inserted i ON i.product_id=d.product_id
WHERE i.product_id IS NULL;
END
GO

-- order_info
IF OBJECT_ID('dbo.trg_order_info_log','TR') IS NOT NULL DROP TRIGGER dbo.trg_order_info_log;
GO
CREATE TRIGGER dbo.trg_order_info_log ON dbo.order_info
    AFTER INSERT, UPDATE, DELETE
    AS
BEGIN
  SET NOCOUNT ON;

INSERT INTO dbo.change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
SELECT 'SQLSERVER','order_info',
       CASE WHEN d.order_id IS NULL THEN 'INSERT' ELSE 'UPDATE' END,
       CAST(i.order_id AS NVARCHAR(64)), i.version, i.updated_at,
       (SELECT i.* FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)
FROM inserted i LEFT JOIN deleted d ON d.order_id=i.order_id;

INSERT INTO dbo.change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
SELECT 'SQLSERVER','order_info','DELETE',
       CAST(d.order_id AS NVARCHAR(64)), d.version, d.updated_at,
       (SELECT d.* FOR JSON PATH, WITHOUT_ARRAY_WRAPPER)
FROM deleted d LEFT JOIN inserted i ON i.order_id=d.order_id
WHERE i.order_id IS NULL;
END
GO