USE sss_db;

-- 初始数据（密码 hash 后续由后端生成，这里先占位）
INSERT INTO user_info(username, password_hash, email, role) VALUES
('admin', '{bcrypt}$2a$10$placeholderadmin', 'admin@example.com', 'ADMIN'),
('user1', '{bcrypt}$2a$10$placeholderuser', 'user1@example.com', 'USER');

INSERT INTO category_info(category_name, description) VALUES
('食品', '可食用商品'),
('数码', '电子数码产品');

INSERT INTO supplier_info(supplier_name, contact_name, contact_phone, address, email) VALUES
('供应商A', '张三', '13800000000', '北京市海淀区', 'a@supplier.com'),
('供应商B', '李四', '13900000000', '上海市浦东新区', 'b@supplier.com');

INSERT INTO product_info(product_name, category_id, supplier_id, price, stock, description) VALUES
('苹果', 1, 1, 3.50, 1000, '新鲜苹果'),
('键盘', 2, 2, 199.00, 200, '机械键盘');