USE sss_db;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

-- =========================
-- 1) 用户表
-- =========================
DROP TABLE IF EXISTS user_info;
CREATE TABLE user_info (
                           user_id        BIGINT PRIMARY KEY,
                           username       VARCHAR(50) NOT NULL UNIQUE,
                           password_hash  VARCHAR(255) NOT NULL,
                           email          VARCHAR(120) NOT NULL UNIQUE,
                           role           VARCHAR(20) NOT NULL, -- USER / ADMIN

                           version        BIGINT NOT NULL DEFAULT 1,
                           updated_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                           deleted        TINYINT(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB;

-- =========================
-- 2) 商品类型
-- =========================
DROP TABLE IF EXISTS category_info;
CREATE TABLE category_info (
                               category_id   BIGINT PRIMARY KEY,
                               category_name VARCHAR(80) NOT NULL UNIQUE,
                               description   VARCHAR(255),

                               version       BIGINT NOT NULL DEFAULT 1,
                               updated_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                               deleted       TINYINT(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB;

-- =========================
-- 3) 供应商
-- =========================
DROP TABLE IF EXISTS supplier_info;
CREATE TABLE supplier_info (
                               supplier_id   BIGINT PRIMARY KEY,
                               supplier_name VARCHAR(120) NOT NULL UNIQUE,
                               contact_name  VARCHAR(80),
                               contact_phone VARCHAR(40),
                               address       VARCHAR(255),
                               email         VARCHAR(120),

                               version       BIGINT NOT NULL DEFAULT 1,
                               updated_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                               deleted       TINYINT(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB;

-- =========================
-- 4) 商品
-- =========================
DROP TABLE IF EXISTS product_info;
CREATE TABLE product_info (
                              product_id    BIGINT PRIMARY KEY,
                              product_name  VARCHAR(120) NOT NULL,
                              category_id   BIGINT NOT NULL,
                              supplier_id   BIGINT NOT NULL,
                              price         DECIMAL(10,2) NOT NULL,
                              stock         INT NOT NULL,
                              description   VARCHAR(255),
                              listed_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

                              version       BIGINT NOT NULL DEFAULT 1,
                              updated_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                              deleted       TINYINT(1) NOT NULL DEFAULT 0,

                              CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category_info(category_id),
                              CONSTRAINT fk_product_supplier FOREIGN KEY (supplier_id) REFERENCES supplier_info(supplier_id)
) ENGINE=InnoDB;

-- =========================
-- 5) 订单
-- =========================
DROP TABLE IF EXISTS order_info;
CREATE TABLE order_info (
                            order_id        BIGINT PRIMARY KEY,
                            user_id         BIGINT NOT NULL,
                            product_id      BIGINT NOT NULL,
                            quantity        INT NOT NULL,
                            order_status    VARCHAR(30) NOT NULL, -- CREATED/PAID/CANCELLED/SHIPPED...
                            ordered_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                            shipping_address VARCHAR(255) NOT NULL,

                            version         BIGINT NOT NULL DEFAULT 1,
                            updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                            deleted         TINYINT(1) NOT NULL DEFAULT 0,

                            CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES user_info(user_id),
                            CONSTRAINT fk_order_product FOREIGN KEY (product_id) REFERENCES product_info(product_id)
) ENGINE=InnoDB;

-- =========================
-- 同步支撑表：变更日志（触发器写入）
-- =========================
DROP TABLE IF EXISTS change_log;
CREATE TABLE change_log (
                            change_id    BIGINT PRIMARY KEY AUTO_INCREMENT,
                            db_code      VARCHAR(20) NOT NULL,   -- MYSQL / POSTGRES / SQLSERVER
                            table_name   VARCHAR(64) NOT NULL,
                            op_type      VARCHAR(10) NOT NULL,   -- INSERT / UPDATE / DELETE
                            pk_value     VARCHAR(64) NOT NULL,
                            row_version  BIGINT NOT NULL,
                            row_updated_at DATETIME(3) NOT NULL,
                            payload_json JSON NULL,              -- 可选：整行快照（方便排查/冲突展示）
                            created_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

                            INDEX idx_change_created_at(created_at),
                            INDEX idx_change_table_pk(table_name, pk_value),
                            INDEX idx_change_table_created(table_name, created_at)
) ENGINE=InnoDB;

-- =========================
-- 冲突表：记录冲突
-- =========================
DROP TABLE IF EXISTS conflict_record;
CREATE TABLE conflict_record (
                                 conflict_id     BIGINT PRIMARY KEY AUTO_INCREMENT,
                                 table_name      VARCHAR(64) NOT NULL,
                                 pk_value        VARCHAR(64) NOT NULL,

                                 source_db       VARCHAR(20) NOT NULL,    -- 冲突来源（检测到的那条变更来自哪个库）
                                 target_db       VARCHAR(20) NOT NULL,    -- 本次同步要写入的目标库

                                 source_version  BIGINT NOT NULL,
                                 target_version  BIGINT NOT NULL,
                                 source_updated_at DATETIME(3) NOT NULL,
                                 target_updated_at DATETIME(3) NOT NULL,

                                 source_payload_json JSON NOT NULL,
                                 target_payload_json JSON NOT NULL,

                                 status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',  -- OPEN/RESOLVED/IGNORED
                                 resolved_by     VARCHAR(50) NULL,
                                 resolved_at     DATETIME(3) NULL,
                                 resolution      VARCHAR(20) NULL, -- KEEP_SOURCE / KEEP_TARGET

                                 created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

                                 UNIQUE KEY uk_conflict_open(table_name, pk_value, status),
                                 INDEX idx_conflict_status_created(status, created_at)
) ENGINE=InnoDB;

-- =========================
-- 每日同步报表聚合表（后台写入）
-- =========================
DROP TABLE IF EXISTS sync_run_daily;
CREATE TABLE sync_run_daily (
                                stat_date    DATE PRIMARY KEY,
                                success_count INT NOT NULL DEFAULT 0,
                                conflict_count INT NOT NULL DEFAULT 0,
                                failure_count INT NOT NULL DEFAULT 0,
                                updated_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB;

-- =========================
-- 索引（访问特征：下单/按用户查订单/按品类统计/报表）
-- =========================
CREATE INDEX idx_order_user_time ON order_info(user_id, ordered_at);
CREATE INDEX idx_order_product_time ON order_info(product_id, ordered_at);
CREATE INDEX idx_product_category ON product_info(category_id);
CREATE INDEX idx_product_supplier ON product_info(supplier_id);

-- =========================
-- 视图：订单明细（前端查询可用）
-- =========================
DROP VIEW IF EXISTS v_order_detail;
CREATE VIEW v_order_detail AS
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
FROM order_info o
         JOIN user_info u ON u.user_id = o.user_id
         JOIN product_info p ON p.product_id = o.product_id
         JOIN category_info c ON c.category_id = p.category_id
         JOIN supplier_info s ON s.supplier_id = p.supplier_id
WHERE o.deleted=0 AND u.deleted=0 AND p.deleted=0 AND c.deleted=0 AND s.deleted=0;

SET FOREIGN_KEY_CHECKS=1;