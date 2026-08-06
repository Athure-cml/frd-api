package com.furuiduo.quote.shippingline.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.auth.AuthService;
import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.common.RequestIds;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.cost.dto.CostImportResult;
import com.furuiduo.quote.shippingline.dto.ShippingLineResponse;
import com.furuiduo.quote.shippingline.dto.ShippingLineSaveRequest;
import com.furuiduo.quote.shippingline.service.ShippingLineCommandService;
import com.furuiduo.quote.shippingline.service.ShippingLineQueryService;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "船公司", description = "船公司主数据")
@RestController
@RequestMapping("/shipping-lines")
public class ShippingLineController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final ShippingLineQueryService queryService;
  private final ShippingLineCommandService commandService;

  public ShippingLineController(
      AuthService authService,
      PermissionService permissionService,
      ShippingLineQueryService queryService,
      ShippingLineCommandService commandService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.queryService = queryService;
    this.commandService = commandService;
  }

  @Operation(
      summary = "船公司分页列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<PageResult<ShippingLineResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Integer status) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(queryService.list(page, pageSize, code, name, status));
  }

  @Operation(
      summary = "船公司详情",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}")
  public ApiResponse<ShippingLineResponse> get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(commandService.getById(id));
  }

  @Operation(
      summary = "新建船公司",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<ShippingLineResponse> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody ShippingLineSaveRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireCreate(user);
    return ApiResponse.ok(commandService.create(user, request));
  }

  @Operation(
      summary = "更新船公司",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<ShippingLineResponse> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody ShippingLineSaveRequest request) {
    requireEdit(authService.requireUser(authorization));
    return ApiResponse.ok(commandService.update(id, request));
  }

  @Operation(
      summary = "删除船公司",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireDelete(authService.requireUser(authorization));
    commandService.delete(id);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "导入船公司",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<CostImportResult> importExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam("file") MultipartFile file)
      throws IOException {
    SysUser user = authService.requireUser(authorization);
    requireCreate(user);
    return ApiResponse.ok(commandService.importExcel(user, file));
  }

  @Operation(
      summary = "导出船公司",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/export")
  public ResponseEntity<byte[]> exportExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Integer status,
      @RequestParam(required = false) String ids) {
    requireView(authService.requireUser(authorization));
    byte[] bytes = commandService.exportExcel(code, name, status, RequestIds.parse(ids));
    String filename =
        URLEncoder.encode("船公司.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }

  private void requireView(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.SHIPPING_LINE_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireCreate(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.SHIPPING_LINE_CREATE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireEdit(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.SHIPPING_LINE_EDIT)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireDelete(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.SHIPPING_LINE_DELETE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }
}
