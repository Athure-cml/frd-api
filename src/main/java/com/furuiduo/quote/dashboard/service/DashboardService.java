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

import org.springframework.stereotype.Service;

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
import com.furuiduo.quote.dashboard.dto.WorkspaceResponse;
import com.furuiduo.quote.dashboard.dto.WorkspaceRouteDto;
import com.furuiduo.quote.dashboard.dto.WorkspaceTodoDto;
import com.furuiduo.quote.dashboard.repository.DashboardQueryRepository;
import com.furuiduo.quote.dashboard.support.DashboardScopeParams;
import com.furuiduo.quote.quote.entity.QuoteCostSnapshot;
import com.furuiduo.quote.quote.entity.QuoteCostType;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.entity.QuoteStatus;
import com.furuiduo.quote.quote.repository.QuoteCostSnapshotRepository;
import com.furuiduo.quote.quote.support.QuoteDateTimes;
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

  public DashboardService(
      DashboardQueryRepository dashboardQueryRepository,
      QuoteCostSnapshotRepository quoteCostSnapshotRepository,
      CostRoadRepository costRoadRepository,
      CostSeaRepository costSeaRepository,
      CostFumigationRepository costFumigationRepository,
      PermissionService permissionService) {
    this.dashboardQueryRepository = dashboardQueryRepository;
    this.quoteCostSnapshotRepository = quoteCostSnapshotRepository;
    this.costRoadRepository = costRoadRepository;
    this.costSeaRepository = costSeaRepository;
    this.costFumigationRepository = costFumigationRepository;
    this.permissionService = permissionService;
  }

  public WorkspaceResponse getWorkspace(SysUser user) {
    DashboardScopeParams scope = DashboardScopeParams.from(user, permissionService);
    LocalDate today = LocalDate.now();
    LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
    LocalDateTime tomorrow = today.plusDays(1).atStartOfDay();
    LocalDateTime todayStart = today.atStartOfDay();
    LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();

    List<WorkspaceMetricDto> metrics = buildMetrics(scope, monthStart, tomorrow, todayStart, yesterdayStart, today);
    List<WorkspaceTodoDto> todos = buildTodos(scope);
    List<WorkspacePipelineDto> pipeline = buildPipeline(scope);
    List<WorkspaceNoticeDto> notices = buildNotices(scope, today);
    List<WorkspaceRouteDto> topRoutes = buildTopRoutes(scope);

    return new WorkspaceResponse(metrics, todos, pipeline, notices, topRoutes);
  }

  public List<NotificationItemDto> getNotifications(SysUser user) {
    DashboardScopeParams scope = DashboardScopeParams.from(user, permissionService);
    LocalDate today = LocalDate.now();
    List<WorkspaceNoticeDto> notices = buildNotices(scope, today);
    List<NotificationItemDto> items = new ArrayList<>();
    for (WorkspaceNoticeDto notice : notices) {
      items.add(toNotificationItem(notice));
    }
    return items;
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
            scope, List.of("FOLLOWING", "EFFECTIVE", "PENDING", "SENT"));
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

  private List<WorkspaceTodoDto> buildTodos(DashboardScopeParams scope) {
    return dashboardQueryRepository.findRecentActionable(scope, 6).stream()
        .map(this::toTodo)
        .toList();
  }

  private List<WorkspacePipelineDto> buildPipeline(DashboardScopeParams scope) {
    return dashboardQueryRepository.findRecentActionable(scope, 4).stream()
        .map(this::toPipeline)
        .toList();
  }

  private List<WorkspaceRouteDto> buildTopRoutes(DashboardScopeParams scope) {
    LocalDateTime since = LocalDate.now().minusDays(30).atStartOfDay();
    return dashboardQueryRepository.findTopRoutes(scope, since, 4).stream()
        .map(
            row ->
                new WorkspaceRouteDto(
                    row[0] == null ? "" : row[0].toString(),
                    row[1] == null ? 0 : ((Number) row[1]).longValue()))
        .toList();
  }

  private List<WorkspaceNoticeDto> buildNotices(DashboardScopeParams scope, LocalDate today) {
    List<WorkspaceNoticeDto> notices = new ArrayList<>();
    LocalDate deadline = today.plusDays(EXPIRING_DAYS);
    long expiringCount = dashboardQueryRepository.countExpiringSoon(scope, today, deadline);
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

    Map<Long, StaleQuoteNotice> staleByQuote = new LinkedHashMap<>();
    for (QuoteOrder order : dashboardQueryRepository.findDraftsWithSnapshots(scope, 30)) {
      List<QuoteCostSnapshot> snapshots =
          quoteCostSnapshotRepository.findByQuoteOrderIdOrderByCreatedAtDesc(order.getId());
      for (QuoteCostSnapshot snapshot : snapshots) {
        if (isSnapshotStale(snapshot)) {
          staleByQuote.putIfAbsent(
              order.getId(),
              new StaleQuoteNotice(
                  order.getId(),
                  order.getQuoteNo(),
                  order.getRouteSummary(),
                  snapshot.getCostType().name()));
          break;
        }
      }
      if (staleByQuote.size() >= 5) {
        break;
      }
    }

    for (StaleQuoteNotice stale : staleByQuote.values()) {
      Map<String, Object> payload = new HashMap<>();
      payload.put("quoteId", stale.quoteId());
      payload.put("quoteNo", stale.quoteNo());
      payload.put("routeSummary", stale.routeSummary());
      payload.put("costType", stale.costType());
      notices.add(
          new WorkspaceNoticeDto(
              "cost-stale-" + stale.quoteId(),
              "COST_UPDATED",
              QuoteDateTimes.format(LocalDateTime.now()),
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
      case SEA -> costSeaRepository.findById(refId).map(CostSea::getValidDate).orElse(null);
      case FUMIGATION ->
          costFumigationRepository
              .findById(refId)
              .map(CostFumigation::getUpdatedAt)
              .map(Object::toString)
              .orElse(null);
    };
  }

  private WorkspaceTodoDto toTodo(QuoteOrder order) {
    QuoteStatus status = order.getStatus();
    String todoType =
        switch (status) {
          case DRAFT -> "completeDraft";
          case WON -> "confirmWon";
          case LOST, EXPIRED, VOIDED -> "archiveLost";
          default -> "followSent";
        };
    String priority =
        switch (status) {
          case DRAFT -> "medium";
          case FOLLOWING, PENDING -> "urgent";
          case EFFECTIVE, SENT -> "high";
          case WON -> "high";
          default -> "medium";
        };
    boolean done = status == QuoteStatus.WON || status == QuoteStatus.VOIDED;
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
    QuoteStatus status = order.getStatus();
    int progress =
        switch (status) {
          case DRAFT -> 25;
          case FOLLOWING, PENDING -> 50;
          case EFFECTIVE, SENT -> 72;
          case WON -> 100;
          default -> 40;
        };
    String pipelineStatus = status == QuoteStatus.WON ? "done" : "progress";
    String title =
        order.getRouteSummary() != null && !order.getRouteSummary().isBlank()
            ? order.getRouteSummary()
            : order.getQuoteNo();
    return new WorkspacePipelineDto(
        order.getId(), order.getQuoteNo(), title, progress, pipelineStatus);
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

  private NotificationItemDto toNotificationItem(WorkspaceNoticeDto notice) {
    String link = null;
    Map<String, Object> payload = notice.payload() == null ? Map.of() : notice.payload();
    if ("QUOTE_EXPIRING".equals(notice.type())) {
      link = "/quotes/list";
    } else if ("COST_UPDATED".equals(notice.type()) && payload.get("quoteId") != null) {
      link = "/quotes/" + payload.get("quoteId") + "/edit";
    }
    return new NotificationItemDto(
        notice.id(),
        notice.type(),
        null,
        null,
        notice.time(),
        false,
        link,
        payload);
  }

  private record StaleQuoteNotice(
      Long quoteId, String quoteNo, String routeSummary, String costType) {}

  private long countFollowUpUpdatedBetween(
      DashboardScopeParams scope, LocalDateTime from, LocalDateTime to) {
    return dashboardQueryRepository.countByStatusesUpdatedBetween(
        scope, List.of("FOLLOWING", "EFFECTIVE", "PENDING", "SENT"), from, to);
  }
}
