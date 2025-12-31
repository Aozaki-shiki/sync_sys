package com.sss.sync.web.controller;

import com.sss.sync.common.api.ApiResponse;
import com.sss.sync.domain.entity.ProductInfo;
import com.sss.sync.service.ProductQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "商品查询相关接口")
public class ProductController {

  private final ProductQueryService productQueryService;

  @GetMapping
  @Operation(summary = "查询所有商品", description = "从 SQL Server 读库查询所有未删除的商品列表")
  public ApiResponse<List<ProductInfo>> list() {
    return ApiResponse.ok(productQueryService.listAll());
  }
}