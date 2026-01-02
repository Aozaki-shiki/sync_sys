package com.sss.sync.service;

import com.sss.sync.infra.mapper.read.ReadComplexQueryMapper;
import com.sss.sync.web.dto.ComplexQueryRequest;
import com.sss.sync.web.dto.ComplexQueryResponse;
import com.sss.sync.web.dto.OrderAnalyticsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplexQueryService {
  
  private final ReadComplexQueryMapper complexQueryMapper;
  
  public ComplexQueryResponse queryOrderAnalytics(ComplexQueryRequest request) {
    log.info("Executing complex query: startDate={}, endDate={}, category={}, supplier={}, page={}, pageSize={}", 
        request.getStartDate(), request.getEndDate(), request.getCategoryName(), 
        request.getSupplierName(), request.getPage(), request.getPageSize());
    
    // Calculate offset for pagination
    int offset = (request.getPage() - 1) * request.getPageSize();
    
    // Execute query
    List<OrderAnalyticsDTO> data = complexQueryMapper.queryOrderAnalytics(
        request.getStartDate(),
        request.getEndDate(),
        request.getCategoryName(),
        request.getSupplierName(),
        offset,
        request.getPageSize()
    );
    
    // Get total count
    long totalRecords = complexQueryMapper.countOrderAnalytics(
        request.getStartDate(),
        request.getEndDate(),
        request.getCategoryName(),
        request.getSupplierName()
    );
    
    // Calculate total pages
    int totalPages = (int) Math.ceil((double) totalRecords / request.getPageSize());
    
    log.info("Query returned {} records out of {} total", data.size(), totalRecords);
    
    return new ComplexQueryResponse(
        data,
        totalRecords,
        request.getPage(),
        request.getPageSize(),
        totalPages
    );
  }
}
