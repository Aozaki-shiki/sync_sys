package com.sss.sync.web.controller;

import com.sss.sync.common.api.ApiResponse;
import com.sss.sync.service.AuthService;
import com.sss.sync.web.dto.LoginRequest;
import com.sss.sync.web.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
    String token = authService.login(req.getUsername(), req.getPassword());
    var user = authService.findUser(req.getUsername());
    return ApiResponse.ok(new LoginResponse(token, user.getUserId(), user.getUsername(), user.getRole()));
  }
}