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
import com.furuiduo.quote.masterdata.dto.GlobalPortResponse;
import com.furuiduo.quote.masterdata.dto.GlobalPortSaveRequest;
import com.furuiduo.quote.masterdata.dto.GlobalPortSyncStatus;
import com.furuiduo.quote.masterdata.entity.PortType;
import com.furuiduo.quote.masterdata.service.GlobalPortService;
import com.furuiduo.quote.masterdata.service.GlobalPortSyncService;
import com.furuiduo.quote.masterdata.sync.GlobalPortSyncExecutor;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "全球港口", description = "全球港口主数据")
@RestController
@RequestMapping("/global-ports")
public class GlobalPortController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final GlobalPortService globalPortService;
  private final GlobalPortSyncService globalPortSyncService;
  private final GlobalPortSyncExecutor globalPortSyncExecutor;

  public GlobalPortController(
      AuthService authService,
      PermissionService permissionService,
      GlobalPortService globalPortService,
      GlobalPortSyncService globalPortSyncService,
      GlobalPortSyncExecutor globalPortSyncExecutor) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.globalPortService = globalPortService;
    this.globalPortSyncService = globalPortSyncService;
    this.globalPortSyncExecutor = globalPortSyncExecutor;
  }

  @Operation(
      summary = "全球港口列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<PageResult<GlobalPortResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String nameEn,
      @RequestParam(required = false) String nameZh,
      @RequestParam(required = false) String route,
      @RequestParam(required = false) String countryRegion,
      @RequestParam(required = false) PortType portType) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(
        globalPortService.list(
            page, pageSize, code, nameEn, nameZh, route, countryRegion, portType));
  }

  @Operation(
      summary = "全球港口详情",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}")
  public ApiResponse<GlobalPortResponse> get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(globalPortService.getById(id));
  }

  @Operation(
      summary = "新建全球港口",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<GlobalPortResponse> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody GlobalPortSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(globalPortService.create(request));
  }

  @Operation(
      summary = "更新全球港口",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<GlobalPortResponse> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody GlobalPortSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(globalPortService.update(id, request));
  }

  @Operation(
      summary = "删除全球港口",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireManage(authService.requireUser(authorization));
    globalPortService.delete(id);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "导入全球港口",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<CostImportResult> importExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam("file") MultipartFile file)
      throws IOException {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(globalPortService.importExcel(file));
  }

  @Operation(
      summary = "导出全球港口",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/export")
  public ResponseEntity<byte[]> exportExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String nameEn,
      @RequestParam(required = false) String nameZh,
      @RequestParam(required = false) String route,
      @RequestParam(required = false) String countryRegion,
      @RequestParam(required = false) PortType portType) {
    requireView(authService.requireUser(authorization));
    byte[] bytes =
        globalPortService.exportExcel(code, nameEn, nameZh, route, countryRegion, portType);
    String filename =
        URLEncoder.encode("global-port.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }

  @Operation(
      summary = "从 UNECE UN/LOCODE 增量同步（异步）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/sync")
  public ApiResponse<GlobalPortSyncStatus> sync(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    requireManage(authService.requireUser(authorization));
    GlobalPortSyncStatus status = globalPortSyncService.prepareAsyncSync();
    globalPortSyncExecutor.runIncrementalSync();
    return ApiResponse.ok(status);
  }

  @Operation(
      summary = "查询 UN/LOCODE 同步状态",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/sync/status")
  public ApiResponse<GlobalPortSyncStatus> syncStatus(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(globalPortSyncService.getStatus());
  }

  @Operation(
      summary = "重置 UN/LOCODE 同步状态",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/sync/reset")
  public ApiResponse<Void> resetSync(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    requireManage(authService.requireUser(authorization));
    globalPortSyncService.resetSyncState();
    return ApiResponse.ok(null);
  }

  private void requireView(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.MD_GLOBAL_PORT_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireManage(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.MD_GLOBAL_PORT_MANAGE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }
}
