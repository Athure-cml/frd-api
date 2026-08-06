package com.furuiduo.quote.masterdata.controller;

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
import com.furuiduo.quote.masterdata.dto.ContainerTypeResponse;
import com.furuiduo.quote.masterdata.dto.ContainerTypeSaveRequest;
import com.furuiduo.quote.masterdata.service.ContainerTypeService;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "箱型", description = "集装箱箱型主数据")
@RestController
@RequestMapping("/container-types")
public class ContainerTypeController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final ContainerTypeService containerTypeService;

  public ContainerTypeController(
      AuthService authService,
      PermissionService permissionService,
      ContainerTypeService containerTypeService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.containerTypeService = containerTypeService;
  }

  @Operation(
      summary = "箱型列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<List<ContainerTypeResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String name) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(containerTypeService.list(code, name));
  }

  @Operation(
      summary = "启用中的箱型（下拉用）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/enabled")
  public ApiResponse<List<ContainerTypeResponse>> listEnabled(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    requireSelect(authService.requireUser(authorization));
    return ApiResponse.ok(containerTypeService.listEnabled());
  }

  @Operation(
      summary = "箱型详情",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}")
  public ApiResponse<ContainerTypeResponse> get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(containerTypeService.getById(id));
  }

  @Operation(
      summary = "新建箱型",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<ContainerTypeResponse> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody ContainerTypeSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(containerTypeService.create(request));
  }

  @Operation(
      summary = "更新箱型",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<ContainerTypeResponse> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody ContainerTypeSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(containerTypeService.update(id, request));
  }

  @Operation(
      summary = "删除箱型",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireManage(authService.requireUser(authorization));
    containerTypeService.delete(id);
    return ApiResponse.ok(null);
  }

  private void requireView(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.MD_CONTAINER_TYPE_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  /** 主数据查看，或海运成本查看（录入下拉） */
  private void requireSelect(SysUser user) {
    if (permissionService.hasPermission(user, PermissionCodes.MD_CONTAINER_TYPE_VIEW)
        || permissionService.hasPermission(user, PermissionCodes.COST_SEA_VIEW)) {
      return;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
  }

  private void requireManage(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.MD_CONTAINER_TYPE_MANAGE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }
}
