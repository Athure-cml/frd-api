package com.furuiduo.quote.quote.controller;

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

import jakarta.validation.Valid;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.auth.AuthService;
import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.common.RequestIds;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.cost.service.CostFumigationService;
import com.furuiduo.quote.mail.QuoteQuotedMailService;
import com.furuiduo.quote.masterdata.dto.DestAddressRowResponse;
import com.furuiduo.quote.masterdata.service.DestAddressService;
import com.furuiduo.quote.quote.dto.QuoteApprovalCommentRequest;
import com.furuiduo.quote.quote.dto.QuoteApprovalHistoryItem;
import com.furuiduo.quote.quote.dto.QuoteSubmitRequest;
import com.furuiduo.quote.quote.dto.QuoteWithdrawRequest;
import com.furuiduo.quote.quote.dto.QuoteApplyCostImportRequest;
import com.furuiduo.quote.quote.dto.QuoteApplyCostImportResponse;
import com.furuiduo.quote.quote.dto.QuoteBatchExportRequest;
import com.furuiduo.quote.quote.dto.QuoteGenerateSheetRequest;
import com.furuiduo.quote.quote.dto.QuoteDocFeeResponse;
import com.furuiduo.quote.quote.dto.QuoteGenerateSheetResponse;
import com.furuiduo.quote.quote.dto.QuoteDetailResponse;
import com.furuiduo.quote.quote.dto.QuoteMailLogResponse;
import com.furuiduo.quote.quote.dto.QuoteMailSendRequest;
import com.furuiduo.quote.quote.entity.QuoteMailTrigger;
import com.furuiduo.quote.quote.repository.QuoteMailLogRepository;
import com.furuiduo.quote.quote.dto.QuoteFollowUpResponse;
import com.furuiduo.quote.quote.dto.QuoteFollowUpSaveRequest;
import com.furuiduo.quote.quote.dto.QuoteListItem;
import com.furuiduo.quote.quote.dto.QuoteMatchCostsRequest;
import com.furuiduo.quote.quote.dto.QuoteMatchCostsResponse;
import com.furuiduo.quote.quote.dto.QuoteSaveRequest;
import com.furuiduo.quote.quote.service.QuoteCommandService;
import com.furuiduo.quote.quote.service.QuoteCostImportApplyService;
import com.furuiduo.quote.quote.service.QuoteCostMatchService;
import com.furuiduo.quote.quote.service.QuoteExportService;
import com.furuiduo.quote.quote.service.QuoteFollowUpService;
import com.furuiduo.quote.quote.service.QuoteQueryService;
import com.furuiduo.quote.quote.service.QuoteSheetGenerateService;
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

  /** 人工发送收件邮箱格式校验 */
  private static final java.util.regex.Pattern EMAIL_PATTERN =
      java.util.regex.Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");

  private final AuthService authService;
  private final PermissionService permissionService;
  private final QuoteQueryService quoteQueryService;
  private final QuoteCommandService quoteCommandService;
  private final QuoteCostMatchService quoteCostMatchService;
  private final QuoteWorkflowService quoteWorkflowService;
  private final QuoteFollowUpService quoteFollowUpService;
  private final QuoteExportService quoteExportService;
  private final DestAddressService destAddressService;
  private final QuoteSheetGenerateService quoteSheetGenerateService;
  private final QuoteCostImportApplyService quoteCostImportApplyService;
  private final CostFumigationService costFumigationService;
  private final QuoteMailLogRepository quoteMailLogRepository;
  private final QuoteQuotedMailService quoteQuotedMailService;

  public QuoteController(
      AuthService authService,
      PermissionService permissionService,
      QuoteQueryService quoteQueryService,
      QuoteCommandService quoteCommandService,
      QuoteCostMatchService quoteCostMatchService,
      QuoteWorkflowService quoteWorkflowService,
      QuoteFollowUpService quoteFollowUpService,
      QuoteExportService quoteExportService,
      DestAddressService destAddressService,
      QuoteSheetGenerateService quoteSheetGenerateService,
      QuoteCostImportApplyService quoteCostImportApplyService,
      CostFumigationService costFumigationService,
      QuoteMailLogRepository quoteMailLogRepository,
      QuoteQuotedMailService quoteQuotedMailService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.quoteQueryService = quoteQueryService;
    this.quoteCommandService = quoteCommandService;
    this.quoteCostMatchService = quoteCostMatchService;
    this.quoteWorkflowService = quoteWorkflowService;
    this.quoteFollowUpService = quoteFollowUpService;
    this.quoteExportService = quoteExportService;
    this.destAddressService = destAddressService;
    this.quoteSheetGenerateService = quoteSheetGenerateService;
    this.quoteCostImportApplyService = quoteCostImportApplyService;
    this.quoteMailLogRepository = quoteMailLogRepository;
    this.quoteQuotedMailService = quoteQuotedMailService;
    this.costFumigationService = costFumigationService;
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
      @RequestParam(required = false) String pickUpAddress,
      @RequestParam(required = false) String fumigationPoint,
      @RequestParam(required = false) String supplier,
      @RequestParam(required = false) String ssl,
      @RequestParam(required = false) String followUpByName,
      @RequestParam(required = false) Long createdBy,
      @RequestParam(required = false) String libraryMode,
      @RequestParam(required = false) Long libraryCostId) {
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
            pickUpAddress,
            fumigationPoint,
            supplier,
            ssl,
            followUpByName,
            createdBy,
            libraryMode,
            libraryCostId));
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
      summary = "熏蒸点 STATION 选项（熏蒸成本库去重）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/fumigation-stations")
  public ApiResponse<List<String>> fumigationStations(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    SysUser user = authService.requireUser(authorization);
    requireView(user);
    return ApiResponse.ok(costFumigationService.listDistinctStations());
  }

  @Operation(
      summary = "按 POD 自动匹配单证费",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/doc-fee")
  public ApiResponse<QuoteDocFeeResponse> previewDocFee(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String pod) {
    SysUser user = authService.requireUser(authorization);
    requireView(user);
    return ApiResponse.ok(
        new QuoteDocFeeResponse(quoteSheetGenerateService.previewDocFee(pod)));
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
    requireCreateOrEdit(user);
    QuoteMatchCostsResponse result = quoteCostMatchService.match(request);
    if (!result.matched()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "无对应成本数据，请先维护成本库");
    }
    return ApiResponse.ok(result);
  }

  @Operation(
      summary = "生成报价单（按业务规则自动取值）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/generate-sheet")
  public ApiResponse<QuoteGenerateSheetResponse> generateSheet(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody QuoteGenerateSheetRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireCreateOrEdit(user);
    return ApiResponse.ok(quoteSheetGenerateService.generate(request));
  }

  @Operation(
      summary = "引入成本库后按报价规则重算字段",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/apply-cost-import")
  public ApiResponse<QuoteApplyCostImportResponse> applyCostImport(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody QuoteApplyCostImportRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireCreateOrEdit(user);
    return ApiResponse.ok(quoteCostImportApplyService.apply(request));
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
      summary = "退回报价单（取消待报价，询价单回到待递交）",
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
      summary = "提交审批",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/submit")
  public ApiResponse<QuoteDetailResponse> submit(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody(required = false) @Valid QuoteSubmitRequest body) {
    SysUser user = authService.requireUser(authorization);
    requireSubmit(user);
    String comment = body != null ? body.comment() : null;
    return ApiResponse.ok(quoteWorkflowService.submitForApproval(user, id, comment));
  }

  @Operation(
      summary = "发送报价",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/send")
  public ApiResponse<QuoteDetailResponse> send(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody(required = false) @Valid QuoteApprovalCommentRequest body) {
    SysUser user = authService.requireUser(authorization);
    requireApprove(user);
    String comment = body != null ? body.comment() : null;
    return ApiResponse.ok(quoteWorkflowService.markSent(user, id, comment));
  }

  @Operation(
      summary = "发送报价（兼容旧接口）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/follow")
  public ApiResponse<QuoteDetailResponse> follow(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody(required = false) @Valid QuoteApprovalCommentRequest body) {
    SysUser user = authService.requireUser(authorization);
    requireApprove(user);
    String comment = body != null ? body.comment() : null;
    return ApiResponse.ok(quoteWorkflowService.markSent(user, id, comment));
  }

  @Operation(
      summary = "审批驳回（待审批退回草稿）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/reject-approval")
  public ApiResponse<QuoteDetailResponse> rejectApproval(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody @Valid QuoteApprovalCommentRequest body) {
    SysUser user = authService.requireUser(authorization);
    requireApprove(user);
    String comment = body != null ? body.comment() : null;
    if (comment == null || comment.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写审批意见");
    }
    return ApiResponse.ok(quoteWorkflowService.rejectApproval(user, id, comment));
  }

  @Operation(
      summary = "撤回审批",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/cancel-approval")
  public ApiResponse<QuoteDetailResponse> cancelApproval(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody @Valid QuoteWithdrawRequest body) {
    SysUser user = authService.requireUser(authorization);
    requireSubmit(user);
    return ApiResponse.ok(quoteWorkflowService.cancelApproval(user, id, body.comment()));
  }

  @Operation(
      summary = "拒绝报价",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/reject")
  public ApiResponse<QuoteDetailResponse> reject(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireApprove(user);
    return ApiResponse.ok(quoteWorkflowService.reject(user, id));
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
      summary = "确认报价",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/confirm")
  public ApiResponse<QuoteDetailResponse> confirmQuote(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireEdit(user);
    return ApiResponse.ok(quoteWorkflowService.confirmQuote(user, id));
  }

  @Operation(
      summary = "已报价发信流水",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}/mail-logs")
  public ApiResponse<List<QuoteMailLogResponse>> mailLogs(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireView(user);
    return ApiResponse.ok(
        quoteMailLogRepository.findByQuoteIdOrderByCreatedAtDescIdDesc(id).stream()
            .map(QuoteMailLogResponse::from)
            .toList());
  }

  @Operation(
      summary = "重新发送已报价邮件",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/mail/resend")
  public ApiResponse<List<QuoteMailLogResponse>> resendMail(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireEdit(user);
    QuoteDetailResponse detail = quoteQueryService.getById(user, id);
    if (!"QUOTED".equals(detail.status())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅已报价状态可重发邮件");
    }
    quoteQuotedMailService.scheduleAfterQuoted(
        id, user.getId(), user.getRealName(), QuoteMailTrigger.RESEND);
    return ApiResponse.ok(
        quoteMailLogRepository.findByQuoteIdOrderByCreatedAtDescIdDesc(id).stream()
            .map(QuoteMailLogResponse::from)
            .toList());
  }

  @Operation(
      summary = "人工发送报价单邮件（指定收件邮箱）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/mail/send")
  public ApiResponse<List<QuoteMailLogResponse>> sendMail(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody(required = false) QuoteMailSendRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireEdit(user);
    QuoteDetailResponse detail = quoteQueryService.getById(user, id);
    if (!"QUOTED".equals(detail.status())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅已报价状态可发送报价单邮件");
    }
    List<String> emails = normalizeEmails(request == null ? null : request.toEmails());
    if (emails.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写收件邮箱");
    }
    for (String email : emails) {
      if (!EMAIL_PATTERN.matcher(email).matches()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱格式不正确：" + email);
      }
    }
    String blocked = quoteQuotedMailService.manualSendBlockedReason();
    if (blocked != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, blocked);
    }
    quoteQuotedMailService.scheduleManualSend(id, emails, user.getId(), user.getRealName());
    return ApiResponse.ok(
        quoteMailLogRepository.findByQuoteIdOrderByCreatedAtDescIdDesc(id).stream()
            .map(QuoteMailLogResponse::from)
            .toList());
  }

  /** 收件邮箱归一：支持逗号/分号/空格分隔的整串，去空去重。 */
  private static List<String> normalizeEmails(List<String> raw) {
    if (raw == null || raw.isEmpty()) {
      return List.of();
    }
    return raw.stream()
        .filter(item -> item != null && !item.isBlank())
        .flatMap(item -> java.util.Arrays.stream(item.split("[,;，；\\s]+")))
        .map(String::trim)
        .filter(item -> !item.isEmpty())
        .distinct()
        .toList();
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
      summary = "关闭成本风险警示（人工复核）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/dismiss-cost-risk")
  public ApiResponse<QuoteDetailResponse> dismissCostRisk(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireEdit(user);
    return ApiResponse.ok(quoteCommandService.dismissCostRisk(user, id));
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
      summary = "变更保存（新单直接落已报价，旧版归档）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/revise")
  public ApiResponse<QuoteDetailResponse> revise(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody QuoteSaveRequest body) {
    SysUser user = authService.requireUser(authorization);
    requireCreate(user);
    requireEdit(user);
    return ApiResponse.ok(quoteWorkflowService.commitRevision(user, id, body));
  }

  @Operation(
      summary = "导出 Excel（勾选优先，否则按搜索条件，无条件则全部）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/export")
  public ResponseEntity<byte[]> export(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String ids,
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
      @RequestParam(required = false) String pickUpAddress,
      @RequestParam(required = false) String fumigationPoint,
      @RequestParam(required = false) String supplier,
      @RequestParam(required = false) String ssl,
      @RequestParam(required = false) String followUpByName,
      @RequestParam(required = false) Long createdBy) {
    SysUser user = authService.requireUser(authorization);
    requireExport(user);
    byte[] bytes =
        quoteExportService.export(
            user,
            RequestIds.parse(ids),
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
            pickUpAddress,
            fumigationPoint,
            supplier,
            ssl,
            followUpByName,
            createdBy);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename*=UTF-8''"
                + URLEncoder.encode("报价单.xlsx", StandardCharsets.UTF_8).replace("+", "%20"))
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(bytes);
  }

  @Operation(
      summary = "批量导出 Excel（兼容旧 POST）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/export")
  public ResponseEntity<byte[]> exportPost(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody QuoteBatchExportRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireExport(user);
    byte[] bytes = quoteExportService.exportByIds(user, request.ids());
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename*=UTF-8''"
                + URLEncoder.encode("报价单.xlsx", StandardCharsets.UTF_8).replace("+", "%20"))
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(bytes);
  }

  @Operation(
      summary = "报价单审批日志",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}/approval-logs")
  public ApiResponse<List<QuoteApprovalHistoryItem>> approvalLogs(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireView(user);
    return ApiResponse.ok(quoteQueryService.listApprovalLogs(user, id));
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

  private void requireCreateOrEdit(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.QUOTE_CREATE)
        && !permissionService.hasPermission(user, PermissionCodes.QUOTE_EDIT)) {
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
