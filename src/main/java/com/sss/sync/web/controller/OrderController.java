package com.sss.sync.web.controller;

import com.sss.sync.common.api.ApiResponse;
import com.sss.sync.service.OrderService;
import com.sss.sync.web.dto.PlaceOrderRequest;
import com.sss.sync.web.dto.PlaceOrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "订单相关接口")
public class OrderController {

  private final OrderService orderService;

  @PostMapping("/place")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  @Operation(summary = "下单", description = "在 MySQL 或 PostgreSQL 写库下单，包含库存扣减和事务回滚", 
             security = @SecurityRequirement(name = "Bearer Authentication"))
  public ApiResponse<PlaceOrderResponse> place(@Valid @RequestBody PlaceOrderRequest req) {
    Long orderId = orderService.placeOrder(req.getWriteDb(), req.getUserId(), req.getProductId(), req.getQuantity(), req.getShippingAddress());
    return ApiResponse.ok(new PlaceOrderResponse(orderId, req.getWriteDb().name()));
  }
}