package com.sss.sync.infra.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sss.sync.domain.entity.ProductInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MysqlProductMapper extends BaseMapper<ProductInfo> {

  // 用 SQL 保证库存不会减成负数（并发安全的一种简单写法）
  @Update("""
    UPDATE product_info
    SET stock = stock - #{qty},
        version = version + 1
    WHERE product_id = #{productId}
      AND deleted = 0
      AND stock >= #{qty}
  """)
  int decreaseStockIfEnough(@Param("productId") Long productId, @Param("qty") int qty);
}