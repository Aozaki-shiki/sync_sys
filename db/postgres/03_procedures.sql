-- PostgreSQL：用 FUNCTION 实现“下单事务”
CREATE OR REPLACE FUNCTION sp_place_order(
  p_user_id BIGINT,
  p_product_id BIGINT,
  p_quantity INT,
  p_shipping_address VARCHAR
)
RETURNS BIGINT AS $$
DECLARE
v_stock INT;
  v_order_id BIGINT;
BEGIN
  -- 行级锁
SELECT stock INTO v_stock
FROM product_info
WHERE product_id = p_product_id AND deleted=false
    FOR UPDATE;

IF v_stock IS NULL THEN
    RAISE EXCEPTION 'PRODUCT_NOT_FOUND';
END IF;

  IF v_stock < p_quantity THEN
    RAISE EXCEPTION 'INSUFFICIENT_STOCK';
END IF;

UPDATE product_info
SET stock = stock - p_quantity,
    version = version + 1
WHERE product_id = p_product_id;

INSERT INTO order_info(user_id, product_id, quantity, order_status, shipping_address, version)
VALUES (p_user_id, p_product_id, p_quantity, 'CREATED', p_shipping_address, 1)
    RETURNING order_id INTO v_order_id;

RETURN v_order_id;
END;
$$ LANGUAGE plpgsql;