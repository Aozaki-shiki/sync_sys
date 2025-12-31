package com.sss.sync.infra.mapper.postgres;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sss.sync.domain.entity.ProductInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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
}