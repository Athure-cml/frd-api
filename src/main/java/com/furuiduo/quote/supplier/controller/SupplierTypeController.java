package com.furuiduo.quote.supplier.controller;

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
import com.furuiduo.quote.supplier.dto.SupplierTypeResponse;
import com.furuiduo.quote.supplier.dto.SupplierTypeSaveRequest;
import com.furuiduo.quote.supplier.service.SupplierTypeService;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.SupplierPermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "其他供应商类型", description = "其他供应商类型字典")
@RestController
@RequestMapping("/supplier-types")
public class SupplierTypeController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final SupplierTypeService supplierTypeService;

  public SupplierTypeController(
      AuthService authService,
      PermissionService permissionService,
      SupplierTypeService supplierTypeService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.supplierTypeService = supplierTypeService;
  }

  @Operation(
      summary = "类型列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<List<SupplierTypeResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(defaultValue = "false") boolean enabledOnly) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(supplierTypeService.list(enabledOnly));
  }

  @Operation(
      summary = "新建类型",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<SupplierTypeResponse> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody SupplierTypeSaveRequest request) {
    requireEdit(authService.requireUser(authorization));
    return ApiResponse.ok(supplierTypeService.create(request));
  }

  @Operation(
      summary = "更新类型",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<SupplierTypeResponse> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody SupplierTypeSaveRequest request) {
    requireEdit(authService.requireUser(authorization));
    return ApiResponse.ok(supplierTypeService.update(id, request));
  }

  @Operation(
      summary = "删除类型",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireEdit(authService.requireUser(authorization));
    supplierTypeService.delete(id);
    return ApiResponse.ok(null);
  }

  private void requireView(SysUser user) {
    if (permissionService.hasAnyPermission(user, SupplierPermissionCodes.allViews())) {
      return;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
  }

  private void requireEdit(SysUser user) {
    if (permissionService.hasAnyPermission(
        user, PermissionCodes.SUPPLIER_OTHER_CREATE, PermissionCodes.SUPPLIER_OTHER_EDIT)) {
      return;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
  }
}
