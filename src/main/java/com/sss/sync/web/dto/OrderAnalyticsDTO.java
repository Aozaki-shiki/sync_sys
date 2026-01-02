package com.sss.sync.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderAnalyticsDTO {
  private String categoryName;
  private String supplierName;
  private Long totalOrders;
  private Long totalQuantity;
  private BigDecimal totalRevenue;
  private BigDecimal avgOrderValue;
  private Long uniqueCustomers;
  private String topProduct;
}
