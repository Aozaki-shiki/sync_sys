-- Using explicit IDs for Snowflake ID compatibility
INSERT INTO user_info(user_id, username, password_hash, email, role) VALUES
(1000000000000000001, 'admin', '{bcrypt}$2a$10$placeholderadmin', 'admin@example.com', 'ADMIN'),
(1000000000000000002, 'user1', '{bcrypt}$2a$10$placeholderuser', 'user1@example.com', 'USER');

INSERT INTO category_info(category_id, category_name, description) VALUES
(1000000000000000001, '食品', '可食用商品'),
(1000000000000000002, '数码', '电子数码产品');

INSERT INTO supplier_info(supplier_id, supplier_name, contact_name, contact_phone, address, email) VALUES
(1000000000000000001, '供应商A', '张三', '13800000000', '北京市海淀区', 'a@supplier.com'),
(1000000000000000002, '供应商B', '李四', '13900000000', '上海市浦东新区', 'b@supplier.com');

INSERT INTO product_info(product_id, product_name, category_id, supplier_id, price, stock, description) VALUES
(1000000000000000001, '苹果', 1000000000000000001, 1000000000000000001, 3.50, 1000, '新鲜苹果'),
(1000000000000000002, '键盘', 1000000000000000002, 1000000000000000002, 199.00, 200, '机械键盘');