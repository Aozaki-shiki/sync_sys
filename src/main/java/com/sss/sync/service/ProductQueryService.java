package com.sss.sync.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sss.sync.domain.entity.ProductInfo;
import com.sss.sync.infra.mapper.read.ReadProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductQueryService {

  private final ReadProductMapper readProductMapper;

  public List<ProductInfo> listAll() {
    return readProductMapper.selectList(new LambdaQueryWrapper<ProductInfo>()
      .eq(ProductInfo::getDeleted, false)
      .orderByDesc(ProductInfo::getProductId));
  }
}