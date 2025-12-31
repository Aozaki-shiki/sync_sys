-- 在 sss_db 执行
-- PostgreSQL：使用 plpgsql trigger function + trigger

-- 统一更新 updated_at
CREATE OR REPLACE FUNCTION fn_touch_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 写入 change_log（单表一个触发器函数更直观，课程验收也好讲）
CREATE OR REPLACE FUNCTION fn_log_user_info()
RETURNS TRIGGER AS $$
DECLARE
  v_op TEXT;
  v_pk TEXT;
  v_ver BIGINT;
  v_up TIMESTAMP(3);
  v_payload JSONB;
BEGIN
  IF TG_OP = 'INSERT' THEN
    v_op := 'INSERT';
    v_pk := NEW.user_id::TEXT;
    v_ver := NEW.version;
    v_up := NEW.updated_at;
    v_payload := to_jsonb(NEW);
  ELSIF TG_OP = 'UPDATE' THEN
    v_op := 'UPDATE';
    v_pk := NEW.user_id::TEXT;
    v_ver := NEW.version;
    v_up := NEW.updated_at;
    v_payload := to_jsonb(NEW);
  ELSE
    v_op := 'DELETE';
    v_pk := OLD.user_id::TEXT;
    v_ver := OLD.version;
    v_up := OLD.updated_at;
    v_payload := to_jsonb(OLD);
  END IF;

  INSERT INTO change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
  VALUES ('POSTGRES', 'user_info', v_op, v_pk, v_ver, v_up, v_payload);

  RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- user_info: before update touch time + after change log
DROP TRIGGER IF EXISTS trg_user_info_touch ON user_info;
CREATE TRIGGER trg_user_info_touch
BEFORE UPDATE ON user_info
FOR EACH ROW EXECUTE FUNCTION fn_touch_updated_at();

DROP TRIGGER IF EXISTS trg_user_info_log ON user_info;
CREATE TRIGGER trg_user_info_log
AFTER INSERT OR UPDATE OR DELETE ON user_info
FOR EACH ROW EXECUTE FUNCTION fn_log_user_info();

-- 为控制篇幅：其余表同样模式（我给你“完整”，但用一套通用函数：减少重复）
CREATE OR REPLACE FUNCTION fn_log_generic()
RETURNS TRIGGER AS $$
DECLARE
  v_op TEXT;
  v_pk TEXT;
  v_ver BIGINT;
  v_up TIMESTAMP(3);
  v_payload JSONB;
  v_table TEXT := TG_TABLE_NAME;
BEGIN
  IF TG_OP = 'INSERT' THEN
    v_op := 'INSERT';
    v_payload := to_jsonb(NEW);
    v_ver := (NEW).version;
    v_up := (NEW).updated_at;
  ELSIF TG_OP = 'UPDATE' THEN
    v_op := 'UPDATE';
    v_payload := to_jsonb(NEW);
    v_ver := (NEW).version;
    v_up := (NEW).updated_at;
  ELSE
    v_op := 'DELETE';
    v_payload := to_jsonb(OLD);
    v_ver := (OLD).version;
    v_up := (OLD).updated_at;
  END IF;

  -- 约定主键列名：xxx_id
  IF v_table = 'category_info' THEN
    v_pk := COALESCE((to_jsonb(COALESCE(NEW, OLD)) ->> 'category_id'), '');
  ELSIF v_table = 'supplier_info' THEN
    v_pk := COALESCE((to_jsonb(COALESCE(NEW, OLD)) ->> 'supplier_id'), '');
  ELSIF v_table = 'product_info' THEN
    v_pk := COALESCE((to_jsonb(COALESCE(NEW, OLD)) ->> 'product_id'), '');
  ELSIF v_table = 'order_info' THEN
    v_pk := COALESCE((to_jsonb(COALESCE(NEW, OLD)) ->> 'order_id'), '');
  ELSE
    v_pk := COALESCE((to_jsonb(COALESCE(NEW, OLD)) ->> 'id'), '');
  END IF;

  INSERT INTO change_log(db_code, table_name, op_type, pk_value, row_version, row_updated_at, payload_json)
  VALUES ('POSTGRES', v_table, v_op, v_pk, v_ver, v_up, v_payload);

  RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- touch triggers
DROP TRIGGER IF EXISTS trg_category_info_touch ON category_info;
CREATE TRIGGER trg_category_info_touch BEFORE UPDATE ON category_info FOR EACH ROW EXECUTE FUNCTION fn_touch_updated_at();
DROP TRIGGER IF EXISTS trg_supplier_info_touch ON supplier_info;
CREATE TRIGGER trg_supplier_info_touch BEFORE UPDATE ON supplier_info FOR EACH ROW EXECUTE FUNCTION fn_touch_updated_at();
DROP TRIGGER IF EXISTS trg_product_info_touch ON product_info;
CREATE TRIGGER trg_product_info_touch BEFORE UPDATE ON product_info FOR EACH ROW EXECUTE FUNCTION fn_touch_updated_at();
DROP TRIGGER IF EXISTS trg_order_info_touch ON order_info;
CREATE TRIGGER trg_order_info_touch BEFORE UPDATE ON order_info FOR EACH ROW EXECUTE FUNCTION fn_touch_updated_at();

-- log triggers
DROP TRIGGER IF EXISTS trg_category_info_log ON category_info;
CREATE TRIGGER trg_category_info_log AFTER INSERT OR UPDATE OR DELETE ON category_info FOR EACH ROW EXECUTE FUNCTION fn_log_generic();
DROP TRIGGER IF EXISTS trg_supplier_info_log ON supplier_info;
CREATE TRIGGER trg_supplier_info_log AFTER INSERT OR UPDATE OR DELETE ON supplier_info FOR EACH ROW EXECUTE FUNCTION fn_log_generic();
DROP TRIGGER IF EXISTS trg_product_info_log ON product_info;
CREATE TRIGGER trg_product_info_log AFTER INSERT OR UPDATE OR DELETE ON product_info FOR EACH ROW EXECUTE FUNCTION fn_log_generic();
DROP TRIGGER IF EXISTS trg_order_info_log ON order_info;
CREATE TRIGGER trg_order_info_log AFTER INSERT OR UPDATE OR DELETE ON order_info FOR EACH ROW EXECUTE FUNCTION fn_log_generic();