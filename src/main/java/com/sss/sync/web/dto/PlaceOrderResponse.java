package com.sss.sync.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlaceOrderResponse {
  private Long orderId;
  private String writeDb;
}