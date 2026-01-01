USE sss_db;

-- MySQL：用触发器写 change_log
-- 说明：payload_json 用 JSON_OBJECT 生成，便于冲突邮件/页面展示

DROP TRIGGER IF EXISTS trg_user_info_ai;
DELIMITER $$
CREATE TRIGGER trg_user_info_ai AFTER INSERT ON user_info
    FOR EACH ROW
BEGIN
    INSERT INTO change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
    VALUES ('MYSQL','user_info','INSERT', NEW.user_id, NEW.version, NEW.updated_at,
            JSON_OBJECT('user_id', NEW.user_id,'username',NEW.username,'email',NEW.email,'role',NEW.role,'version',NEW.version,'updated_at',NEW.updated_at,'deleted',NEW.deleted)
           );
    END$$
    DELIMITER ;

    DROP TRIGGER IF EXISTS trg_user_info_au;
    DELIMITER $$
    CREATE TRIGGER trg_user_info_au AFTER UPDATE ON user_info
        FOR EACH ROW
    BEGIN
        INSERT INTO change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
        VALUES ('MYSQL','user_info','UPDATE', NEW.user_id, NEW.version, NEW.updated_at,
                JSON_OBJECT('user_id', NEW.user_id,'username',NEW.username,'email',NEW.email,'role',NEW.role,'version',NEW.version,'updated_at',NEW.updated_at,'deleted',NEW.deleted)
               );
        END$$
        DELIMITER ;

        DROP TRIGGER IF EXISTS trg_user_info_ad;
        DELIMITER $$
        CREATE TRIGGER trg_user_info_ad AFTER DELETE ON user_info
            FOR EACH ROW
        BEGIN
            INSERT INTO change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
            VALUES ('MYSQL','user_info','DELETE', OLD.user_id, OLD.version, OLD.updated_at,
                    JSON_OBJECT('user_id', OLD.user_id,'username',OLD.username,'email',OLD.email,'role',OLD.role,'version',OLD.version,'updated_at',OLD.updated_at,'deleted',OLD.deleted)
                   );
            END$$
            DELIMITER ;

-- 为了篇幅可控：下面 4 张表用相同模式（你要“完整代码”，所以我照样给全）
-- category_info
            DROP TRIGGER IF EXISTS trg_category_info_ai;
            DELIMITER $$
            CREATE TRIGGER trg_category_info_ai AFTER INSERT ON category_info
                FOR EACH ROW
            BEGIN
                INSERT INTO change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
                VALUES ('MYSQL','category_info','INSERT', NEW.category_id, NEW.version, NEW.updated_at,
                        JSON_OBJECT('category_id', NEW.category_id,'category_name',NEW.category_name,'description',NEW.description,'version',NEW.version,'updated_at',NEW.updated_at,'deleted',NEW.deleted)
                       );
                END$$
                DELIMITER ;

                DROP TRIGGER IF EXISTS trg_category_info_au;
                DELIMITER $$
                CREATE TRIGGER trg_category_info_au AFTER UPDATE ON category_info
                    FOR EACH ROW
                BEGIN
                    INSERT INTO change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
                    VALUES ('MYSQL','category_info','UPDATE', NEW.category_id, NEW.version, NEW.updated_at,
                            JSON_OBJECT('category_id', NEW.category_id,'category_name',NEW.category_name,'description',NEW.description,'version',NEW.version,'updated_at',NEW.updated_at,'deleted',NEW.deleted)
                           );
                    END$$
                    DELIMITER ;

                    DROP TRIGGER IF EXISTS trg_category_info_ad;
                    DELIMITER $$
                    CREATE TRIGGER trg_category_info_ad AFTER DELETE ON category_info
                        FOR EACH ROW
                    BEGIN
                        INSERT INTO change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
                        VALUES ('MYSQL','category_info','DELETE', OLD.category_id, OLD.version, OLD.updated_at,
                                JSON_OBJECT('category_id', OLD.category_id,'category_name',OLD.category_name,'description',OLD.description,'version',OLD.version,'updated_at',OLD.updated_at,'deleted',OLD.deleted)
                               );
                        END$$
                        DELIMITER ;

-- supplier_info
                        DROP TRIGGER IF EXISTS trg_supplier_info_ai;
                        DELIMITER $$
                        CREATE TRIGGER trg_supplier_info_ai AFTER INSERT ON supplier_info
                            FOR EACH ROW
                        BEGIN
                            INSERT INTO change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
                            VALUES ('MYSQL','supplier_info','INSERT', NEW.supplier_id, NEW.version, NEW.updated_at,
                                    JSON_OBJECT('supplier_id',NEW.supplier_id,'supplier_name',NEW.supplier_name,'contact_name',NEW.contact_name,'contact_phone',NEW.contact_phone,'address',NEW.address,'email',NEW.email,'version',NEW.version,'updated_at',NEW.updated_at,'deleted',NEW.deleted)
                                   );
                            END$$
                            DELIMITER ;

                            DROP TRIGGER IF EXISTS trg_supplier_info_au;
                            DELIMITER $$
                            CREATE TRIGGER trg_supplier_info_au AFTER UPDATE ON supplier_info
                                FOR EACH ROW
                            BEGIN
                                INSERT INTO change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
                                VALUES ('MYSQL','supplier_info','UPDATE', NEW.supplier_id, NEW.version, NEW.updated_at,
                                        JSON_OBJECT('supplier_id',NEW.supplier_id,'supplier_name',NEW.supplier_name,'contact_name',NEW.contact_name,'contact_phone',NEW.contact_phone,'address',NEW.address,'email',NEW.email,'version',NEW.version,'updated_at',NEW.updated_at,'deleted',NEW.deleted)
                                       );
                                END$$
                                DELIMITER ;

                                DROP TRIGGER IF EXISTS trg_supplier_info_ad;
                                DELIMITER $$
                                CREATE TRIGGER trg_supplier_info_ad AFTER DELETE ON supplier_info
                                    FOR EACH ROW
                                BEGIN
                                    INSERT INTO change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
                                    VALUES ('MYSQL','supplier_info','DELETE', OLD.supplier_id, OLD.version, OLD.updated_at,
                                            JSON_OBJECT('supplier_id',OLD.supplier_id,'supplier_name',OLD.supplier_name,'contact_name',OLD.contact_name,'contact_phone',OLD.contact_phone,'address',OLD.address,'email',OLD.email,'version',OLD.version,'updated_at',OLD.updated_at,'deleted',OLD.deleted)
                                           );
                                    END$$
                                    DELIMITER ;

-- product_info
                                    DROP TRIGGER IF EXISTS trg_product_info_ai;
                                    DELIMITER $$
                                    CREATE TRIGGER trg_product_info_ai AFTER INSERT ON product_info
                                        FOR EACH ROW
                                    BEGIN
                                        INSERT INTO change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
                                        VALUES ('MYSQL','product_info','INSERT', NEW.product_id, NEW.version, NEW.updated_at,
                                                JSON_OBJECT('product_id',NEW.product_id,'product_name',NEW.product_name,'category_id',NEW.category_id,'supplier_id',NEW.supplier_id,'price',NEW.price,'stock',NEW.stock,'description',NEW.description,'listed_at',NEW.listed_at,'version',NEW.version,'updated_at',NEW.updated_at,'deleted',NEW.deleted)
                                               );
                                        END$$
                                        DELIMITER ;

                                        DROP TRIGGER IF EXISTS trg_product_info_au;
                                        DELIMITER $$
                                        CREATE TRIGGER trg_product_info_au AFTER UPDATE ON product_info
                                            FOR EACH ROW
                                        BEGIN
                                            INSERT INTO change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
                                            VALUES ('MYSQL','product_info','UPDATE', NEW.product_id, NEW.version, NEW.updated_at,
                                                    JSON_OBJECT('product_id',NEW.product_id,'product_name',NEW.product_name,'category_id',NEW.category_id,'supplier_id',NEW.supplier_id,'price',NEW.price,'stock',NEW.stock,'description',NEW.description,'listed_at',NEW.listed_at,'version',NEW.version,'updated_at',NEW.updated_at,'deleted',NEW.deleted)
                                                   );
                                            END$$
                                            DELIMITER ;

                                            DROP TRIGGER IF EXISTS trg_product_info_ad;
                                            DELIMITER $$
                                            CREATE TRIGGER trg_product_info_ad AFTER DELETE ON product_info
                                                FOR EACH ROW
                                            BEGIN
                                                INSERT INTO change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
                                                VALUES ('MYSQL','product_info','DELETE', OLD.product_id, OLD.version, OLD.updated_at,
                                                        JSON_OBJECT('product_id',OLD.product_id,'product_name',OLD.product_name,'category_id',OLD.category_id,'supplier_id',OLD.supplier_id,'price',OLD.price,'stock',OLD.stock,'description',OLD.description,'listed_at',OLD.listed_at,'version',OLD.version,'updated_at',OLD.updated_at,'deleted',OLD.deleted)
                                                       );
                                                END$$
                                                DELIMITER ;

-- order_info
                                                DROP TRIGGER IF EXISTS trg_order_info_ai;
                                                DELIMITER $$
                                                CREATE TRIGGER trg_order_info_ai AFTER INSERT ON order_info
                                                    FOR EACH ROW
                                                BEGIN
                                                    INSERT INTO change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
                                                    VALUES ('MYSQL','order_info','INSERT', NEW.order_id, NEW.version, NEW.updated_at,
                                                            JSON_OBJECT('order_id',NEW.order_id,'user_id',NEW.user_id,'product_id',NEW.product_id,'quantity',NEW.quantity,'order_status',NEW.order_status,'ordered_at',NEW.ordered_at,'shipping_address',NEW.shipping_address,'version',NEW.version,'updated_at',NEW.updated_at,'deleted',NEW.deleted)
                                                           );
                                                    END$$
                                                    DELIMITER ;

                                                    DROP TRIGGER IF EXISTS trg_order_info_au;
                                                    DELIMITER $$
                                                    CREATE TRIGGER trg_order_info_au AFTER UPDATE ON order_info
                                                        FOR EACH ROW
                                                    BEGIN
                                                        INSERT INTO change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
                                                        VALUES ('MYSQL','order_info','UPDATE', NEW.order_id, NEW.version, NEW.updated_at,
                                                                JSON_OBJECT('order_id',NEW.order_id,'user_id',NEW.user_id,'product_id',NEW.product_id,'quantity',NEW.quantity,'order_status',NEW.order_status,'ordered_at',NEW.ordered_at,'shipping_address',NEW.shipping_address,'version',NEW.version,'updated_at',NEW.updated_at,'deleted',NEW.deleted)
                                                               );
                                                        END$$
                                                        DELIMITER ;

                                                        DROP TRIGGER IF EXISTS trg_order_info_ad;
                                                        DELIMITER $$
                                                        CREATE TRIGGER trg_order_info_ad AFTER DELETE ON order_info
                                                            FOR EACH ROW
                                                        BEGIN
                                                            INSERT INTO change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
                                                            VALUES ('MYSQL','order_info','DELETE', OLD.order_id, OLD.version, OLD.updated_at,
                                                                    JSON_OBJECT('order_id',OLD.order_id,'user_id',OLD.user_id,'product_id',OLD.product_id,'quantity',OLD.quantity,'order_status',OLD.order_status,'ordered_at',OLD.ordered_at,'shipping_address',OLD.shipping_address,'version',OLD.version,'updated_at',OLD.updated_at,'deleted',OLD.deleted)
                                                                   );
                                                            END$$
                                                            DELIMITER ;