package com.sss.sync.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplexQueryResponse {
  private List<OrderAnalyticsDTO> data;
  private long totalRecords;
  private int currentPage;
  private int pageSize;
  private int totalPages;
}