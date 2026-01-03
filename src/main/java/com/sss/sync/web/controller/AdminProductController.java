package com.sss.sync.web.controller;

import com.sss.sync.common.api.ApiResponse;
import com.sss.sync.domain.entity.ProductInfo;
import com.sss.sync.service.ProductAdminService;
import com.sss.sync.web.dto.ProductListResponse;
import com.sss.sync.web.dto.UpdateProductRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

  private final ProductAdminService productAdminService;

  /**
   * List products with optional search
   * Admin only
   */
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<ProductListResponse> listProducts(@RequestParam(required = false) String search) {
    log.info("Admin listing products with search: {}", search);
    List<ProductInfo> products = productAdminService.listProducts(search);
    return ApiResponse.ok(new ProductListResponse(products));
  }

  /**
   * Update product name, price, and stock
   * Admin only
   */
  @PutMapping("/{productId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Void> updateProduct(@PathVariable Long productId, @Valid @RequestBody UpdateProductRequest request) {
    log.info("Admin updating product {} on {} with: name={}, price={}, stock={}",
      productId, request.getWriteDb(), request.getProductName(), request.getPrice(), request.getStock());
    
    productAdminService.updateProduct(productId, request.getWriteDb(),
      request.getProductName(), request.getPrice(), request.getStock());
    
    return ApiResponse.ok(null);
  }
}
