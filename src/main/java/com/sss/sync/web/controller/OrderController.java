package com.sss.sync.web.controller;

import com.sss.sync.common.api.ApiResponse;
import com.sss.sync.service.OrderService;
import com.sss.sync.web.dto.PlaceOrderRequest;
import com.sss.sync.web.dto.PlaceOrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  @PostMapping("/place")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public ApiResponse<PlaceOrderResponse> place(@Valid @RequestBody PlaceOrderRequest req) {
    Long orderId = orderService.placeOrder(req.getWriteDb(), req.getUserId(), req.getProductId(), req.getQuantity(), req.getShippingAddress());
    return ApiResponse.ok(new PlaceOrderResponse(orderId, req.getWriteDb().name()));
  }
}