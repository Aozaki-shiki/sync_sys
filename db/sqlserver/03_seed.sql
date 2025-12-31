USE sss_db;
GO

INSERT INTO dbo.user_info(username, password_hash, email, role) VALUES
(N'admin', N'{bcrypt}$2a$10$placeholderadmin', N'admin@example.com', N'ADMIN'),
(N'user1', N'{bcrypt}$2a$10$placeholderuser', N'user1@example.com', N'USER');

INSERT INTO dbo.category_info(category_name, description) VALUES
(N'食品', N'可食用商品'),
(N'数码', N'电子数码产品');

INSERT INTO dbo.supplier_info(supplier_name, contact_name, contact_phone, address, email) VALUES
(N'供应商A', N'张三', N'13800000000', N'北京市海淀区', N'a@supplier.com'),
(N'供应商B', N'李四', N'13900000000', N'上海市浦东新区', N'b@supplier.com');

INSERT INTO dbo.product_info(product_name, category_id, supplier_id, price, stock, description) VALUES
(N'苹果', 1, 1, 3.50, 1000, N'新鲜苹果'),
(N'键盘', 2, 2, 199.00, 200, N'机械键盘');
GO