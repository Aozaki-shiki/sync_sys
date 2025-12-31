package com.sss.sync.infra.mapper.mysql;

import org.apache.ibatis.annotations.*;

import java.util.Map;

@Mapper
public interface MysqlSyncBusinessMapper {

  @Select("""
    SELECT version AS version, updated_at AS updatedAt
    FROM product_info
    WHERE product_id = #{id}
    LIMIT 1
  """)
  Map<String, Object> getProductMeta(@Param("id") long id);

  @Select("""
    SELECT version AS version, updated_at AS updatedAt
    FROM order_info
    WHERE order_id = #{id}
    LIMIT 1
  """)
  Map<String, Object> getOrderMeta(@Param("id") long id);

  @Select("""
    SELECT JSON_OBJECT(
      'product_id', product_id,
      'product_name', product_name,
      'category_id', category_id,
      'supplier_id', supplier_id,
      'price', price,
      'stock', stock,
      'description', description,
      'listed_at', listed_at,
      'version', version,
      'updated_at', updated_at,
      'deleted', deleted
    ) AS json
    FROM product_info
    WHERE product_id = #{id}
    LIMIT 1
  """)
  String getProductAsJson(@Param("id") long id);

  @Select("""
    SELECT JSON_OBJECT(
      'order_id', order_id,
      'user_id', user_id,
      'product_id', product_id,
      'quantity', quantity,
      'order_status', order_status,
      'ordered_at', ordered_at,
      'shipping_address', shipping_address,
      'version', version,
      'updated_at', updated_at,
      'deleted', deleted
    ) AS json
    FROM order_info
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
    ON DUPLICATE KEY UPDATE
      product_name = VALUES(product_name),
      category_id = VALUES(category_id),
      supplier_id = VALUES(supplier_id),
      price = VALUES(price),
      stock = VALUES(stock),
      description = VALUES(description),
      listed_at = VALUES(listed_at),
      version = VALUES(version),
      updated_at = VALUES(updated_at),
      deleted = VALUES(deleted)
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
    ON DUPLICATE KEY UPDATE
      user_id = VALUES(user_id),
      product_id = VALUES(product_id),
      quantity = VALUES(quantity),
      order_status = VALUES(order_status),
      ordered_at = VALUES(ordered_at),
      shipping_address = VALUES(shipping_address),
      version = VALUES(version),
      updated_at = VALUES(updated_at),
      deleted = VALUES(deleted)
  """)
  int upsertOrder(Map<String, Object> row);
}