package com.sss.sync.infra.mapper.postgres;

import org.apache.ibatis.annotations.*;

import java.util.Map;

@Mapper
public interface PostgresSyncBusinessMapper {

  @Select("""
    SELECT version AS "version", updated_at AS "updatedAt"
    FROM product_info
    WHERE product_id = #{id}
    LIMIT 1
  """)
  Map<String, Object> getProductMeta(@Param("id") long id);

  @Select("""
    SELECT version AS "version", updated_at AS "updatedAt"
    FROM order_info
    WHERE order_id = #{id}
    LIMIT 1
  """)
  Map<String, Object> getOrderMeta(@Param("id") long id);

  @Select("""
    SELECT to_jsonb(p)::text AS json
    FROM product_info p
    WHERE product_id = #{id}
    LIMIT 1
  """)
  String getProductAsJson(@Param("id") long id);

  @Select("""
    SELECT to_jsonb(o)::text AS json
    FROM order_info o
    WHERE order_id = #{id}
    LIMIT 1
  """)
  String getOrderAsJson(@Param("id") long id);

  @Insert("""
    INSERT INTO product_info(
      product_id, product_name, category_id, supplier_id, price, stock, description, listed_at,
      version, updated_at, deleted
    )
    VALUES(
      #{productId}, #{productName}, #{categoryId}, #{supplierId}, #{price}, #{stock}, #{description}, #{listedAt},
      #{version}, #{updatedAt}, #{deleted}
    )
    ON CONFLICT (product_id) DO UPDATE SET
      product_name = EXCLUDED.product_name,
      category_id = EXCLUDED.category_id,
      supplier_id = EXCLUDED.supplier_id,
      price = EXCLUDED.price,
      stock = EXCLUDED.stock,
      description = EXCLUDED.description,
      listed_at = EXCLUDED.listed_at,
      version = EXCLUDED.version,
      updated_at = EXCLUDED.updated_at,
      deleted = EXCLUDED.deleted
  """)
  int upsertProduct(Map<String, Object> row);

  @Insert("""
    INSERT INTO order_info(
      order_id, user_id, product_id, quantity, order_status, ordered_at, shipping_address,
      version, updated_at, deleted
    )
    VALUES(
      #{orderId}, #{userId}, #{productId}, #{quantity}, #{orderStatus}, #{orderedAt}, #{shippingAddress},
      #{version}, #{updatedAt}, #{deleted}
    )
    ON CONFLICT (order_id) DO UPDATE SET
      user_id = EXCLUDED.user_id,
      product_id = EXCLUDED.product_id,
      quantity = EXCLUDED.quantity,
      order_status = EXCLUDED.order_status,
      ordered_at = EXCLUDED.ordered_at,
      shipping_address = EXCLUDED.shipping_address,
      version = EXCLUDED.version,
      updated_at = EXCLUDED.updated_at,
      deleted = EXCLUDED.deleted
  """)
  int upsertOrder(Map<String, Object> row);

  @Update("SET LOCAL sss.skip_changelog = '1'")
  void setSkipChangeLog();

  @Update("SET LOCAL sss.skip_changelog = '0'")
  void clearSkipChangeLog();
}