package com.furuiduo.quote.quote.controller;

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
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.auth.AuthService;
import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.masterdata.dto.DestAddressRowResponse;
import com.furuiduo.quote.masterdata.service.DestAddressService;
import com.furuiduo.quote.quote.dto.QuoteBatchExportRequest;
import com.furuiduo.quote.quote.dto.QuoteDetailResponse;
import com.furuiduo.quote.quote.dto.QuoteFollowUpResponse;
import com.furuiduo.quote.quote.dto.QuoteFollowUpSaveRequest;
import com.furuiduo.quote.quote.dto.QuoteListItem;
import com.furuiduo.quote.quote.dto.QuoteMatchCostsRequest;
import com.furuiduo.quote.quote.dto.QuoteMatchCostsResponse;
import com.furuiduo.quote.quote.dto.QuoteSaveRequest;
import com.furuiduo.quote.quote.service.QuoteCommandService;
import com.furuiduo.quote.quote.service.QuoteCostMatchService;
import com.furuiduo.quote.quote.service.QuoteExportService;
import com.furuiduo.quote.quote.service.QuoteFollowUpService;
import com.furuiduo.quote.quote.service.QuoteQueryService;
import com.furuiduo.quote.quote.service.QuoteWorkflowService;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.dto.OperationLogResponse;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "报价单", description = "报价单管理")
@RestController
@RequestMapping("/quotes")
public class QuoteController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final QuoteQueryService quoteQueryService;
  private final QuoteCommandService quoteCommandService;
  private final QuoteCostMatchService quoteCostMatchService;
  private final QuoteWorkflowService quoteWorkflowService;
  private final QuoteFollowUpService quoteFollowUpService;
  private final QuoteExportService quoteExportService;
  private final DestAddressService destAddressService;

  public QuoteController(
      AuthService authService,
      PermissionService permissionService,
      QuoteQueryService quoteQueryService,
      QuoteCommandService quoteCommandService,
      QuoteCostMatchService quoteCostMatchService,
      QuoteWorkflowService quoteWorkflowService,
      QuoteFollowUpService quoteFollowUpService,
      QuoteExportService quoteExportService,
      DestAddressService destAddressService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.quoteQueryService = quoteQueryService;
    this.quoteCommandService = quoteCommandService;
    this.quoteCostMatchService = quoteCostMatchService;
    this.quoteWorkflowService = quoteWorkflowService;
    this.quoteFollowUpService = quoteFollowUpService;
    this.quoteExportService = quoteExportService;
    this.destAddressService = destAddressService;
  }

  @Operation(
      summary = "报价单分页列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<PageResult<QuoteListItem>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String quoteNo,
      @RequestParam(required = false) String customerName,
      @RequestParam(required = false) String transportMode,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String zipCode,
      @RequestParam(required = false) String city,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String por,
      @RequestParam(required = false) String pol,
      @RequestParam(required = false) String pod,
      @RequestParam(required = false) String ssl,
      @RequestParam(required = false) String followUpByName) {
    SysUser user = authService.requireUser(authorization);
    requireView(user);
    return ApiResponse.ok(
        quoteQueryService.list(
            user,
            page,
            pageSize,
            quoteNo,
            customerName,
            transportMode,
            status,
            zipCode,
            city,
            state,
            por,
            pol,
            pod,
            ssl,
            followUpByName));
  }

  @Operation(
      summary = "Zip code 模糊查询（自动填充 City / State）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/zip-lookup")
  public ApiResponse<List<DestAddressRowResponse>> zipLookup(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "20") int limit) {
    SysUser user = authService.requireUser(authorization);
    requireView(user);
    if (keyword == null || keyword.isBlank()) {
      return ApiResponse.ok(List.of());
    }
    return ApiResponse.ok(destAddressService.lookup(keyword.trim(), limit));
  }

  @Operation(
      summary = "报价单详情",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}")
  public ApiResponse<QuoteDetailResponse> get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireView(user);
    return ApiResponse.ok(quoteQueryService.getById(user, id));
  }

  @Operation(
      summary = "引入成本库匹配",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/match-costs")
  public ApiResponse<QuoteMatchCostsResponse> matchCosts(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody QuoteMatchCostsRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireView(user);
    QuoteMatchCostsResponse result = quoteCostMatchService.match(request);
    if (!result.matched()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "无对应成本数据，请先维护成本库");
    }
    return ApiResponse.ok(result);
  }

  @Operation(
      summary = "新建报价单",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<QuoteDetailResponse> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody QuoteSaveRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireCreate(user);
    return ApiResponse.ok(quoteCommandService.create(user, request));
  }

  @Operation(
      summary = "更新报价单",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<QuoteDetailResponse> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody QuoteSaveRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireEdit(user);
    return ApiResponse.ok(quoteCommandService.update(user, id, request));
  }

  @Operation(
      summary = "删除报价单",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireDelete(user);
    quoteCommandService.delete(user, id);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "提交生效",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/submit")
  public ApiResponse<QuoteDetailResponse> submit(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireSubmit(user);
    return ApiResponse.ok(quoteWorkflowService.submitEffective(user, id));
  }

  @Operation(
      summary = "标记跟进中",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/follow")
  public ApiResponse<QuoteDetailResponse> follow(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireEdit(user);
    return ApiResponse.ok(quoteWorkflowService.markFollowing(user, id));
  }

  @Operation(
      summary = "成交",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/won")
  public ApiResponse<QuoteDetailResponse> won(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireApprove(user);
    return ApiResponse.ok(quoteWorkflowService.markWon(user, id));
  }

  @Operation(
      summary = "作废",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/void")
  public ApiResponse<QuoteDetailResponse> voidQuote(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireApprove(user);
    return ApiResponse.ok(quoteWorkflowService.voidQuote(user, id));
  }

  @Operation(
      summary = "复制新建",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/copy")
  public ApiResponse<QuoteDetailResponse> copy(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireCreate(user);
    return ApiResponse.ok(quoteWorkflowService.copyAsNew(user, id));
  }

  @Operation(
      summary = "批量导出 Excel",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/export")
  public ResponseEntity<byte[]> export(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody QuoteBatchExportRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireExport(user);
    byte[] bytes = quoteExportService.exportByIds(user, request.ids());
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=quotes-export.xlsx")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(bytes);
  }

  @Operation(
      summary = "报价单操作日志",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}/operation-logs")
  public ApiResponse<PageResult<OperationLogResponse>> operationLogs(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    SysUser user = authService.requireUser(authorization);
    requireView(user);
    return ApiResponse.ok(quoteQueryService.listOperationLogs(user, id, page, pageSize));
  }

  @Operation(
      summary = "成本快照历史",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}/cost-snapshots")
  public ApiResponse<List<com.furuiduo.quote.quote.dto.QuoteCostMatchItemDto>> costSnapshots(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestParam(required = false) String costType) {
    SysUser user = authService.requireUser(authorization);
    requireView(user);
    quoteQueryService.getById(user, id);
    return ApiResponse.ok(quoteCostMatchService.listSnapshots(id, costType));
  }

  @Operation(
      summary = "跟进记录列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}/follow-ups")
  public ApiResponse<List<QuoteFollowUpResponse>> followUps(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireView(user);
    return ApiResponse.ok(quoteFollowUpService.list(user, id));
  }

  @Operation(
      summary = "新增跟进记录",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/follow-ups")
  public ApiResponse<QuoteFollowUpResponse> createFollowUp(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody QuoteFollowUpSaveRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireEdit(user);
    return ApiResponse.ok(quoteFollowUpService.create(user, id, request));
  }

  @Operation(
      summary = "更新跟进记录",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{quoteId}/follow-ups/{followUpId}")
  public ApiResponse<QuoteFollowUpResponse> updateFollowUp(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long quoteId,
      @PathVariable Long followUpId,
      @RequestBody QuoteFollowUpSaveRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireEdit(user);
    return ApiResponse.ok(quoteFollowUpService.update(user, quoteId, followUpId, request));
  }

  @Operation(
      summary = "删除跟进记录",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{quoteId}/follow-ups/{followUpId}")
  public ApiResponse<Void> deleteFollowUp(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long quoteId,
      @PathVariable Long followUpId) {
    SysUser user = authService.requireUser(authorization);
    requireEdit(user);
    quoteFollowUpService.delete(user, quoteId, followUpId);
    return ApiResponse.ok(null);
  }

  private void requireView(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.QUOTE_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权查看报价单");
    }
  }

  private void requireCreate(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.QUOTE_CREATE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权新建报价单");
    }
  }

  private void requireEdit(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.QUOTE_EDIT)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权编辑报价单");
    }
  }

  private void requireDelete(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.QUOTE_DELETE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权删除报价单");
    }
  }

  private void requireSubmit(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.QUOTE_SUBMIT)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权提交报价单");
    }
  }

  private void requireApprove(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.QUOTE_APPROVE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权审批报价单");
    }
  }

  private void requireExport(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.QUOTE_EXPORT)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权导出报价单");
    }
  }
}
