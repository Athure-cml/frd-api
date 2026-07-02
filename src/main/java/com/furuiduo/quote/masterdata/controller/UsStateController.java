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
import com.furuiduo.quote.masterdata.dto.UsStateResponse;
import com.furuiduo.quote.masterdata.dto.UsStateSaveRequest;
import com.furuiduo.quote.masterdata.service.UsStateService;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "美国州", description = "美国州主数据")
@RestController
@RequestMapping("/us-states")
public class UsStateController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final UsStateService usStateService;

  public UsStateController(
      AuthService authService,
      PermissionService permissionService,
      UsStateService usStateService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.usStateService = usStateService;
  }

  @Operation(
      summary = "美国州列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<List<UsStateResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String nameZh) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(usStateService.list(code, nameZh));
  }

  @Operation(
      summary = "美国州详情",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}")
  public ApiResponse<UsStateResponse> get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(usStateService.getById(id));
  }

  @Operation(
      summary = "新建美国州",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<UsStateResponse> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody UsStateSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(usStateService.create(request));
  }

  @Operation(
      summary = "更新美国州",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<UsStateResponse> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody UsStateSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(usStateService.update(id, request));
  }

  @Operation(
      summary = "删除美国州",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireManage(authService.requireUser(authorization));
    usStateService.delete(id);
    return ApiResponse.ok(null);
  }

  private void requireView(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.MD_US_STATE_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireManage(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.MD_US_STATE_MANAGE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }
}
