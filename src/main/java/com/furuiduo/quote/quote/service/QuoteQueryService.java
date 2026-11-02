package com.furuiduo.quote.quote.service;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.approval.service.ApprovalConfigService;
import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.common.SearchText;
import com.furuiduo.quote.quote.dto.QuoteApprovalHistoryItem;
import com.furuiduo.quote.quote.dto.QuoteDetailResponse;
import com.furuiduo.quote.quote.dto.QuoteListItem;
import com.furuiduo.quote.quote.entity.QuoteCostType;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.support.QuoteApprovalSupport;
import com.furuiduo.quote.quote.support.QuoteCostRiskSupport;
import com.furuiduo.quote.quote.entity.QuoteStatus;
import com.furuiduo.quote.quote.entity.QuoteTransportMode;
import com.furuiduo.quote.quote.repository.QuoteOrderRepository;
import com.furuiduo.quote.quote.support.QuoteLibraryModeSupport;
import com.furuiduo.quote.quote.support.QuoteStatusSupport;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.OperationLogService;
import com.furuiduo.quote.sys.service.PermissionService;
import com.furuiduo.quote.sys.support.DataScopeFilter;

@Service
public class QuoteQueryService {

  private final QuoteOrderRepository quoteOrderRepository;
  private final PermissionService permissionService;
  private final QuoteAccessService quoteAccessService;
  private final QuoteCostMatchService quoteCostMatchService;
  private final QuoteFollowUpService quoteFollowUpService;
  private final OperationLogService operationLogService;
  private final QuoteApprovalLogService quoteApprovalLogService;
  private final ApprovalConfigService approvalConfigService;
  private final InquiryQuoteSyncService inquiryQuoteSyncService;

  public QuoteQueryService(
      QuoteOrderRepository quoteOrderRepository,
      PermissionService permissionService,
      QuoteAccessService quoteAccessService,
      QuoteCostMatchService quoteCostMatchService,
      QuoteFollowUpService quoteFollowUpService,
      OperationLogService operationLogService,
      QuoteApprovalLogService quoteApprovalLogService,
      ApprovalConfigService approvalConfigService,
      InquiryQuoteSyncService inquiryQuoteSyncService) {
    this.quoteOrderRepository = quoteOrderRepository;
    this.permissionService = permissionService;
    this.quoteAccessService = quoteAccessService;
    this.quoteCostMatchService = quoteCostMatchService;
    this.quoteFollowUpService = quoteFollowUpService;
    this.operationLogService = operationLogService;
    this.quoteApprovalLogService = quoteApprovalLogService;
    this.approvalConfigService = approvalConfigService;
    this.inquiryQuoteSyncService = inquiryQuoteSyncService;
  }

  @Transactional
  public PageResult<QuoteListItem> list(
      SysUser user,
      int page,
      int pageSize,
      String quoteNo,
      String customerName,
      String transportMode,
      String status,
      String zipCode,
      String city,
      String state,
      String por,
      String pol,
      String pod,
      String pickUpAddress,
      String fumigationPoint,
      String supplier,
      String ssl,
      String followUpByName,
      Long createdBy,
      String libraryMode,
      Long libraryCostId) {
    inquiryQuoteSyncService.syncExpiredQuotesAndInquiries();
    DataScopeFilter scope = DataScopeFilter.from(user, permissionService);

    var pageable =
        PageRequest.of(
            Math.max(page - 1, 0),
            Math.max(pageSize, 1),
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

    QuoteCostType libraryCostType = parseLibraryMode(libraryMode);
    if (libraryCostId != null && libraryCostId > 0 && libraryCostType == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "反查报价单需同时指定 libraryMode");
    }

    String qNo = SearchText.orEmpty(quoteNo);
    String qCustomer = SearchText.orEmpty(customerName);
    QuoteTransportMode qTransport = parseTransportMode(transportMode);
    Set<QuoteStatus> qStatuses = QuoteStatusSupport.statusesForBusinessFilter(status);
    boolean statusFilterEmpty = qStatuses == null || qStatuses.isEmpty();
    Collection<QuoteStatus> statusArgs =
        statusFilterEmpty ? EnumSet.of(QuoteStatus.PENDING_QUOTE) : qStatuses;
    String qZip = SearchText.orEmpty(zipCode);
    String qCity = SearchText.orEmpty(city);
    String qState = SearchText.orEmpty(state);
    String qPor = SearchText.orEmpty(por);
    String qPol = SearchText.orEmpty(pol);
    String qPod = SearchText.orEmpty(pod);
    String qPickUp = SearchText.orEmpty(pickUpAddress);
    String qFumigation = SearchText.orEmpty(fumigationPoint);
    String qSupplier = SearchText.orEmpty(supplier);
    String qSsl = SearchText.orEmpty(ssl);
    String qFollow = SearchText.orEmpty(followUpByName);

    var result =
        libraryCostId != null && libraryCostId > 0 && libraryCostType != null
            ? quoteOrderRepository.searchByLibraryUsage(
                QuoteLibraryModeSupport.toMode(libraryCostType),
                libraryCostId,
                qNo,
                qCustomer,
                qTransport,
                statusFilterEmpty,
                statusArgs,
                qZip,
                qCity,
                qState,
                qPor,
                qPol,
                qPod,
                qPickUp,
                qFumigation,
                qSupplier,
                qSsl,
                qFollow,
                createdBy,
                scope.scopeAll(),
                scope.scopeDept(),
                scope.scopeAssigned(),
                scope.scopeSelf(),
                scope.deptId(),
                scope.userId(),
                scope.assignedUserIds(),
                pageable)
            : libraryCostType == null
                ? quoteOrderRepository.search(
                    qNo,
                    qCustomer,
                    qTransport,
                    statusFilterEmpty,
                    statusArgs,
                    qZip,
                    qCity,
                    qState,
                    qPor,
                    qPol,
                    qPod,
                    qPickUp,
                    qFumigation,
                    qSupplier,
                    qSsl,
                    qFollow,
                    createdBy,
                    scope.scopeAll(),
                    scope.scopeDept(),
                    scope.scopeAssigned(),
                    scope.scopeSelf(),
                    scope.deptId(),
                    scope.userId(),
                    scope.assignedUserIds(),
                    pageable)
                : quoteOrderRepository.searchWithCostSnapshot(
                    libraryCostType,
                    qNo,
                    qCustomer,
                    qTransport,
                    statusFilterEmpty,
                    statusArgs,
                    qZip,
                    qCity,
                    qState,
                    qPor,
                    qPol,
                    qPod,
                    qPickUp,
                    qFumigation,
                    qSupplier,
                    qSsl,
                    qFollow,
                    createdBy,
                    scope.scopeAll(),
                    scope.scopeDept(),
                    scope.scopeAssigned(),
                    scope.scopeSelf(),
                    scope.deptId(),
                    scope.userId(),
                    scope.assignedUserIds(),
                    pageable);

    Map<Long, Map<String, Object>> libraryRows =
        libraryCostType == null
            ? Map.of()
            : quoteCostMatchService.buildLibraryRows(result.getContent(), libraryCostType);

    List<Long> pageQuoteIds = result.getContent().stream().map(QuoteOrder::getId).toList();
    Map<Long, String> roadSuppliers = quoteCostMatchService.roadSuppliersByQuoteIds(pageQuoteIds);

    return new PageResult<>(
        result.getContent().stream()
            .map(
                order ->
                    QuoteListItem.from(
                        order,
                        quoteAccessService.canOperate(user, order),
                        libraryRows.get(order.getId()),
                        roadSuppliers.get(order.getId())))
            .toList(),
        result.getTotalElements());
  }

  /** 导出用：按列表同款筛选条件返回实体（无筛选即权限范围内全部）。 */
  public List<QuoteOrder> findOrdersForExport(
      SysUser user,
      String quoteNo,
      String customerName,
      String transportMode,
      String status,
      String zipCode,
      String city,
      String state,
      String por,
      String pol,
      String pod,
      String pickUpAddress,
      String fumigationPoint,
      String supplier,
      String ssl,
      String followUpByName,
      Long createdBy) {
    DataScopeFilter scope = DataScopeFilter.from(user, permissionService);
    var pageable =
        Pageable.unpaged(
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
    Set<QuoteStatus> qStatuses = QuoteStatusSupport.statusesForBusinessFilter(status);
    boolean statusFilterEmpty = qStatuses == null || qStatuses.isEmpty();
    Collection<QuoteStatus> statusArgs =
        statusFilterEmpty ? EnumSet.of(QuoteStatus.PENDING_QUOTE) : qStatuses;
    return quoteOrderRepository
        .search(
            SearchText.orEmpty(quoteNo),
            SearchText.orEmpty(customerName),
            parseTransportMode(transportMode),
            statusFilterEmpty,
            statusArgs,
            SearchText.orEmpty(zipCode),
            SearchText.orEmpty(city),
            SearchText.orEmpty(state),
            SearchText.orEmpty(por),
            SearchText.orEmpty(pol),
            SearchText.orEmpty(pod),
            SearchText.orEmpty(pickUpAddress),
            SearchText.orEmpty(fumigationPoint),
            SearchText.orEmpty(supplier),
            SearchText.orEmpty(ssl),
            SearchText.orEmpty(followUpByName),
            createdBy,
            scope.scopeAll(),
            scope.scopeDept(),
            scope.scopeAssigned(),
            scope.scopeSelf(),
            scope.deptId(),
            scope.userId(),
            scope.assignedUserIds(),
            pageable)
        .getContent();
  }

  @Transactional
  public QuoteDetailResponse getById(SysUser user, Long id) {
    QuoteOrder order =
        quoteOrderRepository
            .findWithLinesById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "报价单不存在"));
    quoteAccessService.assertReadable(user, order);
    if (QuoteStatusSupport.applyExpiredAsVoided(order)) {
      order.setUpdatedAt(java.time.LocalDateTime.now());
      order = quoteOrderRepository.save(order);
      if (InquiryQuoteSyncService.isExpiredArchive(order)) {
        inquiryQuoteSyncService.onQuoteExpired(order);
      }
    }
    return QuoteDetailResponse.from(
        order,
        quoteCostMatchService.listSnapshots(id, null),
        quoteFollowUpService.list(user, id),
        quoteAccessService.canOperate(user, order),
        QuoteCostRiskSupport.parseModes(order.getCostRiskReason()));
  }

  public PageResult<com.furuiduo.quote.sys.dto.OperationLogResponse> listOperationLogs(
      SysUser user, Long quoteId, int page, int pageSize) {
    quoteAccessService.requireReadable(user, quoteId);
    return operationLogService.listForQuote(quoteId, page, pageSize);
  }

  public List<QuoteApprovalHistoryItem> listApprovalLogs(SysUser user, Long quoteId) {
    quoteAccessService.requireReadable(user, quoteId);
    List<QuoteApprovalSupport.FlowNode> flow =
        approvalConfigService.findQuoteFlowSteps().stream()
            .map(step -> new QuoteApprovalSupport.FlowNode(step.approverId(), step.approverName()))
            .toList();
    return quoteApprovalLogService.listHistory(quoteId, flow);
  }

  private QuoteTransportMode parseTransportMode(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return QuoteTransportMode.valueOf(value);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的运输方式");
    }
  }

  private QuoteCostType parseLibraryMode(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return switch (value.trim().toLowerCase()) {
      case "road" -> QuoteCostType.ROAD;
      case "sea" -> QuoteCostType.SEA;
      case "fumigation" -> QuoteCostType.FUMIGATION;
      default -> null;
    };
  }

  void assertReadable(SysUser user, QuoteOrder order) {
    quoteAccessService.assertReadable(user, order);
  }
}
