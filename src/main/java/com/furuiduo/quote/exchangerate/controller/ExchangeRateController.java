package com.furuiduo.quote.exchangerate.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.auth.AuthService;
import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.exchangerate.dto.ExchangeRateResponse;
import com.furuiduo.quote.exchangerate.dto.ExchangeRateSaveRequest;
import com.furuiduo.quote.exchangerate.service.ExchangeRateCommandService;
import com.furuiduo.quote.exchangerate.service.ExchangeRateQueryService;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "汇率", description = "汇率主数据")
@RestController
@RequestMapping("/exchange-rates")
public class ExchangeRateController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final ExchangeRateQueryService exchangeRateQueryService;
  private final ExchangeRateCommandService exchangeRateCommandService;

  public ExchangeRateController(
      AuthService authService,
      PermissionService permissionService,
      ExchangeRateQueryService exchangeRateQueryService,
      ExchangeRateCommandService exchangeRateCommandService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.exchangeRateQueryService = exchangeRateQueryService;
    this.exchangeRateCommandService = exchangeRateCommandService;
  }

  @Operation(
      summary = "汇率列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<List<ExchangeRateResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String fromCurrency,
      @RequestParam(required = false) String toCurrency,
      @RequestParam(required = false) Integer status) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(exchangeRateQueryService.list(fromCurrency, toCurrency, status));
  }

  @Operation(
      summary = "汇率详情",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}")
  public ApiResponse<ExchangeRateResponse> get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(exchangeRateCommandService.getById(id));
  }

  @Operation(
      summary = "新建汇率",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<ExchangeRateResponse> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody ExchangeRateSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(exchangeRateCommandService.create(request));
  }

  @Operation(
      summary = "更新汇率",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<ExchangeRateResponse> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody ExchangeRateSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(exchangeRateCommandService.update(id, request));
  }

  @Operation(
      summary = "删除汇率",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireManage(authService.requireUser(authorization));
    exchangeRateCommandService.delete(id);
    return ApiResponse.ok(null);
  }

  private void requireView(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.EXCHANGE_RATE_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireManage(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.EXCHANGE_RATE_MANAGE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }
}
