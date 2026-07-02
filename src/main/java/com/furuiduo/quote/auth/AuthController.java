package com.furuiduo.quote.auth;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "认证", description = "登录、退出与权限码")
@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @Operation(summary = "用户登录", description = "使用工号与密码登录，返回 accessToken。")
  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
    return ApiResponse.ok(authService.login(request));
  }

  @Operation(
      summary = "获取权限码",
      description = "返回当前登录用户的按钮/接口权限码列表。",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/codes")
  public ApiResponse<List<String>> codes(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return ApiResponse.ok(authService.accessCodes(authorization));
  }

  @Operation(
      summary = "退出登录",
      description = "注销当前 accessToken。",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/logout")
  public ApiResponse<String> logout(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    authService.logout(authorization);
    return ApiResponse.ok("");
  }
}
