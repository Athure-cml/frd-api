package com.furuiduo.quote.unit.controller;

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
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;
import com.furuiduo.quote.unit.dto.UnitResponse;
import com.furuiduo.quote.unit.dto.UnitSaveRequest;
import com.furuiduo.quote.unit.service.UnitCommandService;
import com.furuiduo.quote.unit.service.UnitQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "单位", description = "费用单位主数据")
@RestController
@RequestMapping("/units")
public class UnitController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final UnitQueryService unitQueryService;
  private final UnitCommandService unitCommandService;

  public UnitController(
      AuthService authService,
      PermissionService permissionService,
      UnitQueryService unitQueryService,
      UnitCommandService unitCommandService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.unitQueryService = unitQueryService;
    this.unitCommandService = unitCommandService;
  }

  @Operation(
      summary = "单位列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<List<UnitResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Integer status) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(unitQueryService.list(code, name, status));
  }

  @Operation(
      summary = "单位详情",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}")
  public ApiResponse<UnitResponse> get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(unitCommandService.getById(id));
  }

  @Operation(
      summary = "新建单位",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<UnitResponse> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody UnitSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(unitCommandService.create(request));
  }

  @Operation(
      summary = "更新单位",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<UnitResponse> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody UnitSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(unitCommandService.update(id, request));
  }

  @Operation(
      summary = "删除单位",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireManage(authService.requireUser(authorization));
    unitCommandService.delete(id);
    return ApiResponse.ok(null);
  }

  private void requireView(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.UNIT_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireManage(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.UNIT_MANAGE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }
}
