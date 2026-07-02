package com.furuiduo.quote.sys.controller;

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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.auth.AuthService;
import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.dto.RoleListItem;
import com.furuiduo.quote.sys.dto.RoleSaveRequest;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.repository.SysRoleRepository;
import com.furuiduo.quote.sys.service.PermissionService;
import com.furuiduo.quote.sys.service.SysRoleCommandService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "系统-角色", description = "角色与权限管理")
@RestController
@RequestMapping("/sys/roles")
public class SysRoleController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final SysRoleRepository roleRepository;
  private final SysRoleCommandService roleCommandService;

  public SysRoleController(
      AuthService authService,
      PermissionService permissionService,
      SysRoleRepository roleRepository,
      SysRoleCommandService roleCommandService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.roleRepository = roleRepository;
    this.roleCommandService = roleCommandService;
  }

  @Operation(
      summary = "角色列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<List<RoleListItem>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    requireView(authService.requireUser(authorization));
    List<RoleListItem> list =
        roleRepository.findAllWithPermissions().stream().map(RoleListItem::from).toList();
    return ApiResponse.ok(list);
  }

  @Operation(
      summary = "角色详情",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}")
  public ApiResponse<RoleListItem> get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(roleCommandService.getById(id));
  }

  @Operation(
      summary = "新建角色",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<RoleListItem> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody RoleSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(roleCommandService.create(request));
  }

  @Operation(
      summary = "更新角色",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<RoleListItem> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody RoleSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(roleCommandService.update(id, request));
  }

  @Operation(
      summary = "删除角色",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireManage(authService.requireUser(authorization));
    roleCommandService.delete(id);
    return ApiResponse.ok(null);
  }

  private void requireView(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.SYS_ROLE_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireManage(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.SYS_ROLE_MANAGE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }
}
