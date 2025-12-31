package com.sss.sync.web.dto;

import com.sss.sync.domain.enums.WriteDb;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlaceOrderRequest {
  @NotNull
  private Long userId;

  @NotNull
  private Long productId;

  @NotNull
  @Min(1)
  private Integer quantity;

  @NotBlank
  private String shippingAddress;

  @NotNull
  private WriteDb writeDb; // MYSQL / POSTGRES
}