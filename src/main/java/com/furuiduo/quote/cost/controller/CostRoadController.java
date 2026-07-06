package com.furuiduo.quote.cost.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import com.furuiduo.quote.cost.dto.CostBatchDeleteRequest;
import com.furuiduo.quote.cost.dto.CostBatchUpdateRequest;
import com.furuiduo.quote.cost.dto.CostImportResult;
import com.furuiduo.quote.cost.dto.RoadCostResponse;
import com.furuiduo.quote.cost.dto.RoadCostSaveRequest;
import com.furuiduo.quote.cost.service.CostRoadService;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "成本库-卡车", description = "卡车成本库")
@RestController
@RequestMapping("/cost-library/road")
public class CostRoadController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final CostRoadService costRoadService;

  public CostRoadController(
      AuthService authService,
      PermissionService permissionService,
      CostRoadService costRoadService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.costRoadService = costRoadService;
  }

  @Operation(
      summary = "卡车成本分页列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<PageResult<RoadCostResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String supplier,
      @RequestParam(required = false) String city,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String pol,
      @RequestParam(required = false) String zipCode) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(
        costRoadService.list(page, pageSize, supplier, city, state, pol, zipCode));
  }

  @Operation(
      summary = "卡车成本详情",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}")
  public ApiResponse<RoadCostResponse> get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(costRoadService.getById(id));
  }

  @Operation(
      summary = "新建卡车成本",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<RoadCostResponse> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody RoadCostSaveRequest request) {
    requireEdit(authService.requireUser(authorization));
    return ApiResponse.ok(costRoadService.create(request));
  }

  @Operation(
      summary = "更新卡车成本",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<RoadCostResponse> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody RoadCostSaveRequest request) {
    requireEdit(authService.requireUser(authorization));
    return ApiResponse.ok(costRoadService.update(id, request));
  }

  @Operation(
      summary = "删除卡车成本",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireEdit(authService.requireUser(authorization));
    costRoadService.delete(id);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "批量删除卡车成本",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/batch-delete")
  public ApiResponse<Void> batchDelete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody CostBatchDeleteRequest request) {
    requireEdit(authService.requireUser(authorization));
    costRoadService.batchDelete(request);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "批量更新卡车成本",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PatchMapping("/batch")
  public ApiResponse<Map<String, Integer>> batchUpdate(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody CostBatchUpdateRequest request) {
    requireEdit(authService.requireUser(authorization));
    int updated = costRoadService.batchUpdate(request);
    return ApiResponse.ok(Map.of("updated", updated));
  }

  @Operation(
      summary = "导入卡车成本 Excel",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<CostImportResult> importExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam("file") MultipartFile file)
      throws IOException {
    requireEdit(authService.requireUser(authorization));
    return ApiResponse.ok(costRoadService.importExcel(file));
  }

  @Operation(
      summary = "导出卡车成本 Excel",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/export")
  public ResponseEntity<byte[]> exportExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String supplier,
      @RequestParam(required = false) String city,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String pol,
      @RequestParam(required = false) String zipCode,
      @RequestParam(required = false) Long templateId) {
    requireView(authService.requireUser(authorization));
    byte[] bytes =
        costRoadService.exportExcel(supplier, city, state, pol, zipCode, templateId);
    String filename =
        URLEncoder.encode("road-cost.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }

  private void requireView(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.COST_ROAD_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireEdit(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.COST_ROAD_EDIT)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }
}
