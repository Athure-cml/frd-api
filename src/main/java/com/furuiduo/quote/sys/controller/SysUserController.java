package com.furuiduo.quote.sys.controller;

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
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.dto.UserCreateRequest;
import com.furuiduo.quote.sys.dto.UserListItem;
import com.furuiduo.quote.sys.dto.UserUpdateRequest;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;
import com.furuiduo.quote.sys.service.SysUserCommandService;
import com.furuiduo.quote.sys.service.SysUserQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "系统-用户", description = "用户管理")
@RestController
@RequestMapping("/sys/users")
public class SysUserController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final SysUserQueryService userQueryService;
  private final SysUserCommandService userCommandService;

  public SysUserController(
      AuthService authService,
      PermissionService permissionService,
      SysUserQueryService userQueryService,
      SysUserCommandService userCommandService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.userQueryService = userQueryService;
    this.userCommandService = userCommandService;
  }

  @Operation(
      summary = "用户分页列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<PageResult<UserListItem>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String username,
      @RequestParam(required = false) Long deptId,
      @RequestParam(required = false) Integer status) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(userQueryService.listUsers(page, pageSize, username, deptId, status));
  }

  @Operation(
      summary = "用户详情",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}")
  public ApiResponse<UserListItem> get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(userCommandService.getById(id));
  }

  @Operation(
      summary = "新建用户",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<UserListItem> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody UserCreateRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(userCommandService.create(request));
  }

  @Operation(
      summary = "更新用户",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<UserListItem> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody UserUpdateRequest request) {
    SysUser operator = authService.requireUser(authorization);
    requireManage(operator);
    return ApiResponse.ok(userCommandService.update(id, request, operator));
  }

  @Operation(
      summary = "删除用户",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser operator = authService.requireUser(authorization);
    requireManage(operator);
    userCommandService.delete(id, operator);
    return ApiResponse.ok(null);
  }

  private void requireView(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.SYS_USER_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireManage(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.SYS_USER_MANAGE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }
}
