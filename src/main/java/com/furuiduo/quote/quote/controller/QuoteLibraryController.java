package com.furuiduo.quote.quote.controller;

import java.math.BigDecimal;
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
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.auth.AuthService;
import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.common.RequestIds;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.cost.dto.FreightCostResponse;
import com.furuiduo.quote.cost.dto.FumigationCostResponse;
import com.furuiduo.quote.cost.dto.RoadCostResponse;
import com.furuiduo.quote.cost.entity.CostHighlightMode;
import com.furuiduo.quote.quote.dto.QuoteLibraryBatchDeleteRequest;
import com.furuiduo.quote.quote.dto.QuoteLibraryBatchUpdateRequest;
import com.furuiduo.quote.quote.dto.QuoteLibraryBatchUpdateResult;
import com.furuiduo.quote.quote.dto.QuoteLibraryEditContext;
import com.furuiduo.quote.quote.dto.QuoteLibraryPromoteRequest;
import com.furuiduo.quote.quote.dto.QuoteLibraryPromoteResult;
import com.furuiduo.quote.quote.dto.QuoteLibraryUpdateRequest;
import com.furuiduo.quote.quote.service.QuoteLibraryExportService;
import com.furuiduo.quote.quote.service.QuoteLibraryMutationService;
import com.furuiduo.quote.quote.service.QuoteLibraryService;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "报价库", description = "成本库数据 + 报价规则处理后展示")
@RestController
@RequestMapping("/quote-library")
public class QuoteLibraryController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final QuoteLibraryService quoteLibraryService;
  private final QuoteLibraryMutationService quoteLibraryMutationService;
  private final QuoteLibraryExportService quoteLibraryExportService;

  public QuoteLibraryController(
      AuthService authService,
      PermissionService permissionService,
      QuoteLibraryService quoteLibraryService,
      QuoteLibraryMutationService quoteLibraryMutationService,
      QuoteLibraryExportService quoteLibraryExportService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.quoteLibraryService = quoteLibraryService;
    this.quoteLibraryMutationService = quoteLibraryMutationService;
    this.quoteLibraryExportService = quoteLibraryExportService;
  }

  @Operation(
      summary = "卡车报价库分页",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/road")
  public ApiResponse<PageResult<RoadCostResponse>> listRoad(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String zipCode,
      @RequestParam(required = false) String city,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String por,
      @RequestParam(required = false) String pol,
      @RequestParam(required = false) String supplier,
      @RequestParam(required = false) BigDecimal redelivery,
      @RequestParam(required = false) String effectiveDate,
      @RequestParam(required = false) String validDate,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String sortField,
      @RequestParam(required = false) String sortOrder,
      @RequestParam(required = false) Boolean highlightOnly) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryView(
        user, PermissionCodes.QUOTE_LIBRARY_ROAD_VIEW, PermissionCodes.COST_ROAD_VIEW);
    return ApiResponse.ok(
        quoteLibraryService.listRoad(
            user,
            page,
            pageSize,
            zipCode,
            city,
            state,
            por,
            pol,
            supplier,
            redelivery,
            effectiveDate,
            validDate,
            status,
            sortField,
            sortOrder,
            highlightOnly));
  }

  @Operation(
      summary = "海运报价库分页",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/sea")
  public ApiResponse<PageResult<FreightCostResponse>> listSea(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String por,
      @RequestParam(required = false) String pol,
      @RequestParam(required = false) String origin,
      @RequestParam(required = false) String pod,
      @RequestParam(required = false) String destination,
      @RequestParam(required = false) String ssl,
      @RequestParam(required = false) String carrier,
      @RequestParam(required = false) String containerType,
      @RequestParam(required = false) String agent,
      @RequestParam(required = false) String freightValidDate,
      @RequestParam(required = false) String freightEffDate,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String remark,
      @RequestParam(required = false) String sortField,
      @RequestParam(required = false) String sortOrder,
      @RequestParam(required = false) Boolean highlightOnly) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryView(
        user, PermissionCodes.QUOTE_LIBRARY_SEA_VIEW, PermissionCodes.COST_SEA_VIEW);
    String polFilter = firstNonBlank(pol, origin);
    String podFilter = firstNonBlank(pod, destination);
    String sslFilter = firstNonBlank(ssl, carrier);
    return ApiResponse.ok(
        quoteLibraryService.listSea(
            user,
            page,
            pageSize,
            por,
            polFilter,
            podFilter,
            sslFilter,
            containerType,
            agent,
            freightValidDate,
            freightEffDate,
            status,
            remark,
            sortField,
            sortOrder,
            highlightOnly));
  }

  @Operation(
      summary = "熏蒸报价库分页",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/fumigation")
  public ApiResponse<PageResult<FumigationCostResponse>> listFumigation(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String region,
      @RequestParam(required = false) String port,
      @RequestParam(required = false) String station,
      @RequestParam(required = false) String outdoorValidity,
      @RequestParam(required = false) String indoorValidity,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String sortField,
      @RequestParam(required = false) String sortOrder,
      @RequestParam(required = false) Boolean highlightOnly) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryView(
        user,
        PermissionCodes.QUOTE_LIBRARY_FUMIGATION_VIEW,
        PermissionCodes.COST_FUMIGATION_VIEW);
    String regionFilter = firstNonBlank(region, port);
    return ApiResponse.ok(
        quoteLibraryService.listFumigation(
            user,
            page,
            pageSize,
            regionFilter,
            station,
            outdoorValidity,
            indoorValidity,
            status,
            sortField,
            sortOrder,
            highlightOnly));
  }

  @Operation(
      summary = "卡车成本入报价库",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/road/promote")
  public ApiResponse<QuoteLibraryPromoteResult> promoteRoad(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody QuoteLibraryPromoteRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryEdit(
        user,
        PermissionCodes.QUOTE_LIBRARY_ROAD_VIEW,
        PermissionCodes.QUOTE_LIBRARY_ROAD_EDIT,
        PermissionCodes.COST_ROAD_VIEW,
        PermissionCodes.COST_ROAD_EDIT);
    return ApiResponse.ok(quoteLibraryMutationService.promoteRoad(request, user));
  }

  @Operation(
      summary = "海运成本入报价库",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/sea/promote")
  public ApiResponse<QuoteLibraryPromoteResult> promoteSea(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody QuoteLibraryPromoteRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryEdit(
        user,
        PermissionCodes.QUOTE_LIBRARY_SEA_VIEW,
        PermissionCodes.QUOTE_LIBRARY_SEA_EDIT,
        PermissionCodes.COST_SEA_VIEW,
        PermissionCodes.COST_SEA_EDIT);
    return ApiResponse.ok(quoteLibraryMutationService.promoteSea(request, user));
  }

  @Operation(
      summary = "熏蒸成本入报价库",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/fumigation/promote")
  public ApiResponse<QuoteLibraryPromoteResult> promoteFumigation(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody QuoteLibraryPromoteRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryEdit(
        user,
        PermissionCodes.QUOTE_LIBRARY_FUMIGATION_VIEW,
        PermissionCodes.QUOTE_LIBRARY_FUMIGATION_EDIT,
        PermissionCodes.COST_FUMIGATION_VIEW,
        PermissionCodes.COST_FUMIGATION_EDIT);
    return ApiResponse.ok(quoteLibraryMutationService.promoteFumigation(request, user));
  }

  @Operation(
      summary = "卡车报价库编辑上下文",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/road/{id}/edit-context")
  public ApiResponse<QuoteLibraryEditContext> roadEditContext(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryView(
        user, PermissionCodes.QUOTE_LIBRARY_ROAD_VIEW, PermissionCodes.COST_ROAD_VIEW);
    requireQuoteLibraryEdit(
        user,
        PermissionCodes.QUOTE_LIBRARY_ROAD_VIEW,
        PermissionCodes.QUOTE_LIBRARY_ROAD_EDIT,
        PermissionCodes.COST_ROAD_VIEW,
        PermissionCodes.COST_ROAD_EDIT);
    return ApiResponse.ok(
        quoteLibraryMutationService.getEditContext(CostHighlightMode.road, id));
  }

  @Operation(
      summary = "海运报价库编辑上下文",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/sea/{id}/edit-context")
  public ApiResponse<QuoteLibraryEditContext> seaEditContext(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryView(
        user, PermissionCodes.QUOTE_LIBRARY_SEA_VIEW, PermissionCodes.COST_SEA_VIEW);
    requireQuoteLibraryEdit(
        user,
        PermissionCodes.QUOTE_LIBRARY_SEA_VIEW,
        PermissionCodes.QUOTE_LIBRARY_SEA_EDIT,
        PermissionCodes.COST_SEA_VIEW,
        PermissionCodes.COST_SEA_EDIT);
    return ApiResponse.ok(
        quoteLibraryMutationService.getEditContext(CostHighlightMode.sea, id));
  }

  @Operation(
      summary = "熏蒸报价库编辑上下文",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/fumigation/{id}/edit-context")
  public ApiResponse<QuoteLibraryEditContext> fumigationEditContext(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryView(
        user,
        PermissionCodes.QUOTE_LIBRARY_FUMIGATION_VIEW,
        PermissionCodes.COST_FUMIGATION_VIEW);
    requireQuoteLibraryEdit(
        user,
        PermissionCodes.QUOTE_LIBRARY_FUMIGATION_VIEW,
        PermissionCodes.QUOTE_LIBRARY_FUMIGATION_EDIT,
        PermissionCodes.COST_FUMIGATION_VIEW,
        PermissionCodes.COST_FUMIGATION_EDIT);
    return ApiResponse.ok(
        quoteLibraryMutationService.getEditContext(CostHighlightMode.fumigation, id));
  }

  @Operation(
      summary = "重置卡车报价库费用",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/road/{id}/reset")
  public ApiResponse<QuoteLibraryEditContext> resetRoad(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryEdit(
        user,
        PermissionCodes.QUOTE_LIBRARY_ROAD_VIEW,
        PermissionCodes.QUOTE_LIBRARY_ROAD_EDIT,
        PermissionCodes.COST_ROAD_VIEW,
        PermissionCodes.COST_ROAD_EDIT);
    return ApiResponse.ok(quoteLibraryMutationService.resetRoad(id));
  }

  @Operation(
      summary = "重置海运报价库费用",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/sea/{id}/reset")
  public ApiResponse<QuoteLibraryEditContext> resetSea(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryEdit(
        user,
        PermissionCodes.QUOTE_LIBRARY_SEA_VIEW,
        PermissionCodes.QUOTE_LIBRARY_SEA_EDIT,
        PermissionCodes.COST_SEA_VIEW,
        PermissionCodes.COST_SEA_EDIT);
    return ApiResponse.ok(quoteLibraryMutationService.resetSea(id));
  }

  @Operation(
      summary = "重置熏蒸报价库费用",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/fumigation/{id}/reset")
  public ApiResponse<QuoteLibraryEditContext> resetFumigation(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryEdit(
        user,
        PermissionCodes.QUOTE_LIBRARY_FUMIGATION_VIEW,
        PermissionCodes.QUOTE_LIBRARY_FUMIGATION_EDIT,
        PermissionCodes.COST_FUMIGATION_VIEW,
        PermissionCodes.COST_FUMIGATION_EDIT);
    return ApiResponse.ok(quoteLibraryMutationService.resetFumigation(id));
  }

  @Operation(
      summary = "更新卡车报价库费用",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/road/{id}")
  public ApiResponse<RoadCostResponse> updateRoad(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody QuoteLibraryUpdateRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryEdit(
        user,
        PermissionCodes.QUOTE_LIBRARY_ROAD_VIEW,
        PermissionCodes.QUOTE_LIBRARY_ROAD_EDIT,
        PermissionCodes.COST_ROAD_VIEW,
        PermissionCodes.COST_ROAD_EDIT);
    return ApiResponse.ok(quoteLibraryMutationService.updateRoad(id, request, user));
  }

  @Operation(
      summary = "更新海运报价库费用",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/sea/{id}")
  public ApiResponse<FreightCostResponse> updateSea(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody QuoteLibraryUpdateRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryEdit(
        user,
        PermissionCodes.QUOTE_LIBRARY_SEA_VIEW,
        PermissionCodes.QUOTE_LIBRARY_SEA_EDIT,
        PermissionCodes.COST_SEA_VIEW,
        PermissionCodes.COST_SEA_EDIT);
    return ApiResponse.ok(quoteLibraryMutationService.updateSea(id, request, user));
  }

  @Operation(
      summary = "更新熏蒸报价库费用",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/fumigation/{id}")
  public ApiResponse<FumigationCostResponse> updateFumigation(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody QuoteLibraryUpdateRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryEdit(
        user,
        PermissionCodes.QUOTE_LIBRARY_FUMIGATION_VIEW,
        PermissionCodes.QUOTE_LIBRARY_FUMIGATION_EDIT,
        PermissionCodes.COST_FUMIGATION_VIEW,
        PermissionCodes.COST_FUMIGATION_EDIT);
    return ApiResponse.ok(quoteLibraryMutationService.updateFumigation(id, request, user));
  }

  @Operation(
      summary = "批量更新卡车报价库费用",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/road/batch-update")
  public ApiResponse<QuoteLibraryBatchUpdateResult> batchUpdateRoad(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody QuoteLibraryBatchUpdateRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryEdit(
        user,
        PermissionCodes.QUOTE_LIBRARY_ROAD_VIEW,
        PermissionCodes.QUOTE_LIBRARY_ROAD_EDIT,
        PermissionCodes.COST_ROAD_VIEW,
        PermissionCodes.COST_ROAD_EDIT);
    return ApiResponse.ok(quoteLibraryMutationService.batchUpdateRoad(request, user));
  }

  @Operation(
      summary = "批量更新海运报价库费用",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/sea/batch-update")
  public ApiResponse<QuoteLibraryBatchUpdateResult> batchUpdateSea(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody QuoteLibraryBatchUpdateRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryEdit(
        user,
        PermissionCodes.QUOTE_LIBRARY_SEA_VIEW,
        PermissionCodes.QUOTE_LIBRARY_SEA_EDIT,
        PermissionCodes.COST_SEA_VIEW,
        PermissionCodes.COST_SEA_EDIT);
    return ApiResponse.ok(quoteLibraryMutationService.batchUpdateSea(request, user));
  }

  @Operation(
      summary = "批量更新熏蒸报价库费用",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/fumigation/batch-update")
  public ApiResponse<QuoteLibraryBatchUpdateResult> batchUpdateFumigation(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody QuoteLibraryBatchUpdateRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryEdit(
        user,
        PermissionCodes.QUOTE_LIBRARY_FUMIGATION_VIEW,
        PermissionCodes.QUOTE_LIBRARY_FUMIGATION_EDIT,
        PermissionCodes.COST_FUMIGATION_VIEW,
        PermissionCodes.COST_FUMIGATION_EDIT);
    return ApiResponse.ok(quoteLibraryMutationService.batchUpdateFumigation(request, user));
  }

  @Operation(
      summary = "删除卡车报价库记录",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/road/{id}")
  public ApiResponse<Void> deleteRoad(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryDelete(
        user,
        PermissionCodes.QUOTE_LIBRARY_ROAD_VIEW,
        PermissionCodes.QUOTE_LIBRARY_ROAD_DELETE,
        PermissionCodes.COST_ROAD_VIEW,
        PermissionCodes.COST_ROAD_EDIT);
    quoteLibraryMutationService.deleteRoad(id);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "删除海运报价库记录",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/sea/{id}")
  public ApiResponse<Void> deleteSea(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryDelete(
        user,
        PermissionCodes.QUOTE_LIBRARY_SEA_VIEW,
        PermissionCodes.QUOTE_LIBRARY_SEA_DELETE,
        PermissionCodes.COST_SEA_VIEW,
        PermissionCodes.COST_SEA_EDIT);
    quoteLibraryMutationService.deleteSea(id);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "删除熏蒸报价库记录",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/fumigation/{id}")
  public ApiResponse<Void> deleteFumigation(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryDelete(
        user,
        PermissionCodes.QUOTE_LIBRARY_FUMIGATION_VIEW,
        PermissionCodes.QUOTE_LIBRARY_FUMIGATION_DELETE,
        PermissionCodes.COST_FUMIGATION_VIEW,
        PermissionCodes.COST_FUMIGATION_EDIT);
    quoteLibraryMutationService.deleteFumigation(id);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "批量删除卡车报价库记录",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/road/batch-delete")
  public ApiResponse<Void> batchDeleteRoad(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody QuoteLibraryBatchDeleteRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryDelete(
        user,
        PermissionCodes.QUOTE_LIBRARY_ROAD_VIEW,
        PermissionCodes.QUOTE_LIBRARY_ROAD_DELETE,
        PermissionCodes.COST_ROAD_VIEW,
        PermissionCodes.COST_ROAD_EDIT);
    quoteLibraryMutationService.batchDeleteRoad(request);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "批量删除海运报价库记录",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/sea/batch-delete")
  public ApiResponse<Void> batchDeleteSea(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody QuoteLibraryBatchDeleteRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryDelete(
        user,
        PermissionCodes.QUOTE_LIBRARY_SEA_VIEW,
        PermissionCodes.QUOTE_LIBRARY_SEA_DELETE,
        PermissionCodes.COST_SEA_VIEW,
        PermissionCodes.COST_SEA_EDIT);
    quoteLibraryMutationService.batchDeleteSea(request);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "批量删除熏蒸报价库记录",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/fumigation/batch-delete")
  public ApiResponse<Void> batchDeleteFumigation(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody QuoteLibraryBatchDeleteRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryDelete(
        user,
        PermissionCodes.QUOTE_LIBRARY_FUMIGATION_VIEW,
        PermissionCodes.QUOTE_LIBRARY_FUMIGATION_DELETE,
        PermissionCodes.COST_FUMIGATION_VIEW,
        PermissionCodes.COST_FUMIGATION_EDIT);
    quoteLibraryMutationService.batchDeleteFumigation(request);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "导出卡车报价库 Excel",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/road/export")
  public ResponseEntity<byte[]> exportRoad(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String zipCode,
      @RequestParam(required = false) String city,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String por,
      @RequestParam(required = false) String pol,
      @RequestParam(required = false) String supplier,
      @RequestParam(required = false) BigDecimal redelivery,
      @RequestParam(required = false) String effectiveDate,
      @RequestParam(required = false) String validDate,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Boolean highlightOnly,
      @RequestParam(required = false) String ids,
      @RequestParam(required = false) String columns) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryView(
        user, PermissionCodes.QUOTE_LIBRARY_ROAD_VIEW, PermissionCodes.COST_ROAD_VIEW);
    byte[] bytes =
        quoteLibraryExportService.exportRoad(
            user,
            zipCode,
            city,
            state,
            por,
            pol,
            supplier,
            redelivery,
            effectiveDate,
            validDate,
            status,
            highlightOnly,
            RequestIds.parse(ids),
            columns);
    return excelAttachment("卡车报价库.xlsx", bytes);
  }

  @Operation(
      summary = "导出海运报价库 Excel",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/sea/export")
  public ResponseEntity<byte[]> exportSea(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String por,
      @RequestParam(required = false) String pol,
      @RequestParam(required = false) String origin,
      @RequestParam(required = false) String pod,
      @RequestParam(required = false) String destination,
      @RequestParam(required = false) String ssl,
      @RequestParam(required = false) String carrier,
      @RequestParam(required = false) String containerType,
      @RequestParam(required = false) String agent,
      @RequestParam(required = false) String freightValidDate,
      @RequestParam(required = false) String freightEffDate,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String remark,
      @RequestParam(required = false) Boolean highlightOnly,
      @RequestParam(required = false) String ids,
      @RequestParam(required = false) String columns) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryView(
        user, PermissionCodes.QUOTE_LIBRARY_SEA_VIEW, PermissionCodes.COST_SEA_VIEW);
    String polFilter = firstNonBlank(pol, origin);
    String podFilter = firstNonBlank(pod, destination);
    String sslFilter = firstNonBlank(ssl, carrier);
    byte[] bytes =
        quoteLibraryExportService.exportSea(
            user,
            por,
            polFilter,
            podFilter,
            sslFilter,
            containerType,
            agent,
            freightValidDate,
            freightEffDate,
            status,
            remark,
            highlightOnly,
            RequestIds.parse(ids),
            columns);
    return excelAttachment("海运报价库.xlsx", bytes);
  }

  @Operation(
      summary = "导出熏蒸报价库 Excel",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/fumigation/export")
  public ResponseEntity<byte[]> exportFumigation(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String region,
      @RequestParam(required = false) String port,
      @RequestParam(required = false) String station,
      @RequestParam(required = false) String outdoorValidity,
      @RequestParam(required = false) String indoorValidity,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Boolean highlightOnly,
      @RequestParam(required = false) String ids,
      @RequestParam(required = false) String columns) {
    SysUser user = authService.requireUser(authorization);
    requireQuoteLibraryView(
        user,
        PermissionCodes.QUOTE_LIBRARY_FUMIGATION_VIEW,
        PermissionCodes.COST_FUMIGATION_VIEW);
    String regionFilter = firstNonBlank(region, port);
    byte[] bytes =
        quoteLibraryExportService.exportFumigation(
            user,
            regionFilter,
            station,
            outdoorValidity,
            indoorValidity,
            status,
            highlightOnly,
            RequestIds.parse(ids),
            columns);
    return excelAttachment("熏蒸报价库.xlsx", bytes);
  }

  private ResponseEntity<byte[]> excelAttachment(String filename, byte[] bytes) {
    String encoded =
        URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }

  private void requireQuoteLibraryView(
      SysUser user, String libraryViewPermission, String costViewPermission) {
    if (!permissionService.hasPermission(user, libraryViewPermission)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "缺少报价库查看权限");
    }
    if (!permissionService.hasPermission(user, costViewPermission)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "缺少成本库查看权限");
    }
  }

  private void requireQuoteLibraryEdit(
      SysUser user,
      String libraryViewPermission,
      String libraryEditPermission,
      String costViewPermission,
      String costEditPermission) {
    requireQuoteLibraryView(user, libraryViewPermission, costViewPermission);
    if (!permissionService.hasPermission(user, libraryEditPermission)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "缺少报价库编辑权限");
    }
    if (!permissionService.hasPermission(user, costEditPermission)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "缺少成本库编辑权限");
    }
  }

  private void requireQuoteLibraryDelete(
      SysUser user,
      String libraryViewPermission,
      String libraryDeletePermission,
      String costViewPermission,
      String costEditPermission) {
    requireQuoteLibraryView(user, libraryViewPermission, costViewPermission);
    if (!permissionService.hasPermission(user, libraryDeletePermission)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "缺少报价库删除权限");
    }
    if (!permissionService.hasPermission(user, costEditPermission)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "缺少成本库编辑权限");
    }
  }

  private static String firstNonBlank(String primary, String fallback) {
    if (primary != null && !primary.isBlank()) {
      return primary;
    }
    return fallback;
  }
}
