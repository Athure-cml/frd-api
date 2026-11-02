package com.furuiduo.quote.dashboard.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.approval.service.ApprovalConfigService;
import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostHighlightMode;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.entity.CostSea;
import com.furuiduo.quote.cost.repository.CostFumigationRepository;
import com.furuiduo.quote.cost.repository.CostRoadRepository;
import com.furuiduo.quote.cost.repository.CostSeaRepository;
import com.furuiduo.quote.dashboard.dto.NotificationItemDto;
import com.furuiduo.quote.dashboard.dto.WorkspaceMetricDto;
import com.furuiduo.quote.dashboard.dto.WorkspaceNoticeDto;
import com.furuiduo.quote.dashboard.dto.WorkspacePipelineDto;
import com.furuiduo.quote.dashboard.dto.WorkspaceQuoteStatsDto;
import com.furuiduo.quote.dashboard.dto.WorkspaceResponse;
import com.furuiduo.quote.dashboard.dto.WorkspaceRouteDto;
import com.furuiduo.quote.dashboard.dto.WorkspaceTodoDto;
import com.furuiduo.quote.dashboard.entity.SysUserNotificationState;
import com.furuiduo.quote.dashboard.repository.DashboardQueryRepository;
import com.furuiduo.quote.dashboard.repository.SysUserNotificationStateRepository;
import com.furuiduo.quote.dashboard.support.DashboardScopeParams;
import com.furuiduo.quote.quote.entity.QuoteApprovalLog;
import com.furuiduo.quote.quote.entity.QuoteCostSnapshot;
import com.furuiduo.quote.quote.entity.QuoteCostType;
import com.furuiduo.quote.quote.entity.QuoteLibraryConfirmStatus;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.entity.QuoteStatus;
import com.furuiduo.quote.quote.repository.QuoteApprovalLogRepository;
import com.furuiduo.quote.quote.repository.QuoteCostSnapshotRepository;
import com.furuiduo.quote.quote.repository.QuoteLibraryEntryRepository;
import com.furuiduo.quote.quote.repository.QuoteMailLogRepository;
import com.furuiduo.quote.quote.support.QuoteApprovalSupport;
import com.furuiduo.quote.quote.support.QuoteCostRiskSupport;
import com.furuiduo.quote.quote.support.QuoteDateTimes;
import com.furuiduo.quote.quote.support.QuoteStatusSupport;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

@Service
public class DashboardService {

  private static final int EXPIRING_DAYS = 3;

  private final DashboardQueryRepository dashboardQueryRepository;
  private final QuoteCostSnapshotRepository quoteCostSnapshotRepository;
  private final CostRoadRepository costRoadRepository;
  private final CostSeaRepository costSeaRepository;
  private final CostFumigationRepository costFumigationRepository;
  private final QuoteLibraryEntryRepository quoteLibraryEntryRepository;
  private final PermissionService permissionService;
  private final ApprovalConfigService approvalConfigService;
  private final QuoteApprovalLogRepository quoteApprovalLogRepository;
  private final SysUserNotificationStateRepository notificationStateRepository;
  private final QuoteMailLogRepository quoteMailLogRepository;

  public DashboardService(
      DashboardQueryRepository dashboardQueryRepository,
      QuoteCostSnapshotRepository quoteCostSnapshotRepository,
      CostRoadRepository costRoadRepository,
      CostSeaRepository costSeaRepository,
      CostFumigationRepository costFumigationRepository,
      QuoteLibraryEntryRepository quoteLibraryEntryRepository,
      PermissionService permissionService,
      ApprovalConfigService approvalConfigService,
      QuoteApprovalLogRepository quoteApprovalLogRepository,
      SysUserNotificationStateRepository notificationStateRepository,
      QuoteMailLogRepository quoteMailLogRepository) {
    this.dashboardQueryRepository = dashboardQueryRepository;
    this.quoteCostSnapshotRepository = quoteCostSnapshotRepository;
    this.costRoadRepository = costRoadRepository;
    this.costSeaRepository = costSeaRepository;
    this.costFumigationRepository = costFumigationRepository;
    this.quoteLibraryEntryRepository = quoteLibraryEntryRepository;
    this.permissionService = permissionService;
    this.approvalConfigService = approvalConfigService;
    this.quoteApprovalLogRepository = quoteApprovalLogRepository;
    this.notificationStateRepository = notificationStateRepository;
    this.quoteMailLogRepository = quoteMailLogRepository;
  }

  public WorkspaceResponse getWorkspace(SysUser user) {
    DashboardScopeParams scope = DashboardScopeParams.from(user, permissionService);
    LocalDate today = LocalDate.now();
    LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
    LocalDateTime tomorrow = today.plusDays(1).atStartOfDay();
    LocalDateTime todayStart = today.atStartOfDay();
    LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();

    List<WorkspaceMetricDto> metrics =
        buildMetrics(scope, monthStart, tomorrow, todayStart, yesterdayStart);
    List<WorkspaceTodoDto> todos = buildTodos(scope, user, 6);
    List<WorkspacePipelineDto> pipeline = buildPipeline(scope);
    List<WorkspaceNoticeDto> notices = buildNotices(user, today);
    Map<String, SysUserNotificationState> stateById =
        loadNotificationStates(user.getId(), notices);
    notices =
        notices.stream()
            .filter(
                notice -> {
                  SysUserNotificationState state = stateById.get(notice.id());
                  return state == null || !Boolean.TRUE.equals(state.getDismissed());
                })
            .toList();
    List<WorkspaceRouteDto> topRoutes = buildTopRoutes(scope, 4);
    WorkspaceQuoteStatsDto quoteStats = buildQuoteStats(scope, today);

    return new WorkspaceResponse(metrics, todos, pipeline, notices, topRoutes, quoteStats);
  }

  public List<NotificationItemDto> getNotifications(SysUser user) {
    LocalDate today = LocalDate.now();
    List<WorkspaceNoticeDto> notices = buildNotices(user, today);
    Map<String, SysUserNotificationState> stateById = loadNotificationStates(user.getId(), notices);
    List<NotificationItemDto> items = new ArrayList<>();
    for (WorkspaceNoticeDto notice : notices) {
      SysUserNotificationState state = stateById.get(notice.id());
      if (state != null && Boolean.TRUE.equals(state.getDismissed())) {
        continue;
      }
      boolean isRead = state != null && Boolean.TRUE.equals(state.getIsRead());
      items.add(toNotificationItem(notice, isRead));
    }
    return items;
  }

  @Transactional
  public void markNotificationRead(SysUser user, String noticeId) {
    if (noticeId == null || noticeId.isBlank()) {
      return;
    }
    SysUserNotificationState state = requireOrCreateState(user.getId(), noticeId.trim());
    state.setIsRead(true);
    state.setUpdatedAt(LocalDateTime.now());
    notificationStateRepository.save(state);
  }

  @Transactional
  public void markAllNotificationsRead(SysUser user) {
    List<WorkspaceNoticeDto> notices = buildNotices(user, LocalDate.now());
    for (WorkspaceNoticeDto notice : notices) {
      SysUserNotificationState state = requireOrCreateState(user.getId(), notice.id());
      if (Boolean.TRUE.equals(state.getDismissed())) {
        continue;
      }
      state.setIsRead(true);
      state.setUpdatedAt(LocalDateTime.now());
      notificationStateRepository.save(state);
    }
  }

  @Transactional
  public void dismissNotification(SysUser user, String noticeId) {
    if (noticeId == null || noticeId.isBlank()) {
      return;
    }
    SysUserNotificationState state = requireOrCreateState(user.getId(), noticeId.trim());
    state.setIsRead(true);
    state.setDismissed(true);
    state.setUpdatedAt(LocalDateTime.now());
    notificationStateRepository.save(state);
  }

  @Transactional
  public void dismissAllNotifications(SysUser user) {
    List<WorkspaceNoticeDto> notices = buildNotices(user, LocalDate.now());
    for (WorkspaceNoticeDto notice : notices) {
      SysUserNotificationState state = requireOrCreateState(user.getId(), notice.id());
      state.setIsRead(true);
      state.setDismissed(true);
      state.setUpdatedAt(LocalDateTime.now());
      notificationStateRepository.save(state);
    }
  }

  private Map<String, SysUserNotificationState> loadNotificationStates(
      Long userId, List<WorkspaceNoticeDto> notices) {
    if (notices.isEmpty()) {
      return Map.of();
    }
    List<String> ids = notices.stream().map(WorkspaceNoticeDto::id).toList();
    return notificationStateRepository.findByUserIdAndNoticeIdIn(userId, ids).stream()
        .collect(
            Collectors.toMap(
                SysUserNotificationState::getNoticeId, item -> item, (left, right) -> left));
  }

  private SysUserNotificationState requireOrCreateState(Long userId, String noticeId) {
    return notificationStateRepository
        .findByUserIdAndNoticeId(userId, noticeId)
        .orElseGet(
            () -> {
              SysUserNotificationState created = new SysUserNotificationState();
              created.setUserId(userId);
              created.setNoticeId(noticeId);
              created.setIsRead(false);
              created.setDismissed(false);
              created.setUpdatedAt(LocalDateTime.now());
              return created;
            });
  }

  private List<WorkspaceMetricDto> buildMetrics(
      DashboardScopeParams scope,
      LocalDateTime monthStart,
      LocalDateTime tomorrow,
      LocalDateTime todayStart,
      LocalDateTime yesterdayStart) {
    long monthInquiries =
        dashboardQueryRepository.countInquiriesCreatedBetween(scope, monthStart, tomorrow);
    long monthInquiriesTrend =
        dashboardQueryRepository.countInquiriesCreatedBetween(scope, todayStart, tomorrow)
            - dashboardQueryRepository.countInquiriesCreatedBetween(
                scope, yesterdayStart, todayStart);

    long monthQuoted = dashboardQueryRepository.countQuotedBetween(scope, monthStart, tomorrow);
    long monthQuotedTrend =
        dashboardQueryRepository.countQuotedBetween(scope, todayStart, tomorrow)
            - dashboardQueryRepository.countQuotedBetween(scope, yesterdayStart, todayStart);

    // 转换率只统计有关联询价单的已报价，避免历史无询价报价单抬高比率
    long monthQuotedLinked =
        dashboardQueryRepository.countQuotedLinkedInquiryBetween(scope, monthStart, tomorrow);
    long conversionRate =
        monthInquiries == 0 ? 0 : Math.round(monthQuotedLinked * 100.0 / monthInquiries);
    long inquiriesToday =
        dashboardQueryRepository.countInquiriesCreatedBetween(scope, todayStart, tomorrow);
    long quotedLinkedToday =
        dashboardQueryRepository.countQuotedLinkedInquiryBetween(scope, todayStart, tomorrow);
    long inquiriesYesterday =
        dashboardQueryRepository.countInquiriesCreatedBetween(scope, yesterdayStart, todayStart);
    long quotedLinkedYesterday =
        dashboardQueryRepository.countQuotedLinkedInquiryBetween(scope, yesterdayStart, todayStart);
    long conversionToday =
        inquiriesToday == 0 ? 0 : Math.round(quotedLinkedToday * 100.0 / inquiriesToday);
    long conversionYesterday =
        inquiriesYesterday == 0
            ? 0
            : Math.round(quotedLinkedYesterday * 100.0 / inquiriesYesterday);
    long conversionTrend = conversionToday - conversionYesterday;

    long pendingQuote = dashboardQueryRepository.countPendingQuote(scope);
    long pendingQuoteTrend =
        dashboardQueryRepository.countByStatusesUpdatedBetween(
                scope, List.of("PENDING_QUOTE", "DRAFT"), todayStart, tomorrow)
            - dashboardQueryRepository.countByStatusesUpdatedBetween(
                scope, List.of("PENDING_QUOTE", "DRAFT"), yesterdayStart, todayStart);

    long costRisk = dashboardQueryRepository.countCostRiskActive(scope);
    long costRiskTrend =
        dashboardQueryRepository.countCostRiskActiveUpdatedBetween(scope, todayStart, tomorrow)
            - dashboardQueryRepository.countCostRiskActiveUpdatedBetween(
                scope, yesterdayStart, todayStart);

    return List.of(
        new WorkspaceMetricDto("monthInquiries", monthInquiries, monthInquiriesTrend),
        new WorkspaceMetricDto("monthQuoted", monthQuoted, monthQuotedTrend),
        new WorkspaceMetricDto("conversionRate", conversionRate, conversionTrend),
        new WorkspaceMetricDto("pendingQuote", pendingQuote, pendingQuoteTrend),
        new WorkspaceMetricDto("costRisk", costRisk, costRiskTrend));
  }

  public List<WorkspaceTodoDto> listTodos(SysUser user) {
    DashboardScopeParams scope = DashboardScopeParams.from(user, permissionService);
    return buildTodos(scope, user, 50);
  }

  public List<WorkspaceRouteDto> listTopRoutes(SysUser user) {
    DashboardScopeParams scope = DashboardScopeParams.from(user, permissionService);
    return buildTopRoutes(scope, 20);
  }

  private List<WorkspaceTodoDto> buildTodos(
      DashboardScopeParams scope, SysUser user, int limit) {
    int capped = Math.max(1, Math.min(limit, 100));
    DashboardScopeParams mine = DashboardScopeParams.self(user);
    List<WorkspaceTodoDto> todos = new ArrayList<>();
    // 待确认报价库：按海运/卡车/熏蒸分条提示
    for (WorkspaceTodoDto libraryTodo : buildLibraryConfirmTodos(user)) {
      if (todos.size() >= capped) {
        break;
      }
      todos.add(libraryTodo);
    }
    // 成本异常提醒：待报价 / 已报价均保留
    if (todos.size() < capped) {
      int riskLimit = capped - todos.size();
      for (QuoteOrder order : dashboardQueryRepository.findCostRiskQuotes(mine, riskLimit)) {
        todos.add(toCostRiskTodo(order));
      }
    }
    // 待发送邮件：已报价发信失败，仅报价单操作人可见
    if (todos.size() < capped) {
      int mailLimit = capped - todos.size();
      for (Object[] row : quoteMailLogRepository.findLatestFailedByOperator(user.getId(), mailLimit)) {
        Long quoteId = ((Number) row[0]).longValue();
        String quoteNo = row[1] == null ? "" : row[1].toString();
        String customerName = row[2] == null ? "" : row[2].toString();
        LocalDateTime failedAt = row[5] instanceof LocalDateTime t ? t : LocalDateTime.now();
        todos.add(
            new WorkspaceTodoDto(
                quoteId,
                quoteNo,
                customerName,
                "pendingMail",
                "urgent",
                formatTimeLabel(failedAt),
                false));
      }
    }
    int remaining = Math.max(0, capped - todos.size());
    if (remaining > 0) {
      int candidateLimit = Math.min(200, Math.max(20, capped * 4));
      List<QuoteOrder> candidates =
          dashboardQueryRepository.findTodoCandidates(scope, candidateLimit);
      Map<Long, List<QuoteApprovalLog>> logsById = loadApprovalLogs(candidates);
      List<QuoteApprovalSupport.FlowNode> flow = quoteFlowNodes();
      candidates.stream()
          .filter(order -> includeTodo(order, user, logsById, flow))
          .limit(remaining)
          .map(this::toTodo)
          .forEach(todos::add);
    }
    return todos;
  }

  /**
   * 待确认报价库待办：仅有对应报价库「确认」权限时展示。
   * {@code quoteNo} 存模式编码（road/sea/fumigation），{@code customer} 存待确认条数。
   */
  private List<WorkspaceTodoDto> buildLibraryConfirmTodos(SysUser user) {
    List<WorkspaceTodoDto> todos = new ArrayList<>();
    appendLibraryConfirmTodo(
        todos,
        user,
        CostHighlightMode.road,
        PermissionCodes.QUOTE_LIBRARY_ROAD_CONFIRM,
        "confirmLibraryRoad",
        -101L);
    appendLibraryConfirmTodo(
        todos,
        user,
        CostHighlightMode.sea,
        PermissionCodes.QUOTE_LIBRARY_SEA_CONFIRM,
        "confirmLibrarySea",
        -102L);
    appendLibraryConfirmTodo(
        todos,
        user,
        CostHighlightMode.fumigation,
        PermissionCodes.QUOTE_LIBRARY_FUMIGATION_CONFIRM,
        "confirmLibraryFumigation",
        -103L);
    return todos;
  }

  private void appendLibraryConfirmTodo(
      List<WorkspaceTodoDto> todos,
      SysUser user,
      CostHighlightMode mode,
      String confirmCode,
      String todoType,
      long syntheticId) {
    if (!permissionService.hasPermission(user, confirmCode)) {
      return;
    }
    long count =
        quoteLibraryEntryRepository.countByCostModeAndConfirmStatusAndArchivedFalse(
            mode, QuoteLibraryConfirmStatus.pendingConfirm);
    if (count <= 0) {
      return;
    }
    todos.add(
        new WorkspaceTodoDto(
            syntheticId,
            mode.name(),
            String.valueOf(count),
            todoType,
            "high",
            "",
            false));
  }

  private boolean includeTodo(
      QuoteOrder order,
      SysUser user,
      Map<Long, List<QuoteApprovalLog>> logsById,
      List<QuoteApprovalSupport.FlowNode> flow) {
    QuoteStatus status = QuoteStatusSupport.normalize(order.getStatus());
    // 已报价不进待办
    if (QuoteStatusSupport.isQuotedLike(status)) {
      return false;
    }
    if (status == QuoteStatus.PENDING_APPROVAL) {
      List<QuoteApprovalLog> logs = logsById.getOrDefault(order.getId(), List.of());
      return QuoteApprovalSupport.isCurrentApprover(user.getId(), logs, flow);
    }
    // 待报价：未认领时对范围内人员提醒；已认领仅提醒操作人
    if (status == QuoteStatus.PENDING_QUOTE || status == QuoteStatus.DRAFT) {
      return order.getCreatedBy() == null
          || (user.getId() != null && user.getId().equals(order.getCreatedBy()));
    }
    return false;
  }

  private List<QuoteApprovalSupport.FlowNode> quoteFlowNodes() {
    return approvalConfigService.findQuoteFlowSteps().stream()
        .map(
            step ->
                new QuoteApprovalSupport.FlowNode(step.approverId(), step.approverName()))
        .toList();
  }

  private Map<Long, List<QuoteApprovalLog>> loadApprovalLogs(List<QuoteOrder> orders) {
    List<Long> ids = orders.stream().map(QuoteOrder::getId).toList();
    if (ids.isEmpty()) {
      return Map.of();
    }
    return quoteApprovalLogRepository
        .findByQuoteOrderIdInOrderByCreatedAtAscIdAsc(ids)
        .stream()
        .collect(Collectors.groupingBy(QuoteApprovalLog::getQuoteOrderId));
  }

  private List<WorkspacePipelineDto> buildPipeline(DashboardScopeParams scope) {
    return dashboardQueryRepository.findRecentActionable(scope, 4).stream()
        .map(this::toPipeline)
        .toList();
  }

  private List<WorkspaceRouteDto> buildTopRoutes(DashboardScopeParams scope, int limit) {
    int capped = Math.max(1, Math.min(limit, 100));
    LocalDateTime since = LocalDate.now().minusDays(30).atStartOfDay();
    return dashboardQueryRepository.findTopRoutes(scope, since, capped).stream()
        .map(
            row ->
                new WorkspaceRouteDto(
                    row[0] == null ? "" : row[0].toString().trim(),
                    row[1] == null ? 0 : ((Number) row[1]).longValue()))
        .filter(item -> !item.name().isBlank())
        .toList();
  }

  private WorkspaceQuoteStatsDto buildQuoteStats(DashboardScopeParams scope, LocalDate today) {
    LocalDate startMonth = today.minusMonths(11).withDayOfMonth(1);
    LocalDateTime from = startMonth.atStartOfDay();
    LocalDateTime to = today.plusMonths(1).withDayOfMonth(1).atStartOfDay();
    Map<String, Long> inquiriesByMonth =
        monthCountMap(dashboardQueryRepository.countInquiriesByCreatedMonth(scope, from, to));
    Map<String, Long> quotedByMonth =
        monthCountMap(dashboardQueryRepository.countQuotedByMonth(scope, from, to));
    List<String> months = new ArrayList<>();
    List<Long> inquiries = new ArrayList<>();
    List<Long> quoted = new ArrayList<>();
    for (int i = 0; i < 12; i++) {
      LocalDate month = startMonth.plusMonths(i);
      String key = String.format("%04d-%02d", month.getYear(), month.getMonthValue());
      months.add(key);
      inquiries.add(inquiriesByMonth.getOrDefault(key, 0L));
      quoted.add(quotedByMonth.getOrDefault(key, 0L));
    }
    return new WorkspaceQuoteStatsDto(months, inquiries, quoted);
  }

  private Map<String, Long> monthCountMap(List<Object[]> rows) {
    Map<String, Long> map = new LinkedHashMap<>();
    for (Object[] row : rows) {
      if (row == null || row.length < 3 || row[0] == null || row[1] == null) {
        continue;
      }
      int year = ((Number) row[0]).intValue();
      int month = ((Number) row[1]).intValue();
      long count = row[2] instanceof Number number ? number.longValue() : 0L;
      map.put(String.format("%04d-%02d", year, month), count);
    }
    return map;
  }

  private List<WorkspaceNoticeDto> buildNotices(SysUser user, LocalDate today) {
    DashboardScopeParams mine = DashboardScopeParams.self(user);
    List<WorkspaceNoticeDto> notices = new ArrayList<>();
    LocalDate deadline = today.plusDays(EXPIRING_DAYS);
    long expiringCount = dashboardQueryRepository.countExpiringSoon(mine, today, deadline);
    if (expiringCount > 0) {
      Map<String, Object> payload = new HashMap<>();
      payload.put("count", expiringCount);
      payload.put("days", EXPIRING_DAYS);
      notices.add(
          new WorkspaceNoticeDto(
              "expire-" + today,
              "QUOTE_EXPIRING",
              QuoteDateTimes.format(LocalDateTime.now()),
              payload));
    }

    for (QuoteOrder order : dashboardQueryRepository.findCostRiskQuotes(mine, 5)) {
      Map<String, Object> payload = new HashMap<>();
      payload.put("quoteId", order.getId());
      payload.put("quoteNo", order.getQuoteNo());
      payload.put("routeSummary", order.getRouteSummary());
      payload.put("reason", order.getCostRiskReason());
      payload.put("costRiskModes", QuoteCostRiskSupport.parseModes(order.getCostRiskReason()));
      notices.add(
          new WorkspaceNoticeDto(
              "cost-risk-" + order.getId(),
              "COST_RISK",
              QuoteDateTimes.format(
                  order.getCostRiskAt() != null
                      ? order.getCostRiskAt()
                      : LocalDateTime.now()),
              payload));
    }

    // 已报价邮件发送失败：仅推送给报价单操作人（创建人）
    for (Object[] row : quoteMailLogRepository.findLatestFailedByOperator(user.getId(), 5)) {
      Long quoteId = ((Number) row[0]).longValue();
      String quoteNo = row[1] == null ? "" : row[1].toString();
      String customerName = row[2] == null ? "" : row[2].toString();
      String toEmail = row[3] == null ? "" : row[3].toString();
      String errorMessage = row[4] == null ? "" : row[4].toString();
      LocalDateTime failedAt = row[5] instanceof LocalDateTime t ? t : LocalDateTime.now();
      Map<String, Object> payload = new HashMap<>();
      payload.put("quoteId", quoteId);
      payload.put("quoteNo", quoteNo);
      payload.put("customerName", customerName);
      payload.put("toEmail", toEmail);
      payload.put("reason", errorMessage);
      notices.add(
          new WorkspaceNoticeDto(
              "mail-failed-" + quoteId,
              "MAIL_FAILED",
              QuoteDateTimes.format(failedAt),
              payload));
    }

    return notices;
  }

  private boolean isSnapshotStale(QuoteCostSnapshot snapshot) {
    QuoteCostType type = snapshot.getCostType();
    Long refId = snapshot.getCostRefId();
    if (refId == null) {
      return false;
    }
    String liveUpdatedAt = resolveLiveUpdatedAt(type, refId);
    Object snapshotUpdatedAt = snapshot.getSnapshotJson().get("updatedAt");
    String snapUpdatedAt = snapshotUpdatedAt == null ? null : snapshotUpdatedAt.toString();
    if (liveUpdatedAt != null && snapUpdatedAt != null) {
      return !Objects.equals(liveUpdatedAt, snapUpdatedAt);
    }
    LocalDateTime liveUpdatedDateTime = resolveLiveUpdatedAtDateTime(type, refId);
    if (snapUpdatedAt == null
        && liveUpdatedDateTime != null
        && snapshot.getCreatedAt() != null
        && liveUpdatedDateTime.isAfter(snapshot.getCreatedAt())) {
      return true;
    }
    String liveVersion = resolveLiveVersion(type, refId);
    return liveVersion != null
        && snapshot.getCostVersion() != null
        && !Objects.equals(snapshot.getCostVersion(), liveVersion);
  }

  private LocalDateTime resolveLiveUpdatedAtDateTime(QuoteCostType type, Long refId) {
    return switch (type) {
      case ROAD -> costRoadRepository.findById(refId).map(CostRoad::getUpdatedAt).orElse(null);
      case SEA -> costSeaRepository.findById(refId).map(CostSea::getUpdatedAt).orElse(null);
      case FUMIGATION ->
          costFumigationRepository.findById(refId).map(CostFumigation::getUpdatedAt).orElse(null);
    };
  }

  private String resolveLiveUpdatedAt(QuoteCostType type, Long refId) {
    return switch (type) {
      case ROAD ->
          costRoadRepository
              .findById(refId)
              .map(CostRoad::getUpdatedAt)
              .map(QuoteDateTimes::format)
              .orElse(null);
      case SEA ->
          costSeaRepository
              .findById(refId)
              .map(CostSea::getUpdatedAt)
              .map(QuoteDateTimes::format)
              .orElse(null);
      case FUMIGATION ->
          costFumigationRepository
              .findById(refId)
              .map(CostFumigation::getUpdatedAt)
              .map(QuoteDateTimes::format)
              .orElse(null);
    };
  }

  private String resolveLiveVersion(QuoteCostType type, Long refId) {
    return switch (type) {
      case ROAD -> costRoadRepository.findById(refId).map(CostRoad::getValidDate).orElse(null);
      case SEA -> costSeaRepository.findById(refId).map(CostSea::getFreightValidDate).orElse(null);
      case FUMIGATION ->
          costFumigationRepository
              .findById(refId)
              .map(CostFumigation::getUpdatedAt)
              .map(Object::toString)
              .orElse(null);
    };
  }

  private WorkspaceTodoDto toCostRiskTodo(QuoteOrder order) {
    return new WorkspaceTodoDto(
        order.getId(),
        order.getQuoteNo(),
        order.getCustomerName(),
        "reviewCostRisk",
        "urgent",
        formatTimeLabel(order.getCostRiskAt() != null ? order.getCostRiskAt() : order.getUpdatedAt()),
        false);
  }

  private WorkspaceTodoDto toTodo(QuoteOrder order) {
    QuoteStatus status = QuoteStatusSupport.normalize(order.getStatus());
    String todoType =
        switch (status) {
          case PENDING_APPROVAL, PENDING -> "approveQuote";
          default -> "pendingQuote";
        };
    String priority = status == QuoteStatus.PENDING_APPROVAL ? "urgent" : "high";
    return new WorkspaceTodoDto(
        order.getId(),
        order.getQuoteNo(),
        order.getCustomerName(),
        todoType,
        priority,
        formatTimeLabel(order.getUpdatedAt()),
        false);
  }

  private WorkspacePipelineDto toPipeline(QuoteOrder order) {
    QuoteStatus status = QuoteStatusSupport.normalize(order.getStatus());
    int progress =
        switch (status) {
          case DRAFT -> 20;
          case PENDING_APPROVAL -> 45;
          case SENT -> 75;
          case WON -> 100;
          default -> 40;
        };
    String pipelineStatus = status == QuoteStatus.WON ? "done" : "progress";
    String title =
        order.getRouteSummary() != null && !order.getRouteSummary().isBlank()
            ? order.getRouteSummary()
            : order.getQuoteNo();
    List<String> serviceTypes =
        order.getServiceTypes() == null ? List.of() : List.copyOf(order.getServiceTypes());
    BigDecimal amount =
        order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
    String currency =
        order.getCurrency() == null || order.getCurrency().isBlank()
            ? "USD"
            : order.getCurrency();
    String operatorName =
        order.getCreatedByName() == null || order.getCreatedByName().isBlank()
            ? ""
            : order.getCreatedByName().trim();
    return new WorkspacePipelineDto(
        order.getId(),
        order.getQuoteNo(),
        operatorName,
        order.getCustomerName() == null ? "" : order.getCustomerName(),
        serviceTypes,
        amount,
        currency,
        title,
        progress,
        pipelineStatus);
  }

  private String formatTimeLabel(LocalDateTime value) {
    if (value == null) {
      return "";
    }
    if (value.toLocalDate().equals(LocalDate.now())) {
      return value.toLocalTime().withNano(0).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }
    if (value.toLocalDate().equals(LocalDate.now().minusDays(1))) {
      return "昨天";
    }
    return QuoteDateTimes.format(value);
  }

  private NotificationItemDto toNotificationItem(WorkspaceNoticeDto notice, boolean isRead) {
    String link = null;
    Map<String, Object> payload = notice.payload() == null ? Map.of() : notice.payload();
    if ("QUOTE_EXPIRING".equals(notice.type())) {
      link = "/quotes/list";
    } else if (("COST_UPDATED".equals(notice.type()) || "COST_RISK".equals(notice.type()))
        && payload.get("quoteId") != null) {
      link = "/quotes/" + payload.get("quoteId") + "/edit";
    }
    return new NotificationItemDto(
        notice.id(),
        notice.type(),
        null,
        null,
        notice.time(),
        isRead,
        link,
        payload);
  }

  private record StaleQuoteNotice(
      Long quoteId, String quoteNo, String routeSummary, String costType) {}
}
