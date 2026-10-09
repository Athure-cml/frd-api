package com.furuiduo.quote.dashboard.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.entity.QuoteStatus;
import com.furuiduo.quote.quote.repository.QuoteApprovalLogRepository;
import com.furuiduo.quote.quote.repository.QuoteCostSnapshotRepository;
import com.furuiduo.quote.quote.support.QuoteApprovalSupport;
import com.furuiduo.quote.quote.support.QuoteCostRiskSupport;
import com.furuiduo.quote.quote.support.QuoteDateTimes;
import com.furuiduo.quote.quote.support.QuoteStatusSupport;
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
  private final PermissionService permissionService;
  private final ApprovalConfigService approvalConfigService;
  private final QuoteApprovalLogRepository quoteApprovalLogRepository;
  private final SysUserNotificationStateRepository notificationStateRepository;

  public DashboardService(
      DashboardQueryRepository dashboardQueryRepository,
      QuoteCostSnapshotRepository quoteCostSnapshotRepository,
      CostRoadRepository costRoadRepository,
      CostSeaRepository costSeaRepository,
      CostFumigationRepository costFumigationRepository,
      PermissionService permissionService,
      ApprovalConfigService approvalConfigService,
      QuoteApprovalLogRepository quoteApprovalLogRepository,
      SysUserNotificationStateRepository notificationStateRepository) {
    this.dashboardQueryRepository = dashboardQueryRepository;
    this.quoteCostSnapshotRepository = quoteCostSnapshotRepository;
    this.costRoadRepository = costRoadRepository;
    this.costSeaRepository = costSeaRepository;
    this.costFumigationRepository = costFumigationRepository;
    this.permissionService = permissionService;
    this.approvalConfigService = approvalConfigService;
    this.quoteApprovalLogRepository = quoteApprovalLogRepository;
    this.notificationStateRepository = notificationStateRepository;
  }

  public WorkspaceResponse getWorkspace(SysUser user) {
    DashboardScopeParams scope = DashboardScopeParams.from(user, permissionService);
    LocalDate today = LocalDate.now();
    LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
    LocalDateTime tomorrow = today.plusDays(1).atStartOfDay();
    LocalDateTime todayStart = today.atStartOfDay();
    LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();

    List<WorkspaceMetricDto> metrics = buildMetrics(scope, monthStart, tomorrow, todayStart, yesterdayStart, today);
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
      LocalDateTime yesterdayStart,
      LocalDate today) {
    long monthQuotes = dashboardQueryRepository.countCreatedBetween(scope, monthStart, tomorrow);
    long monthQuotesTrend =
        dashboardQueryRepository.countCreatedBetween(scope, todayStart, tomorrow)
            - dashboardQueryRepository.countCreatedBetween(scope, yesterdayStart, todayStart);

    BigDecimal monthAmountRaw =
        dashboardQueryRepository.sumAmountCreatedBetween(scope, monthStart, tomorrow);
    long monthAmount =
        monthAmountRaw.divide(BigDecimal.valueOf(10_000), 0, RoundingMode.HALF_UP).longValue();
    BigDecimal monthAmountToday =
        dashboardQueryRepository.sumAmountCreatedBetween(scope, todayStart, tomorrow);
    BigDecimal monthAmountYesterday =
        dashboardQueryRepository.sumAmountCreatedBetween(scope, yesterdayStart, todayStart);
    long monthAmountTrend =
        monthAmountToday
            .divide(BigDecimal.valueOf(10_000), 0, RoundingMode.HALF_UP)
            .subtract(
                monthAmountYesterday.divide(BigDecimal.valueOf(10_000), 0, RoundingMode.HALF_UP))
            .longValue();

    long wonMonth = dashboardQueryRepository.countWonBetween(scope, monthStart, tomorrow);
    long closedMonth = dashboardQueryRepository.countClosedBetween(scope, monthStart, tomorrow);
    long winRate =
        closedMonth == 0 ? 0 : Math.round(wonMonth * 100.0 / closedMonth);

    long wonToday = dashboardQueryRepository.countWonBetween(scope, todayStart, tomorrow);
    long closedToday = dashboardQueryRepository.countClosedBetween(scope, todayStart, tomorrow);
    long wonYesterday = dashboardQueryRepository.countWonBetween(scope, yesterdayStart, todayStart);
    long closedYesterday =
        dashboardQueryRepository.countClosedBetween(scope, yesterdayStart, todayStart);
    long winRateToday = closedToday == 0 ? 0 : Math.round(wonToday * 100.0 / closedToday);
    long winRateYesterday =
        closedYesterday == 0 ? 0 : Math.round(wonYesterday * 100.0 / closedYesterday);
    long winRateTrend = winRateToday - winRateYesterday;

    long followUp =
        dashboardQueryRepository.countByStatuses(
            scope, List.of("PENDING_APPROVAL", "SENT", "PENDING", "EFFECTIVE", "FOLLOWING"));
    long followUpTrend =
        countFollowUpUpdatedBetween(scope, todayStart, tomorrow)
            - countFollowUpUpdatedBetween(scope, yesterdayStart, todayStart);

    LocalDate deadline = today.plusDays(EXPIRING_DAYS);
    long expiringSoon = dashboardQueryRepository.countExpiringSoon(scope, today, deadline);
    long expiringYesterday =
        dashboardQueryRepository.countExpiringSoon(
            scope, today.minusDays(1), today.minusDays(1).plusDays(EXPIRING_DAYS));
    long expiringTrend = expiringSoon - expiringYesterday;

    return List.of(
        new WorkspaceMetricDto("monthQuotes", monthQuotes, monthQuotesTrend),
        new WorkspaceMetricDto("monthAmount", monthAmount, monthAmountTrend),
        new WorkspaceMetricDto("winRate", winRate, winRateTrend),
        new WorkspaceMetricDto("followUp", followUp, followUpTrend),
        new WorkspaceMetricDto("expiringSoon", expiringSoon, expiringTrend));
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
    for (QuoteOrder order : dashboardQueryRepository.findCostRiskQuotes(mine, capped)) {
      todos.add(toCostRiskTodo(order));
    }
    int remaining = Math.max(0, capped - todos.size());
    if (remaining > 0) {
      int candidateLimit = Math.min(200, Math.max(20, capped * 4));
      List<QuoteOrder> candidates =
          dashboardQueryRepository.findRecentActionable(scope, candidateLimit);
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

  private boolean includeTodo(
      QuoteOrder order,
      SysUser user,
      Map<Long, List<QuoteApprovalLog>> logsById,
      List<QuoteApprovalSupport.FlowNode> flow) {
    QuoteStatus status = QuoteStatusSupport.normalize(order.getStatus());
    if (status == QuoteStatus.PENDING_APPROVAL) {
      List<QuoteApprovalLog> logs = logsById.getOrDefault(order.getId(), List.of());
      return QuoteApprovalSupport.isCurrentApprover(user.getId(), logs, flow);
    }
    return user.getId() != null && user.getId().equals(order.getCreatedBy());
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
    Map<String, Double> quotedByMonth =
        monthAmountMap(dashboardQueryRepository.sumAmountByCreatedMonth(scope, from, to));
    Map<String, Double> wonByMonth =
        monthAmountMap(dashboardQueryRepository.sumWonAmountByUpdatedMonth(scope, from, to));
    List<String> months = new ArrayList<>();
    List<Double> quoted = new ArrayList<>();
    List<Double> won = new ArrayList<>();
    for (int i = 0; i < 12; i++) {
      LocalDate month = startMonth.plusMonths(i);
      String key = String.format("%04d-%02d", month.getYear(), month.getMonthValue());
      months.add(key);
      quoted.add(quotedByMonth.getOrDefault(key, 0D));
      won.add(wonByMonth.getOrDefault(key, 0D));
    }
    return new WorkspaceQuoteStatsDto(months, quoted, won);
  }

  private Map<String, Double> monthAmountMap(List<Object[]> rows) {
    Map<String, Double> map = new LinkedHashMap<>();
    for (Object[] row : rows) {
      if (row == null || row.length < 3 || row[0] == null || row[1] == null) {
        continue;
      }
      int year = ((Number) row[0]).intValue();
      int month = ((Number) row[1]).intValue();
      map.put(String.format("%04d-%02d", year, month), toWan(row[2]));
    }
    return map;
  }

  private double toWan(Object raw) {
    BigDecimal amount = BigDecimal.ZERO;
    if (raw instanceof BigDecimal decimal) {
      amount = decimal;
    } else if (raw instanceof Number number) {
      amount = BigDecimal.valueOf(number.doubleValue());
    }
    return amount.divide(BigDecimal.valueOf(10_000), 2, RoundingMode.HALF_UP).doubleValue();
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
          case DRAFT -> "completeDraft";
          case PENDING_APPROVAL -> "approveQuote";
          case WON -> "confirmWon";
          case REJECTED, EXPIRED, VOIDED -> "archiveLost";
          default -> "followSent";
        };
    String priority =
        switch (status) {
          case DRAFT -> "medium";
          case PENDING_APPROVAL -> "urgent";
          case SENT -> "high";
          case WON -> "high";
          default -> "medium";
        };
    boolean done =
        status == QuoteStatus.WON || QuoteStatusSupport.isAbandoned(status);
    return new WorkspaceTodoDto(
        order.getId(),
        order.getQuoteNo(),
        order.getCustomerName(),
        todoType,
        priority,
        formatTimeLabel(order.getUpdatedAt()),
        done);
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
    return new WorkspacePipelineDto(
        order.getId(),
        order.getQuoteNo(),
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

  private long countFollowUpUpdatedBetween(
      DashboardScopeParams scope, LocalDateTime from, LocalDateTime to) {
    return dashboardQueryRepository.countByStatusesUpdatedBetween(
        scope, List.of("PENDING_APPROVAL", "SENT", "PENDING", "EFFECTIVE", "FOLLOWING"), from, to);
  }
}
