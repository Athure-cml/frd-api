package com.furuiduo.quote.customer.controller;

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
import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.customer.dto.CustomerResponse;
import com.furuiduo.quote.customer.dto.CustomerSaveRequest;
import com.furuiduo.quote.customer.service.CustomerCommandService;
import com.furuiduo.quote.customer.service.CustomerQueryService;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "客户", description = "客户主数据")
@RestController
@RequestMapping("/customers")
public class CustomerController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final CustomerQueryService customerQueryService;
  private final CustomerCommandService customerCommandService;

  public CustomerController(
      AuthService authService,
      PermissionService permissionService,
      CustomerQueryService customerQueryService,
      CustomerCommandService customerCommandService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.customerQueryService = customerQueryService;
    this.customerCommandService = customerCommandService;
  }

  @Operation(
      summary = "客户分页列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<PageResult<CustomerResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Integer status) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(customerQueryService.list(page, pageSize, code, name, status));
  }

  @Operation(
      summary = "客户详情",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}")
  public ApiResponse<CustomerResponse> get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(customerCommandService.getById(id));
  }

  @Operation(
      summary = "新建客户",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<CustomerResponse> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody CustomerSaveRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireCreate(user);
    return ApiResponse.ok(customerCommandService.create(user, request));
  }

  @Operation(
      summary = "更新客户",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<CustomerResponse> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody CustomerSaveRequest request) {
    requireEdit(authService.requireUser(authorization));
    return ApiResponse.ok(customerCommandService.update(id, request));
  }

  @Operation(
      summary = "删除客户",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireDelete(authService.requireUser(authorization));
    customerCommandService.delete(id);
    return ApiResponse.ok(null);
  }

  private void requireView(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.CUSTOMER_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireCreate(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.CUSTOMER_CREATE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireEdit(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.CUSTOMER_EDIT)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireDelete(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.CUSTOMER_DELETE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }
}
