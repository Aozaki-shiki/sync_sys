package com.sss.sync.web.controller;

import com.sss.sync.common.api.ApiResponse;
import com.sss.sync.domain.entity.ProductInfo;
import com.sss.sync.service.ProductQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

  private final ProductQueryService productQueryService;

  @GetMapping
  public ApiResponse<List<ProductInfo>> list() {
    return ApiResponse.ok(productQueryService.listAll());
  }
}