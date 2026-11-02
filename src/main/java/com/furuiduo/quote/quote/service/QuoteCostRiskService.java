package com.furuiduo.quote.quote.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.cost.dto.FreightCostResponse;
import com.furuiduo.quote.cost.dto.FumigationCostResponse;
import com.furuiduo.quote.cost.dto.RoadCostResponse;
import com.furuiduo.quote.cost.entity.CostHighlightMode;
import com.furuiduo.quote.cost.repository.CostFumigationRepository;
import com.furuiduo.quote.cost.repository.CostRoadRepository;
import com.furuiduo.quote.cost.repository.CostSeaRepository;
import com.furuiduo.quote.quote.entity.QuoteCostSnapshot;
import com.furuiduo.quote.quote.entity.QuoteCostType;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.repository.QuoteCostSnapshotRepository;
import com.furuiduo.quote.quote.repository.QuoteOrderRepository;
import com.furuiduo.quote.quote.support.QuoteCostRiskFeeSupport;
import com.furuiduo.quote.quote.support.QuoteCostRiskSupport;
import com.furuiduo.quote.quote.support.QuoteLibraryModeSupport;
import com.furuiduo.quote.quote.support.QuoteStatusSupport;
import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class QuoteCostRiskService {

  private static final Logger log = LoggerFactory.getLogger(QuoteCostRiskService.class);

  private final QuoteCostSnapshotRepository quoteCostSnapshotRepository;
  private final QuoteOrderRepository quoteOrderRepository;
  private final CostRoadRepository costRoadRepository;
  private final CostSeaRepository costSeaRepository;
  private final CostFumigationRepository costFumigationRepository;
  private final QuoteLibraryApplyService quoteLibraryApplyService;
  private final QuoteLibraryOverrideService quoteLibraryOverrideService;

  public QuoteCostRiskService(
      QuoteCostSnapshotRepository quoteCostSnapshotRepository,
      QuoteOrderRepository quoteOrderRepository,
      CostRoadRepository costRoadRepository,
      CostSeaRepository costSeaRepository,
      CostFumigationRepository costFumigationRepository,
      QuoteLibraryApplyService quoteLibraryApplyService,
      QuoteLibraryOverrideService quoteLibraryOverrideService) {
    this.quoteCostSnapshotRepository = quoteCostSnapshotRepository;
    this.quoteOrderRepository = quoteOrderRepository;
    this.costRoadRepository = costRoadRepository;
    this.costSeaRepository = costSeaRepository;
    this.costFumigationRepository = costFumigationRepository;
    this.quoteLibraryApplyService = quoteLibraryApplyService;
    this.quoteLibraryOverrideService = quoteLibraryOverrideService;
  }

  /**
   * 底层成本变更：仅给有效报价单打「异常」标记。
   *
   * <p>已作废 / 已归档单据失效，不再标异常。
   *
   * <p>快照来自报价库（成本库 + 报价规则加价 + 手动覆盖），因此对照的是当前报价库有效价，
   * 不是裸成本库价格。
   *
   * <p>绝不改写报价单上的成本快照（snapshotJson），快照始终保留引入当时的数据。
   */
  @Transactional
  public void onUnderlyingCostChanged(CostHighlightMode mode, Long costId) {
    if (costId == null) {
      return;
    }
    QuoteCostType costType = QuoteLibraryModeSupport.toCostType(mode);
    List<QuoteCostSnapshot> snapshots =
        quoteCostSnapshotRepository.findByCostTypeAndCostRefId(costType, costId);
    if (snapshots.isEmpty()) {
      return;
    }
    String liveSig = liveQuoteLibraryFeeSignature(costType, costId);
    if (liveSig == null) {
      return;
    }
    Set<Long> quoteIds = new HashSet<>();
    for (QuoteCostSnapshot snapshot : snapshots) {
      // 只读引用，禁止 update snapshotJson
      if (snapshot.getQuoteOrder() == null || snapshot.getQuoteOrder().getId() == null) {
        continue;
      }
      String snapSig =
          QuoteCostRiskFeeSupport.signatureFromSnapshot(
              snapshot.getCostType(), snapshot.getSnapshotJson());
      if (!QuoteCostRiskFeeSupport.changed(snapSig, liveSig)) {
        continue;
      }
      quoteIds.add(snapshot.getQuoteOrder().getId());
    }
    if (quoteIds.isEmpty()) {
      return;
    }
    LocalDateTime now = LocalDateTime.now();
    String modeToken = mode.name().toLowerCase();
    for (Long quoteId : quoteIds) {
      quoteOrderRepository
          .findById(quoteId)
          .ifPresent(
              order -> {
                // 已作废 / 已归档：单据失效，不标异常
                if (QuoteStatusSupport.isAbandoned(order.getStatus())) {
                  return;
                }
                boolean wasActive = Boolean.TRUE.equals(order.getCostRiskActive());
                order.setCostRiskActive(true);
                order.setCostRiskReason(
                    wasActive
                        ? QuoteCostRiskSupport.mergeModes(order.getCostRiskReason(), modeToken)
                        : modeToken);
                order.setCostRiskAt(now);
                order.setCostRiskDismissedAt(null);
                order.setCostRiskDismissedBy(null);
              });
    }
  }

  /**
   * 启动时纠偏：对照快照与当前报价库有效价。
   *
   * <ul>
   *   <li>有效价未变（如仅成本变了但规则加价后报价库价不变，或误用裸成本对比）→ 清除
   *   <li>有效价已变 → 保留历史异常标记
   * </ul>
   */
  @Transactional
  public int reconcileFalsePositiveRisks() {
    List<QuoteOrder> flagged = quoteOrderRepository.findByCostRiskActiveTrueAndDeletedAtIsNull();
    if (flagged.isEmpty()) {
      return 0;
    }
    List<QuoteOrder> toClear = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    for (QuoteOrder order : flagged) {
      // 已作废 / 已归档：残留异常一并清掉，且后续不会再触发
      if (QuoteStatusSupport.isAbandoned(order.getStatus()) || !hasRealFeeDrift(order.getId())) {
        order.setCostRiskActive(false);
        order.setCostRiskReason(null);
        order.setCostRiskAt(null);
        order.setCostRiskDismissedAt(now);
        order.setCostRiskDismissedBy(null);
        toClear.add(order);
      }
    }
    if (!toClear.isEmpty()) {
      quoteOrderRepository.saveAll(toClear);
      log.info("成本异常纠偏：清除 {} 条非报价库有效价变更误标，保留真实异常", toClear.size());
    }
    return toClear.size();
  }

  /**
   * 关联快照关键价与当前报价库有效价不一致 → 视为真实费用异常，应保留。
   *
   * <p>若单据已记录异常模块（road/sea/fumigation），只核对这些模块。
   */
  private boolean hasRealFeeDrift(Long quoteId) {
    QuoteOrder order = quoteOrderRepository.findById(quoteId).orElse(null);
    List<QuoteCostSnapshot> snapshots =
        quoteCostSnapshotRepository.findByQuoteOrderIdOrderByCreatedAtDesc(quoteId);
    if (snapshots.isEmpty()) {
      // 无快照无法证明费用变了，按误标清除
      return false;
    }
    Set<QuoteCostType> focusTypes = resolveRiskFocusTypes(order);
    for (QuoteCostSnapshot snapshot : snapshots) {
      if (snapshot.getCostType() == null || snapshot.getCostRefId() == null) {
        continue;
      }
      if (!focusTypes.isEmpty() && !focusTypes.contains(snapshot.getCostType())) {
        continue;
      }
      String snapSig =
          QuoteCostRiskFeeSupport.signatureFromSnapshot(
              snapshot.getCostType(), snapshot.getSnapshotJson());
      String liveSig = liveQuoteLibraryFeeSignature(snapshot.getCostType(), snapshot.getCostRefId());
      if (liveSig == null) {
        // 成本已删：无法对照，保守保留异常
        return true;
      }
      if (QuoteCostRiskFeeSupport.changed(snapSig, liveSig)) {
        return true;
      }
    }
    // 对应模块报价库有效价一致（或无对应快照）→ 视为误标，可清除
    return false;
  }

  private Set<QuoteCostType> resolveRiskFocusTypes(QuoteOrder order) {
    if (order == null) {
      return Set.of();
    }
    Set<QuoteCostType> types = new HashSet<>();
    for (String mode : QuoteCostRiskSupport.parseModes(order.getCostRiskReason())) {
      if ("road".equalsIgnoreCase(mode)) {
        types.add(QuoteCostType.ROAD);
      } else if ("sea".equalsIgnoreCase(mode)) {
        types.add(QuoteCostType.SEA);
      } else if ("fumigation".equalsIgnoreCase(mode)) {
        types.add(QuoteCostType.FUMIGATION);
      }
    }
    return types;
  }

  /**
   * 当前报价库有效价签名：成本库价 → 报价规则加价 → 手动覆盖。
   *
   * @return null 表示成本记录不存在
   */
  private String liveQuoteLibraryFeeSignature(QuoteCostType type, Long costId) {
    return switch (type) {
      case ROAD -> liveRoadQuoteLibrarySignature(costId);
      case SEA -> liveSeaQuoteLibrarySignature(costId);
      case FUMIGATION -> liveFumigationQuoteLibrarySignature(costId);
    };
  }

  private String liveRoadQuoteLibrarySignature(Long costId) {
    return costRoadRepository
        .findById(costId)
        .map(
            entity -> {
              RoadCostResponse base = RoadCostResponse.from(entity);
              RoadCostResponse ruled = quoteLibraryApplyService.applyRoad(base);
              Map<String, BigDecimal> overrides =
                  quoteLibraryOverrideService
                      .loadOverrides(CostHighlightMode.road, List.of(costId))
                      .getOrDefault(costId, Map.of());
              RoadCostResponse effective =
                  quoteLibraryApplyService.applyRoadOverrides(ruled, overrides);
              return QuoteCostRiskFeeSupport.roadSignature(
                  effective.allInNoFm(), effective.allInFmOneWay(), effective.allInFmRound());
            })
        .orElse(null);
  }

  private String liveSeaQuoteLibrarySignature(Long costId) {
    return costSeaRepository
        .findById(costId)
        .map(
            entity -> {
              FreightCostResponse base = FreightCostResponse.fromSea(entity);
              FreightCostResponse ruled = quoteLibraryApplyService.applySea(base);
              Map<String, BigDecimal> overrides =
                  quoteLibraryOverrideService
                      .loadOverrides(CostHighlightMode.sea, List.of(costId))
                      .getOrDefault(costId, Map.of());
              FreightCostResponse effective =
                  quoteLibraryApplyService.applySeaOverrides(ruled, overrides);
              return QuoteCostRiskFeeSupport.seaSignature(effective.allIn());
            })
        .orElse(null);
  }

  private String liveFumigationQuoteLibrarySignature(Long costId) {
    return costFumigationRepository
        .findById(costId)
        .map(
            entity -> {
              FumigationCostResponse base = FumigationCostResponse.from(entity);
              FumigationCostResponse ruled = quoteLibraryApplyService.applyFumigation(base);
              Map<String, BigDecimal> overrides =
                  quoteLibraryOverrideService
                      .loadOverrides(CostHighlightMode.fumigation, List.of(costId))
                      .getOrDefault(costId, Map.of());
              FumigationCostResponse effective =
                  quoteLibraryApplyService.applyFumigationOverrides(ruled, overrides);
              return QuoteCostRiskFeeSupport.fumigationSignature(
                  effective.outdoorNonOak(),
                  effective.outdoorOak(),
                  effective.indoorNonOak(),
                  effective.indoorOak());
            })
        .orElse(null);
  }

  @Transactional
  public void clearOnSave(SysUser user, QuoteOrder order) {
    clearRisk(user, order);
  }

  /** 作废 / 成交 / 变更 / 保存等操作后去掉成本风险标记 */
  public void clearRisk(SysUser user, QuoteOrder order) {
    if (order == null) {
      return;
    }
    boolean hadRisk =
        Boolean.TRUE.equals(order.getCostRiskActive())
            || (order.getCostRiskReason() != null && !order.getCostRiskReason().isBlank());
    if (!hadRisk) {
      return;
    }
    LocalDateTime now = LocalDateTime.now();
    order.setCostRiskActive(false);
    order.setCostRiskReason(null);
    order.setCostRiskAt(null);
    order.setCostRiskDismissedAt(now);
    order.setCostRiskDismissedBy(user != null ? user.getId() : null);
  }

  @Transactional
  public QuoteOrder dismiss(SysUser user, Long quoteId) {
    QuoteOrder order =
        quoteOrderRepository
            .findById(quoteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "报价单不存在"));
    if (!Boolean.TRUE.equals(order.getCostRiskActive())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "该报价单当前无待复核的成本风险");
    }
    clearRisk(user, order);
    return quoteOrderRepository.save(order);
  }
}
