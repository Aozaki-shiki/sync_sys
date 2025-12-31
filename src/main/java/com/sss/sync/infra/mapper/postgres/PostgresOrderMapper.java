package com.sss.sync.infra.mapper.postgres;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sss.sync.domain.entity.OrderInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface PostgresOrderMapper extends BaseMapper<OrderInfo> {

  @Insert("""
    INSERT INTO order_info (user_id, product_id, quantity, order_status, shipping_address, version)
    VALUES (#{userId}, #{productId}, #{quantity}, #{orderStatus}, #{shippingAddress}, #{version})
  """)
  @Options(useGeneratedKeys = true, keyProperty = "orderId", keyColumn = "order_id")
  int insertOrder(OrderInfo order);
}