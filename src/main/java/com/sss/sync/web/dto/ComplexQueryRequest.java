package com.sss.sync.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplexQueryRequest {
  
  @NotNull(message = "Start date is required")
  private LocalDate startDate;
  
  @NotNull(message = "End date is required")
  private LocalDate endDate;
  
  private String categoryName;
  
  private String supplierName;
  
  @Min(value = 1, message = "Page must be at least 1")
  private int page = 1;
  
  @Min(value = 1, message = "Page size must be at least 1")
  private int pageSize = 20;
}
