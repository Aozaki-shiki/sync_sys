package com.sss.sync.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sss.sync.common.exception.BizException;
import com.sss.sync.domain.entity.ProductInfo;
import com.sss.sync.domain.enums.WriteDb;
import com.sss.sync.infra.mapper.mysql.MysqlProductMapper;
import com.sss.sync.infra.mapper.postgres.PostgresProductMapper;
import com.sss.sync.infra.mapper.read.ReadProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductAdminService {

  private final MysqlProductMapper mysqlProductMapper;
  private final PostgresProductMapper postgresProductMapper;
  private final ReadProductMapper readProductMapper;

  public List<ProductInfo> listProducts(String searchQuery) {
    LambdaQueryWrapper<ProductInfo> wrapper = new LambdaQueryWrapper<ProductInfo>()
      .eq(ProductInfo::getDeleted, false);

    if (searchQuery != null && !searchQuery.isBlank()) {
      wrapper.like(ProductInfo::getProductName, searchQuery);
    }

    wrapper.orderByDesc(ProductInfo::getProductId);
    return readProductMapper.selectList(wrapper);
  }

  public void updateProduct(Long productId, WriteDb writeDb, String productName, BigDecimal price, Integer stock) {
    switch (writeDb) {
      case MYSQL -> updateProductOnMysql(productId, productName, price, stock);
      case POSTGRES -> updateProductOnPostgres(productId, productName, price, stock);
    }
  }

  @Transactional(transactionManager = "mysqlTxManager", rollbackFor = Exception.class)
  public void updateProductOnMysql(Long productId, String productName, BigDecimal price, Integer stock) {
    int affected = mysqlProductMapper.updateProductFields(productId, productName, price, stock);
    if (affected != 1) {
      throw BizException.of(404, "PRODUCT_NOT_FOUND");
    }
  }

  @Transactional(transactionManager = "postgresTxManager", rollbackFor = Exception.class)
  public void updateProductOnPostgres(Long productId, String productName, BigDecimal price, Integer stock) {
    int affected = postgresProductMapper.updateProductFields(productId, productName, price, stock);
    if (affected != 1) {
      throw BizException.of(404, "PRODUCT_NOT_FOUND");
    }
  }
}