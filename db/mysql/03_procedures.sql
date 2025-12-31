USE sss_db;

-- 存储过程：下单（事务+库存检测+回滚）
-- 注意：你后端会用 Spring 事务做一遍；这里是为了满足“数据库端程序对象/事务”要求。

DROP PROCEDURE IF EXISTS sp_place_order;
DELIMITER $$
CREATE PROCEDURE sp_place_order(
  IN p_user_id BIGINT,
  IN p_product_id BIGINT,
  IN p_quantity INT,
  IN p_shipping_address VARCHAR(255),
  OUT o_order_id BIGINT
)
BEGIN
  DECLARE v_stock INT;

  START TRANSACTION;

  -- 行级锁：避免并发超卖
  SELECT stock INTO v_stock
  FROM product_info
  WHERE product_id = p_product_id AND deleted=0
  FOR UPDATE;

  IF v_stock IS NULL THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'PRODUCT_NOT_FOUND';
  END IF;

  IF v_stock < p_quantity THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'INSUFFICIENT_STOCK';
  END IF;

  UPDATE product_info
  SET stock = stock - p_quantity,
      version = version + 1
  WHERE product_id = p_product_id;

  INSERT INTO order_info(user_id, product_id, quantity, order_status, shipping_address, version)
  VALUES (p_user_id, p_product_id, p_quantity, 'CREATED', p_shipping_address, 1);

  SET o_order_id = LAST_INSERT_ID();

  COMMIT;
END$$
DELIMITER ;