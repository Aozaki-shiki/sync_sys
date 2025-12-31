package com.sss.sync.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_info")
public class OrderInfo {
  @TableId
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