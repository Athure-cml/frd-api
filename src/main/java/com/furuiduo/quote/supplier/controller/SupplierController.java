package com.furuiduo.quote.supplier.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
import com.furuiduo.quote.common.BatchIdsRequest;
import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.common.ReorderRequest;
import com.furuiduo.quote.common.RequestIds;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.cost.dto.CostImportResult;
import com.furuiduo.quote.supplier.dto.SupplierResponse;
import com.furuiduo.quote.supplier.dto.SupplierSaveRequest;
import com.furuiduo.quote.supplier.service.SupplierCommandService;
import com.furuiduo.quote.supplier.service.SupplierQueryService;
import com.furuiduo.quote.supplier.support.SupplierCategories;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.SupplierPermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "供应商", description = "供应商主数据（按分类）")
@RestController
@RequestMapping("/suppliers")
public class SupplierController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final SupplierQueryService supplierQueryService;
  private final SupplierCommandService supplierCommandService;

  public SupplierController(
      AuthService authService,
      PermissionService permissionService,
      SupplierQueryService supplierQueryService,
      SupplierCommandService supplierCommandService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.supplierQueryService = supplierQueryService;
    this.supplierCommandService = supplierCommandService;
  }

  @Operation(
      summary = "供应商分页列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<PageResult<SupplierResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(defaultValue = "TRUCK") String category,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Integer status,
      @RequestParam(required = false) String typeId) {
    require(authService.requireUser(authorization), category, "view");
    return ApiResponse.ok(
        supplierQueryService.list(page, pageSize, category, code, name, status, typeId));
  }

  @Operation(
      summary = "供应商详情",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}")
  public ApiResponse<SupplierResponse> get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SupplierResponse row = supplierCommandService.getById(id);
    require(authService.requireUser(authorization), row.category(), "view");
    return ApiResponse.ok(row);
  }

  @Operation(
      summary = "新建供应商",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<SupplierResponse> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody SupplierSaveRequest request) {
    SysUser user = authService.requireUser(authorization);
    require(user, request.category(), "create");
    return ApiResponse.ok(supplierCommandService.create(user, request));
  }

  @Operation(
      summary = "更新供应商",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<SupplierResponse> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody SupplierSaveRequest request) {
    SupplierResponse existing = supplierCommandService.getById(id);
    require(authService.requireUser(authorization), existing.category(), "edit");
    return ApiResponse.ok(supplierCommandService.update(id, request));
  }

  @Operation(
      summary = "置顶供应商",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/pin")
  public ApiResponse<SupplierResponse> pin(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SupplierResponse existing = supplierCommandService.getById(id);
    require(authService.requireUser(authorization), existing.category(), "edit");
    return ApiResponse.ok(supplierCommandService.setPinned(id, true));
  }

  @Operation(
      summary = "取消置顶供应商",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/unpin")
  public ApiResponse<SupplierResponse> unpin(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SupplierResponse existing = supplierCommandService.getById(id);
    require(authService.requireUser(authorization), existing.category(), "edit");
    return ApiResponse.ok(supplierCommandService.setPinned(id, false));
  }

  @Operation(
      summary = "拖拽排序",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/reorder")
  public ApiResponse<Void> reorder(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody ReorderRequest request) {
    List<Long> ids = request.ids() == null ? List.of() : request.ids();
    if (!ids.isEmpty()) {
      SupplierResponse existing = supplierCommandService.getById(ids.get(0));
      require(authService.requireUser(authorization), existing.category(), "edit");
    }
    supplierCommandService.reorder(ids);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "删除供应商",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SupplierResponse existing = supplierCommandService.getById(id);
    require(authService.requireUser(authorization), existing.category(), "delete");
    supplierCommandService.delete(id);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "批量删除供应商",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/batch-delete")
  public ApiResponse<Void> batchDelete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody BatchIdsRequest request) {
    SysUser user = authService.requireUser(authorization);
    List<Long> ids = request.ids() == null ? List.of() : request.ids();
    for (Long id : ids) {
      if (id == null) {
        continue;
      }
      SupplierResponse existing = supplierCommandService.getById(id);
      require(user, existing.category(), "delete");
    }
    supplierCommandService.batchDelete(ids);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "导入供应商",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<CostImportResult> importExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam("file") MultipartFile file,
      @RequestParam(defaultValue = "TRUCK") String category)
      throws IOException {
    SysUser user = authService.requireUser(authorization);
    require(user, category, "create");
    return ApiResponse.ok(supplierCommandService.importExcel(user, file, category));
  }

  @Operation(
      summary = "导出供应商",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/export")
  public ResponseEntity<byte[]> exportExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(defaultValue = "TRUCK") String category,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Integer status,
      @RequestParam(required = false) String typeId,
      @RequestParam(required = false) String ids) {
    require(authService.requireUser(authorization), category, "view");
    byte[] bytes =
        supplierCommandService.exportExcel(
            category, code, name, status, typeId, RequestIds.parse(ids));
    String filename =
        URLEncoder.encode(SupplierCategories.exportFilename(category), StandardCharsets.UTF_8)
            .replace("+", "%20");
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }

  private void require(SysUser user, String category, String action) {
    String modern = SupplierPermissionCodes.of(category, action);
    if (permissionService.hasPermission(user, modern)) {
      return;
    }
    // 兼容未迁移的旧扁平权限
    String legacy = "supplier:" + action;
    if (permissionService.hasPermission(user, legacy)) {
      return;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
  }
}
