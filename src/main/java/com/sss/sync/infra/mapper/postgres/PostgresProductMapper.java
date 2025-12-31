package com.sss.sync.infra.mapper.postgres;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sss.sync.domain.entity.ProductInfo;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PostgresProductMapper extends BaseMapper<ProductInfo> {

  @Update("""
    UPDATE product_info
    SET stock = stock - #{qty},
        version = version + 1
    WHERE product_id = #{productId}
      AND deleted = false
      AND stock >= #{qty}
  """)
  int decreaseStockIfEnough(@Param("productId") Long productId, @Param("qty") int qty);

  @Select("""
    SELECT product_id, product_name, category_id, supplier_id, price, stock,
           description, listed_at, version, updated_at, deleted
    FROM product_info
    WHERE product_id = #{productId}
  """)
  ProductInfo findById(@Param("productId") Long productId);

  @Insert("""
    INSERT INTO product_info (product_id, product_name, category_id, supplier_id, price, stock,
                              description, listed_at, version, deleted)
    VALUES (#{productId}, #{productName}, #{categoryId}, #{supplierId}, #{price}, #{stock},
            #{description}, #{listedAt}, #{version}, #{deleted})
  """)
  int insertProduct(ProductInfo product);

  @Update("""
    UPDATE product_info
    SET product_name = #{productName}, category_id = #{categoryId}, supplier_id = #{supplierId},
        price = #{price}, stock = #{stock}, description = #{description},
        listed_at = #{listedAt}, version = #{version}, deleted = #{deleted}
    WHERE product_id = #{productId}
  """)
  int updateProduct(ProductInfo product);
}