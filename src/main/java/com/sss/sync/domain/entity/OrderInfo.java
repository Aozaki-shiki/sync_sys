package com.sss.sync.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderInfo {
  private Long orderId;
  private Long userId;
  private Long productId;
  private Integer quantity;
  private String orderStatus;
  private LocalDateTime orderedAt;
  private String shippingAddress;

  private Long version;
  private LocalDateTime updatedAt;
  private Boolean deleted;
}