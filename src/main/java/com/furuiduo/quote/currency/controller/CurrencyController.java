package com.furuiduo.quote.currency.controller;

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
import com.furuiduo.quote.currency.dto.CurrencyResponse;
import com.furuiduo.quote.currency.dto.CurrencySaveRequest;
import com.furuiduo.quote.currency.service.CurrencyCommandService;
import com.furuiduo.quote.currency.service.CurrencyQueryService;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "币种", description = "币种主数据")
@RestController
@RequestMapping("/currencies")
public class CurrencyController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final CurrencyQueryService currencyQueryService;
  private final CurrencyCommandService currencyCommandService;

  public CurrencyController(
      AuthService authService,
      PermissionService permissionService,
      CurrencyQueryService currencyQueryService,
      CurrencyCommandService currencyCommandService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.currencyQueryService = currencyQueryService;
    this.currencyCommandService = currencyCommandService;
  }

  @Operation(
      summary = "币种列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<List<CurrencyResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Integer status) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(currencyQueryService.list(code, name, status));
  }

  @Operation(
      summary = "币种详情",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}")
  public ApiResponse<CurrencyResponse> get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(currencyCommandService.getById(id));
  }

  @Operation(
      summary = "新建币种",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<CurrencyResponse> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody CurrencySaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(currencyCommandService.create(request));
  }

  @Operation(
      summary = "更新币种",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<CurrencyResponse> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody CurrencySaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(currencyCommandService.update(id, request));
  }

  @Operation(
      summary = "删除币种",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireManage(authService.requireUser(authorization));
    currencyCommandService.delete(id);
    return ApiResponse.ok(null);
  }

  private void requireView(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.CURRENCY_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireManage(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.CURRENCY_MANAGE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }
}
