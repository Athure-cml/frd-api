package com.furuiduo.quote.approval.controller;

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

import com.furuiduo.quote.approval.dto.ApprovalConfigResponse;
import com.furuiduo.quote.approval.dto.ApprovalConfigSaveRequest;
import com.furuiduo.quote.approval.service.ApprovalConfigService;
import com.furuiduo.quote.auth.AuthService;
import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "审批配置", description = "审批流程配置")
@RestController
@RequestMapping("/approvals/configs")
public class ApprovalConfigController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final ApprovalConfigService approvalConfigService;

  public ApprovalConfigController(
      AuthService authService,
      PermissionService permissionService,
      ApprovalConfigService approvalConfigService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.approvalConfigService = approvalConfigService;
  }

  @Operation(
      summary = "审批配置分页列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<PageResult<ApprovalConfigResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String configNo,
      @RequestParam(required = false) String configObject) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(approvalConfigService.list(page, pageSize, configNo, configObject));
  }

  @Operation(
      summary = "新建审批配置",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<ApprovalConfigResponse> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody ApprovalConfigSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(approvalConfigService.create(request));
  }

  @Operation(
      summary = "更新审批配置",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<ApprovalConfigResponse> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody ApprovalConfigSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(approvalConfigService.update(id, request));
  }

  @Operation(
      summary = "删除审批配置",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireManage(authService.requireUser(authorization));
    approvalConfigService.delete(id);
    return ApiResponse.ok(null);
  }

  private void requireView(SysUser user) {
    if (permissionService.hasPermission(user, PermissionCodes.APPROVAL_CONFIG_VIEW)
        || permissionService.hasPermission(user, PermissionCodes.APPROVAL_CONFIG_MANAGE)) {
      return;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无审批配置权限");
  }

  private void requireManage(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.APPROVAL_CONFIG_MANAGE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无审批配置管理权限");
    }
  }
}
