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
import com.furuiduo.quote.sys.dto.DepartmentResponse;
import com.furuiduo.quote.sys.dto.DepartmentSaveRequest;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.repository.SysDepartmentRepository;
import com.furuiduo.quote.sys.service.PermissionService;
import com.furuiduo.quote.sys.service.SysDepartmentCommandService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "系统-部门", description = "部门管理")
@RestController
@RequestMapping("/sys/departments")
public class SysDepartmentController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final SysDepartmentRepository departmentRepository;
  private final SysDepartmentCommandService departmentCommandService;

  public SysDepartmentController(
      AuthService authService,
      PermissionService permissionService,
      SysDepartmentRepository departmentRepository,
      SysDepartmentCommandService departmentCommandService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.departmentRepository = departmentRepository;
    this.departmentCommandService = departmentCommandService;
  }

  @Operation(
      summary = "部门列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<List<DepartmentResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    SysUser user = authService.requireUser(authorization);
    if (!permissionService.hasPermission(user, PermissionCodes.SYS_DEPT_VIEW)
        && !permissionService.hasPermission(user, PermissionCodes.SYS_USER_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
    List<DepartmentResponse> list =
        departmentRepository.findAll().stream()
            .sorted(java.util.Comparator.comparingInt(d -> d.getSort()))
            .map(DepartmentResponse::from)
            .toList();
    return ApiResponse.ok(list);
  }

  @Operation(
      summary = "部门详情",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}")
  public ApiResponse<DepartmentResponse> get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(departmentCommandService.getById(id));
  }

  @Operation(
      summary = "新建部门",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<DepartmentResponse> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody DepartmentSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(departmentCommandService.create(request));
  }

  @Operation(
      summary = "更新部门",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<DepartmentResponse> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody DepartmentSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(departmentCommandService.update(id, request));
  }

  @Operation(
      summary = "删除部门",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireManage(authService.requireUser(authorization));
    departmentCommandService.delete(id);
    return ApiResponse.ok(null);
  }

  private void requireView(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.SYS_DEPT_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireManage(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.SYS_DEPT_MANAGE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }
}
