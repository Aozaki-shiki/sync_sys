package com.sss.sync.infra.mapper.sqlserver;

import org.apache.ibatis.annotations.*;

import java.util.Map;

@Mapper
public interface SqlServerSyncBusinessMapper {

  @Select("""
    SELECT TOP 1 version AS version, updated_at AS updatedAt
    FROM dbo.product_info
    WHERE product_id = #{id}
  """)
  Map<String, Object> getProductMeta(@Param("id") long id);

  @Select("""
    SELECT TOP 1 version AS version, updated_at AS updatedAt
    FROM dbo.order_info
    WHERE order_id = #{id}
  """)
  Map<String, Object> getOrderMeta(@Param("id") long id);

  @Select("""
    SELECT TOP 1 (
      SELECT p.* FOR JSON PATH, WITHOUT_ARRAY_WRAPPER
    ) AS json
    FROM dbo.product_info p
    WHERE product_id = #{id}
  """)
  String getProductAsJson(@Param("id") long id);

  @Select("""
    SELECT TOP 1 (
      SELECT o.* FOR JSON PATH, WITHOUT_ARRAY_WRAPPER
    ) AS json
    FROM dbo.order_info o
    WHERE order_id = #{id}
  """)
  String getOrderAsJson(@Param("id") long id);

  // 原有：只更新
  @Update("""
    UPDATE dbo.product_info
    SET product_name = #{productName},
        category_id = #{categoryId},
        supplier_id = #{supplierId},
        price = #{price},
        stock = #{stock},
        description = #{description},
        listed_at = #{listedAt},
        version = #{version},
        updated_at = #{updatedAt},
        deleted = #{deleted}
    WHERE product_id = #{productId}
  """)
  int updateProduct(Map<String, Object> row);

  @Update("""
    UPDATE dbo.order_info
    SET user_id = #{userId},
        product_id = #{productId},
        quantity = #{quantity},
        order_status = #{orderStatus},
        ordered_at = #{orderedAt},
        shipping_address = #{shippingAddress},
        version = #{version},
        updated_at = #{updatedAt},
        deleted = #{deleted}
    WHERE order_id = #{orderId}
  """)
  int updateOrder(Map<String, Object> row);

  // ✅ 新增：不使用 MERGE 的 UPSERT（UPDATE 不命中则 INSERT）
  @Update("""
    SET NOCOUNT ON;

    UPDATE dbo.product_info
    SET product_name = #{productName},
        category_id = #{categoryId},
        supplier_id = #{supplierId},
        price = #{price},
        stock = #{stock},
        description = #{description},
        listed_at = #{listedAt},
        version = #{version},
        updated_at = #{updatedAt},
        deleted = #{deleted}
    WHERE product_id = #{productId};

    IF @@ROWCOUNT = 0
    BEGIN
      INSERT INTO dbo.product_info(
        product_id, product_name, category_id, supplier_id,
        price, stock, description, listed_at,
        version, updated_at, deleted
      ) VALUES (
        #{productId}, #{productName}, #{categoryId}, #{supplierId},
        #{price}, #{stock}, #{description}, #{listedAt},
        #{version}, #{updatedAt}, #{deleted}
      );
    END
  """)
  int upsertProduct(Map<String, Object> row);

  @Update("""
    SET NOCOUNT ON;

    UPDATE dbo.order_info
    SET user_id = #{userId},
        product_id = #{productId},
        quantity = #{quantity},
        order_status = #{orderStatus},
        ordered_at = #{orderedAt},
        shipping_address = #{shippingAddress},
        version = #{version},
        updated_at = #{updatedAt},
        deleted = #{deleted}
    WHERE order_id = #{orderId};

    IF @@ROWCOUNT = 0
    BEGIN
      INSERT INTO dbo.order_info(
        order_id, user_id, product_id, quantity, order_status,
        ordered_at, shipping_address,
        version, updated_at, deleted
      ) VALUES (
        #{orderId}, #{userId}, #{productId}, #{quantity}, #{orderStatus},
        #{orderedAt}, #{shippingAddress},
        #{version}, #{updatedAt}, #{deleted}
      );
    END
  """)
  int upsertOrder(Map<String, Object> row);
}