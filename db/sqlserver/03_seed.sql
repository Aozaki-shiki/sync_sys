USE sss_db;
GO

-- Using explicit IDs for Snowflake ID compatibility
INSERT INTO dbo.user_info(user_id, username, password_hash, email, role) VALUES
(1000000000000000001, N'admin', N'{bcrypt}$2a$10$placeholderadmin', N'admin@example.com', N'ADMIN'),
(1000000000000000002, N'user1', N'{bcrypt}$2a$10$placeholderuser', N'user1@example.com', N'USER');

INSERT INTO dbo.category_info(category_id, category_name, description) VALUES
(1000000000000000001, N'食品', N'可食用商品'),
(1000000000000000002, N'数码', N'电子数码产品');

INSERT INTO dbo.supplier_info(supplier_id, supplier_name, contact_name, contact_phone, address, email) VALUES
(1000000000000000001, N'供应商A', N'张三', N'13800000000', N'北京市海淀区', N'a@supplier.com'),
(1000000000000000002, N'供应商B', N'李四', N'13900000000', N'上海市浦东新区', N'b@supplier.com');

INSERT INTO dbo.product_info(product_id, product_name, category_id, supplier_id, price, stock, description) VALUES
(1000000000000000001, N'苹果', 1000000000000000001, 1000000000000000001, 3.50, 1000, N'新鲜苹果'),
(1000000000000000002, N'键盘', 1000000000000000002, 1000000000000000002, 199.00, 200, N'机械键盘');
GO