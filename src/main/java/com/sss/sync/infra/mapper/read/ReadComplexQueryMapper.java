package com.sss.sync.infra.mapper.read;

import com.sss.sync.web.dto.OrderAnalyticsDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ReadComplexQueryMapper {
  
  /**
   * Complex business analytics query with:
   * - Multi-table joins (order_info, product_info, category_info, supplier_info, user_info)
   * - Aggregations (COUNT, SUM, AVG)
   * - Nested subquery for top product per category/supplier
   * - Pagination support
   */
  List<OrderAnalyticsDTO> queryOrderAnalytics(
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("categoryName") String categoryName,
      @Param("supplierName") String supplierName,
      @Param("offset") int offset,
      @Param("limit") int limit
  );
  
  /**
   * Count total records for pagination
   */
  long countOrderAnalytics(
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("categoryName") String categoryName,
      @Param("supplierName") String supplierName
  );
}
