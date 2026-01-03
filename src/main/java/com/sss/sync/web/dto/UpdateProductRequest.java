package com.sss.sync.web.dto;

import com.sss.sync.domain.enums.WriteDb;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateProductRequest {
  @NotNull
  private WriteDb writeDb; // MYSQL / POSTGRES

  @NotBlank
  private String productName;

  @NotNull
  @Min(0)
  private BigDecimal price;

  @NotNull
  @Min(0)
  private Integer stock;
}
