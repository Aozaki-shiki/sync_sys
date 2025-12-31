package com.sss.sync.service;

import com.sss.sync.common.exception.BizException;
import com.sss.sync.domain.entity.OrderInfo;
import com.sss.sync.domain.enums.WriteDb;
import com.sss.sync.infra.mapper.mysql.MysqlOrderMapper;
import com.sss.sync.infra.mapper.mysql.MysqlProductMapper;
import com.sss.sync.infra.mapper.postgres.PostgresOrderMapper;
import com.sss.sync.infra.mapper.postgres.PostgresProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

  private final MysqlOrderMapper mysqlOrderMapper;
  private final MysqlProductMapper mysqlProductMapper;

  private final PostgresOrderMapper postgresOrderMapper;
  private final PostgresProductMapper postgresProductMapper;

  @Transactional(transactionManager = "mysqlTxManager", rollbackFor = Exception.class)
  public Long placeOrderOnMysql(Long userId, Long productId, int qty, String address) {
    int affected = mysqlProductMapper.decreaseStockIfEnough(productId, qty);
    if (affected != 1) {
      throw BizException.of(400, "INSUFFICIENT_STOCK_OR_PRODUCT_NOT_FOUND");
    }

    OrderInfo o = new OrderInfo();
    o.setUserId(userId);
    o.setProductId(productId);
    o.setQuantity(qty);
    o.setOrderStatus("CREATED");
    o.setShippingAddress(address);
    o.setVersion(1L);
    // orderedAt/updatedAt 由数据库默认值处理也可以，这里不手动填

    mysqlOrderMapper.insertOrder(o);
    return o.getOrderId();
  }

  @Transactional(transactionManager = "postgresTxManager", rollbackFor = Exception.class)
  public Long placeOrderOnPostgres(Long userId, Long productId, int qty, String address) {
    int affected = postgresProductMapper.decreaseStockIfEnough(productId, qty);
    if (affected != 1) {
      throw BizException.of(400, "INSUFFICIENT_STOCK_OR_PRODUCT_NOT_FOUND");
    }

    OrderInfo o = new OrderInfo();
    o.setUserId(userId);
    o.setProductId(productId);
    o.setQuantity(qty);
    o.setOrderStatus("CREATED");
    o.setShippingAddress(address);
    o.setVersion(1L);

    postgresOrderMapper.insertOrder(o);
    return o.getOrderId();
  }

  public Long placeOrder(WriteDb writeDb, Long userId, Long productId, int qty, String address) {
    return switch (writeDb) {
      case MYSQL -> placeOrderOnMysql(userId, productId, qty, address);
      case POSTGRES -> placeOrderOnPostgres(userId, productId, qty, address);
    };
  }
}