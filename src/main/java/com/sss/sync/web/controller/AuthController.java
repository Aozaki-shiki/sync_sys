package com.sss.sync.web.controller;

import com.sss.sync.common.api.ApiResponse;
import com.sss.sync.service.AuthService;
import com.sss.sync.web.dto.LoginRequest;
import com.sss.sync.web.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "用户认证相关接口")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回 JWT token")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
    String token = authService.login(req.getUsername(), req.getPassword());
    var user = authService.findUser(req.getUsername());
    return ApiResponse.ok(new LoginResponse(token, user.getUserId(), user.getUsername(), user.getRole()));
  }
}