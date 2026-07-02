package com.furuiduo.quote.sys.controller;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import com.furuiduo.quote.sys.dto.OperationLogResponse;
import com.furuiduo.quote.sys.entity.OperationAction;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.OperationLogService;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "系统-操作日志", description = "用户数据变更审计")
@RestController
@RequestMapping("/sys/operation-logs")
public class SysOperationLogController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final OperationLogService operationLogService;

  public SysOperationLogController(
      AuthService authService,
      PermissionService permissionService,
      OperationLogService operationLogService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.operationLogService = operationLogService;
  }

  @Operation(
      summary = "操作日志分页列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<PageResult<OperationLogResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String module,
      @RequestParam(required = false) OperationAction action,
      @RequestParam(required = false) String username,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime startAt,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime endAt) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(
        operationLogService.list(page, pageSize, module, action, username, keyword, startAt, endAt));
  }

  @Operation(
      summary = "操作日志详情",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}")
  public ApiResponse<OperationLogResponse> get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(operationLogService.getById(id));
  }

  private void requireView(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.SYS_OPERATION_LOG_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }
}
