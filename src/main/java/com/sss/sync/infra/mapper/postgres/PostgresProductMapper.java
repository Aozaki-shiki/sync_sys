package com.sss.sync.infra.mapper.postgres;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sss.sync.domain.entity.ProductInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

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

  @Update("""
    UPDATE product_info
    SET product_name = #{productName},
        price = #{price},
        stock = #{stock},
        version = version + 1
    WHERE product_id = #{productId}
      AND deleted = false
  """)
  int updateProductFields(@Param("productId") Long productId,
                          @Param("productName") String productName,
                          @Param("price") BigDecimal price,
                          @Param("stock") Integer stock);
}