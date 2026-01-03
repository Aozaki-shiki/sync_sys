package com.sss.sync.web.controller;

import com.sss.sync.common.api.ApiResponse;
import com.sss.sync.service.ComplexQueryService;
import com.sss.sync.web.dto.ComplexQueryRequest;
import com.sss.sync.web.dto.ComplexQueryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/queries")
@RequiredArgsConstructor
public class ComplexQueryController {

  private final ComplexQueryService complexQueryService;

  /**
   * API endpoint for complex order analytics query
   * Requires ADMIN role for access
   */
  @PostMapping("/order-analytics")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<ComplexQueryResponse> queryOrderAnalytics(@Valid @RequestBody ComplexQueryRequest request) {
    log.info("Complex query API called by admin");
    ComplexQueryResponse response = complexQueryService.queryOrderAnalytics(request);
    return ApiResponse.ok(response);
  }
}