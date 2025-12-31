package com.sss.sync.infra.mapper.read;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sss.sync.domain.entity.ProductInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReadProductMapper extends BaseMapper<ProductInfo> {

  @Select("""
    SELECT product_id, product_name, category_id, supplier_id, price, stock, 
           description, listed_at, version, updated_at, deleted
    FROM product_info
    WHERE deleted = 0
    ORDER BY product_id DESC
  """)
  List<ProductInfo> selectAllActive();
}