USE sss_db;
GO

-- 为了可重复执行：先删视图再删表
IF OBJECT_ID('dbo.v_order_detail', 'V') IS NOT NULL DROP VIEW dbo.v_order_detail;
GO

-- drop tables in FK-safe order
IF OBJECT_ID('dbo.order_info','U') IS NOT NULL DROP TABLE dbo.order_info;
IF OBJECT_ID('dbo.product_info','U') IS NOT NULL DROP TABLE dbo.product_info;
IF OBJECT_ID('dbo.supplier_info','U') IS NOT NULL DROP TABLE dbo.supplier_info;
IF OBJECT_ID('dbo.category_info','U') IS NOT NULL DROP TABLE dbo.category_info;
IF OBJECT_ID('dbo.user_info','U') IS NOT NULL DROP TABLE dbo.user_info;

IF OBJECT_ID('dbo.change_log','U') IS NOT NULL DROP TABLE dbo.change_log;
IF OBJECT_ID('dbo.conflict_record','U') IS NOT NULL DROP TABLE dbo.conflict_record;
IF OBJECT_ID('dbo.sync_run_daily','U') IS NOT NULL DROP TABLE dbo.sync_run_daily;
GO

CREATE TABLE dbo.user_info (
  user_id        BIGINT PRIMARY KEY,
  username       NVARCHAR(50) NOT NULL UNIQUE,
  password_hash  NVARCHAR(255) NOT NULL,
  email          NVARCHAR(120) NOT NULL UNIQUE,
  role           NVARCHAR(20) NOT NULL,

  version        BIGINT NOT NULL DEFAULT(1),
  updated_at     DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
  deleted        BIT NOT NULL DEFAULT(0)
);
GO

CREATE TABLE dbo.category_info (
  category_id    BIGINT PRIMARY KEY,
  category_name  NVARCHAR(80) NOT NULL UNIQUE,
  description    NVARCHAR(255) NULL,

  version        BIGINT NOT NULL DEFAULT(1),
  updated_at     DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
  deleted        BIT NOT NULL DEFAULT(0)
);
GO

CREATE TABLE dbo.supplier_info (
  supplier_id    BIGINT PRIMARY KEY,
  supplier_name  NVARCHAR(120) NOT NULL UNIQUE,
  contact_name   NVARCHAR(80) NULL,
  contact_phone  NVARCHAR(40) NULL,
  address        NVARCHAR(255) NULL,
  email          NVARCHAR(120) NULL,

  version        BIGINT NOT NULL DEFAULT(1),
  updated_at     DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
  deleted        BIT NOT NULL DEFAULT(0)
);
GO

CREATE TABLE dbo.product_info (
  product_id     BIGINT PRIMARY KEY,
  product_name   NVARCHAR(120) NOT NULL,
  category_id    BIGINT NOT NULL,
  supplier_id    BIGINT NOT NULL,
  price          DECIMAL(10,2) NOT NULL,
  stock          INT NOT NULL,
  description    NVARCHAR(255) NULL,
  listed_at      DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),

  version        BIGINT NOT NULL DEFAULT(1),
  updated_at     DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
  deleted        BIT NOT NULL DEFAULT(0),

  CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES dbo.category_info(category_id),
  CONSTRAINT fk_product_supplier FOREIGN KEY (supplier_id) REFERENCES dbo.supplier_info(supplier_id)
);
GO

CREATE TABLE dbo.order_info (
  order_id        BIGINT PRIMARY KEY,
  user_id         BIGINT NOT NULL,
  product_id      BIGINT NOT NULL,
  quantity        INT NOT NULL,
  order_status    NVARCHAR(30) NOT NULL,
  ordered_at      DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
  shipping_address NVARCHAR(255) NOT NULL,

  version         BIGINT NOT NULL DEFAULT(1),
  updated_at      DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
  deleted         BIT NOT NULL DEFAULT(0),

  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES dbo.user_info(user_id),
  CONSTRAINT fk_order_product FOREIGN KEY (product_id) REFERENCES dbo.product_info(product_id)
);
GO

-- 同步表
CREATE TABLE dbo.change_log (
  change_id      BIGINT IDENTITY(1,1) PRIMARY KEY,
  db_code        NVARCHAR(20) NOT NULL,
  table_name     NVARCHAR(64) NOT NULL,
  op_type        NVARCHAR(10) NOT NULL,
  pk_value       NVARCHAR(64) NOT NULL,
  row_version    BIGINT NOT NULL,
  row_updated_at DATETIME2(3) NOT NULL,
  payload_json   NVARCHAR(MAX) NULL,
  created_at     DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME()
);
GO
CREATE INDEX idx_change_created_at ON dbo.change_log(created_at);
CREATE INDEX idx_change_table_pk ON dbo.change_log(table_name, pk_value);
CREATE INDEX idx_change_table_created ON dbo.change_log(table_name, created_at);
GO

CREATE TABLE dbo.conflict_record (
  conflict_id      BIGINT IDENTITY(1,1) PRIMARY KEY,
  table_name       NVARCHAR(64) NOT NULL,
  pk_value         NVARCHAR(64) NOT NULL,

  source_db        NVARCHAR(20) NOT NULL,
  target_db        NVARCHAR(20) NOT NULL,

  source_version   BIGINT NOT NULL,
  target_version   BIGINT NOT NULL,
  source_updated_at DATETIME2(3) NOT NULL,
  target_updated_at DATETIME2(3) NOT NULL,

  source_payload_json NVARCHAR(MAX) NOT NULL,
  target_payload_json NVARCHAR(MAX) NOT NULL,

  status           NVARCHAR(20) NOT NULL DEFAULT('OPEN'),
  resolved_by      NVARCHAR(50) NULL,
  resolved_at      DATETIME2(3) NULL,
  resolution       NVARCHAR(20) NULL,

  created_at       DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME()
);
GO
CREATE UNIQUE INDEX uk_conflict_open ON dbo.conflict_record(table_name, pk_value, status);
CREATE INDEX idx_conflict_status_created ON dbo.conflict_record(status, created_at);
GO

CREATE TABLE dbo.sync_run_daily (
  stat_date      DATE PRIMARY KEY,
  success_count  INT NOT NULL DEFAULT(0),
  conflict_count INT NOT NULL DEFAULT(0),
  failure_count  INT NOT NULL DEFAULT(0),
  updated_at     DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME()
);
GO

-- 索引
CREATE INDEX idx_order_user_time ON dbo.order_info(user_id, ordered_at);
CREATE INDEX idx_order_product_time ON dbo.order_info(product_id, ordered_at);
CREATE INDEX idx_product_category ON dbo.product_info(category_id);
CREATE INDEX idx_product_supplier ON dbo.product_info(supplier_id);
GO

-- 视图
CREATE VIEW dbo.v_order_detail AS
SELECT
  o.order_id,
  o.ordered_at,
  o.order_status,
  o.quantity,
  o.shipping_address,
  u.user_id,
  u.username,
  u.email,
  p.product_id,
  p.product_name,
  p.price,
  c.category_id,
  c.category_name,
  s.supplier_id,
  s.supplier_name
FROM dbo.order_info o
JOIN dbo.user_info u ON u.user_id = o.user_id
JOIN dbo.product_info p ON p.product_id = o.product_id
JOIN dbo.category_info c ON c.category_id = p.category_id
JOIN dbo.supplier_info s ON s.supplier_id = p.supplier_id
WHERE o.deleted=0 AND u.deleted=0 AND p.deleted=0 AND c.deleted=0 AND s.deleted=0;
GO