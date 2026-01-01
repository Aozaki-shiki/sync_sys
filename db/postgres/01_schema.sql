-- 在 sss_db 库中执行

CREATE SCHEMA IF NOT EXISTS public;

-- 统一：用 BIGINT 作为主键，应用层生成 Snowflake ID
DROP TABLE IF EXISTS user_info CASCADE;
CREATE TABLE user_info (
                           user_id        BIGINT PRIMARY KEY,
                           username       VARCHAR(50) NOT NULL UNIQUE,
                           password_hash  VARCHAR(255) NOT NULL,
                           email          VARCHAR(120) NOT NULL UNIQUE,
                           role           VARCHAR(20) NOT NULL,

                           version        BIGINT NOT NULL DEFAULT 1,
                           updated_at     TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           deleted        BOOLEAN NOT NULL DEFAULT FALSE
);

DROP TABLE IF EXISTS category_info CASCADE;
CREATE TABLE category_info (
                               category_id    BIGINT PRIMARY KEY,
                               category_name  VARCHAR(80) NOT NULL UNIQUE,
                               description    VARCHAR(255),

                               version        BIGINT NOT NULL DEFAULT 1,
                               updated_at     TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               deleted        BOOLEAN NOT NULL DEFAULT FALSE
);

DROP TABLE IF EXISTS supplier_info CASCADE;
CREATE TABLE supplier_info (
                               supplier_id    BIGINT PRIMARY KEY,
                               supplier_name  VARCHAR(120) NOT NULL UNIQUE,
                               contact_name   VARCHAR(80),
                               contact_phone  VARCHAR(40),
                               address        VARCHAR(255),
                               email          VARCHAR(120),

                               version        BIGINT NOT NULL DEFAULT 1,
                               updated_at     TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               deleted        BOOLEAN NOT NULL DEFAULT FALSE
);

DROP TABLE IF EXISTS product_info CASCADE;
CREATE TABLE product_info (
                              product_id     BIGINT PRIMARY KEY,
                              product_name   VARCHAR(120) NOT NULL,
                              category_id    BIGINT NOT NULL REFERENCES category_info(category_id),
                              supplier_id    BIGINT NOT NULL REFERENCES supplier_info(supplier_id),
                              price          NUMERIC(10,2) NOT NULL,
                              stock          INT NOT NULL,
                              description    VARCHAR(255),
                              listed_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              version        BIGINT NOT NULL DEFAULT 1,
                              updated_at     TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              deleted        BOOLEAN NOT NULL DEFAULT FALSE
);

DROP TABLE IF EXISTS order_info CASCADE;
CREATE TABLE order_info (
                            order_id        BIGINT PRIMARY KEY,
                            user_id         BIGINT NOT NULL REFERENCES user_info(user_id),
                            product_id      BIGINT NOT NULL REFERENCES product_info(product_id),
                            quantity        INT NOT NULL,
                            order_status    VARCHAR(30) NOT NULL,
                            ordered_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            shipping_address VARCHAR(255) NOT NULL,

                            version         BIGINT NOT NULL DEFAULT 1,
                            updated_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            deleted         BOOLEAN NOT NULL DEFAULT FALSE
);

-- 同步表
DROP TABLE IF EXISTS change_log;
CREATE TABLE change_log (
                            change_id      BIGSERIAL PRIMARY KEY,
                            db_code        VARCHAR(20) NOT NULL,
                            table_name     VARCHAR(64) NOT NULL,
                            op_type        VARCHAR(10) NOT NULL,
                            pk_value       VARCHAR(64) NOT NULL,
                            row_version    BIGINT NOT NULL,
                            row_updated_at TIMESTAMP(3) NOT NULL,
                            payload_json   JSONB NULL,
                            created_at     TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_change_created_at ON change_log(created_at);
CREATE INDEX idx_change_table_pk ON change_log(table_name, pk_value);
CREATE INDEX idx_change_table_created ON change_log(table_name, created_at);

DROP TABLE IF EXISTS conflict_record;
CREATE TABLE conflict_record (
                                 conflict_id      BIGSERIAL PRIMARY KEY,
                                 table_name       VARCHAR(64) NOT NULL,
                                 pk_value         VARCHAR(64) NOT NULL,

                                 source_db        VARCHAR(20) NOT NULL,
                                 target_db        VARCHAR(20) NOT NULL,

                                 source_version   BIGINT NOT NULL,
                                 target_version   BIGINT NOT NULL,
                                 source_updated_at TIMESTAMP(3) NOT NULL,
                                 target_updated_at TIMESTAMP(3) NOT NULL,

                                 source_payload_json JSONB NOT NULL,
                                 target_payload_json JSONB NOT NULL,

                                 status           VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                                 resolved_by      VARCHAR(50),
                                 resolved_at      TIMESTAMP(3),
                                 resolution       VARCHAR(20),

                                 created_at       TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_conflict_open ON conflict_record(table_name, pk_value, status);
CREATE INDEX idx_conflict_status_created ON conflict_record(status, created_at);

DROP TABLE IF EXISTS sync_run_daily;
CREATE TABLE sync_run_daily (
                                stat_date      DATE PRIMARY KEY,
                                success_count  INT NOT NULL DEFAULT 0,
                                conflict_count INT NOT NULL DEFAULT 0,
                                failure_count  INT NOT NULL DEFAULT 0,
                                updated_at     TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_order_user_time ON order_info(user_id, ordered_at);
CREATE INDEX idx_order_product_time ON order_info(product_id, ordered_at);
CREATE INDEX idx_product_category ON product_info(category_id);
CREATE INDEX idx_product_supplier ON product_info(supplier_id);

-- 视图
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
WHERE o.deleted=false AND u.deleted=false AND p.deleted=false AND c.deleted=false AND s.deleted=false;