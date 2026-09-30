package com.furuiduo.quote.dashboard.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.auth.AuthService;
import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.dashboard.dto.NotificationItemDto;
import com.furuiduo.quote.dashboard.dto.WorkspaceResponse;
import com.furuiduo.quote.dashboard.dto.WorkspaceTodoDto;
import com.furuiduo.quote.dashboard.service.DashboardService;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "工作台", description = "工作台 KPI、待办与系统通知")
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final DashboardService dashboardService;

  public DashboardController(
      AuthService authService,
      PermissionService permissionService,
      DashboardService dashboardService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.dashboardService = dashboardService;
  }

  @Operation(
      summary = "工作台聚合数据",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/workspace")
  public ApiResponse<WorkspaceResponse> workspace(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    SysUser user = authService.requireUser(authorization);
    requireDashboardView(user);
    return ApiResponse.ok(dashboardService.getWorkspace(user));
  }

  @Operation(
      summary = "待办任务列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/todos")
  public ApiResponse<List<WorkspaceTodoDto>> todos(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    SysUser user = authService.requireUser(authorization);
    requireDashboardView(user);
    return ApiResponse.ok(dashboardService.listTodos(user));
  }

  @Operation(
      summary = "系统通知",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/notifications")
  public ApiResponse<List<NotificationItemDto>> notifications(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    SysUser user = authService.requireUser(authorization);
    requireDashboardView(user);
    return ApiResponse.ok(dashboardService.getNotifications(user));
  }

  @Operation(
      summary = "标记通知已读",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/notifications/{id}/read")
  public ApiResponse<Void> markRead(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable("id") String id) {
    SysUser user = authService.requireUser(authorization);
    requireDashboardView(user);
    dashboardService.markNotificationRead(user, id);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "全部标记已读",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/notifications/read-all")
  public ApiResponse<Void> markAllRead(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    SysUser user = authService.requireUser(authorization);
    requireDashboardView(user);
    dashboardService.markAllNotificationsRead(user);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "删除（忽略）单条通知",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/notifications/{id}")
  public ApiResponse<Void> dismiss(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable("id") String id) {
    SysUser user = authService.requireUser(authorization);
    requireDashboardView(user);
    dashboardService.dismissNotification(user, id);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "清空通知",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/notifications")
  public ApiResponse<Void> dismissAll(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    SysUser user = authService.requireUser(authorization);
    requireDashboardView(user);
    dashboardService.dismissAllNotifications(user);
    return ApiResponse.ok(null);
  }

  private void requireDashboardView(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.DASHBOARD_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无工作台查看权限");
    }
  }
}
