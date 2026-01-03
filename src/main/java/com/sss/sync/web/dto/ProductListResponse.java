package com.sss.sync.web.dto;

import com.sss.sync.domain.entity.ProductInfo;
import lombok.Data;

import java.util.List;

@Data
public class ProductListResponse {
  private List<ProductInfo> products;
  private int total;

  public ProductListResponse(List<ProductInfo> products) {
    this.products = products;
    this.total = products.size();
  }
}
