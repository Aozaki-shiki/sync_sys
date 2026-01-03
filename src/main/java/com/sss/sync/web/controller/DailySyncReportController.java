package com.sss.sync.web.controller;

import com.sss.sync.common.api.ApiResponse;
import com.sss.sync.service.DailySyncReportService;
import com.sss.sync.web.dto.DailySyncStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class DailySyncReportController {
  
  private final DailySyncReportService dailySyncReportService;
  
  /**
   * API endpoint for daily sync statistics
   * Requires ADMIN role for access
   */
  @GetMapping("/daily-sync")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<DailySyncStatsResponse> getDailySyncStats(
      @RequestParam(defaultValue = "30") int days) {
    log.info("Daily sync stats API called for {} days", days);
    DailySyncStatsResponse response = dailySyncReportService.getDailySyncStats(days);
    return ApiResponse.ok(response);
  }
}