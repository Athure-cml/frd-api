package com.furuiduo.quote.masterdata.controller;

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
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.cost.dto.CostImportResult;
import com.furuiduo.quote.masterdata.dto.InlandPorResponse;
import com.furuiduo.quote.masterdata.dto.InlandPorSaveRequest;
import com.furuiduo.quote.masterdata.service.InlandPorService;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "内陆 POR", description = "内陆 POR 主数据")
@RestController
@RequestMapping("/inland-pors")
public class InlandPorController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final InlandPorService inlandPorService;

  public InlandPorController(
      AuthService authService,
      PermissionService permissionService,
      InlandPorService inlandPorService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.inlandPorService = inlandPorService;
  }

  @Operation(
      summary = "内陆 POR 列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<PageResult<InlandPorResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String region,
      @RequestParam(required = false) Long polId) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(inlandPorService.list(page, pageSize, name, region, polId));
  }

  @Operation(
      summary = "内陆 POR 详情",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}")
  public ApiResponse<InlandPorResponse> get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(inlandPorService.getById(id));
  }

  @Operation(
      summary = "新建内陆 POR",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<InlandPorResponse> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody InlandPorSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(inlandPorService.create(request));
  }

  @Operation(
      summary = "更新内陆 POR",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<InlandPorResponse> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody InlandPorSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(inlandPorService.update(id, request));
  }

  @Operation(
      summary = "删除内陆 POR",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireManage(authService.requireUser(authorization));
    inlandPorService.delete(id);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "导入内陆 POR",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<CostImportResult> importExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam("file") MultipartFile file)
      throws IOException {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(inlandPorService.importExcel(file));
  }

  @Operation(
      summary = "导出内陆 POR",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/export")
  public ResponseEntity<byte[]> exportExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String region,
      @RequestParam(required = false) Long polId) {
    requireView(authService.requireUser(authorization));
    byte[] bytes = inlandPorService.exportExcel(name, region, polId);
    String filename =
        URLEncoder.encode("inland-por.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }

  private void requireView(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.MD_INLAND_POR_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireManage(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.MD_INLAND_POR_MANAGE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }
}
