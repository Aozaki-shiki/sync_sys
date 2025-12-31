USE sss_db;

-- 仅示例：创建应用账号，最小权限（你写报告用）
-- 注意：生产环境不要用 root；这里为了课程演示。
CREATE USER IF NOT EXISTS 'sss_app'@'%' IDENTIFIED BY 'AppPassWord++';
CREATE USER IF NOT EXISTS 'sss_ro'@'%' IDENTIFIED BY 'ReadOnlyPassWord++';

-- 读写账号（写库）
GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE ON sss_db.* TO 'sss_app'@'%';

-- 只读账号（读库/报表）
GRANT SELECT ON sss_db.* TO 'sss_ro'@'%';

FLUSH PRIVILEGES;