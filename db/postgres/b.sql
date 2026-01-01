-- 依赖已存在的 fn_log_change()（在之前的 schema_postgres.sql 中）
DROP TRIGGER IF EXISTS trg_role_info_aiud ON role_info;
CREATE TRIGGER trg_role_info_aiud
AFTER INSERT OR UPDATE OR DELETE ON role_info
FOR EACH ROW EXECUTE FUNCTION fn_log_change();