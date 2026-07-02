package com.furuiduo.quote.sys.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.auth.AuthService;
import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.dto.PermissionResponse;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.repository.SysPermissionRepository;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "系统-权限", description = "权限码目录")
@RestController
@RequestMapping("/sys/permissions")
public class SysPermissionController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final SysPermissionRepository permissionRepository;

  public SysPermissionController(
      AuthService authService,
      PermissionService permissionService,
      SysPermissionRepository permissionRepository) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.permissionRepository = permissionRepository;
  }

  @Operation(
      summary = "权限码列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<List<PermissionResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    SysUser user = authService.requireUser(authorization);
    if (!permissionService.hasPermission(user, PermissionCodes.SYS_ROLE_VIEW)
        && !permissionService.hasPermission(user, PermissionCodes.SYS_ROLE_MANAGE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
    List<PermissionResponse> list =
        permissionRepository.findAll().stream()
            .sorted(java.util.Comparator.comparingInt(p -> p.getSort()))
            .map(PermissionResponse::from)
            .toList();
    return ApiResponse.ok(list);
  }
}
