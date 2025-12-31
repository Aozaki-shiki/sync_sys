package com.sss.sync.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product_info")
public class ProductInfo {
  @TableId
  private Long productId;
  private String productName;
  private Long categoryId;
  private Long supplierId;
  private BigDecimal price;
  private Integer stock;
  private String description;
  private LocalDateTime listedAt;

  private Long version;
  private LocalDateTime updatedAt;
  private Boolean deleted;
}