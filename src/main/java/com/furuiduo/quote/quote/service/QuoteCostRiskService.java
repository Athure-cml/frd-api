package com.furuiduo.quote.quote.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.cost.entity.CostHighlightMode;
import com.furuiduo.quote.quote.entity.QuoteCostSnapshot;
import com.furuiduo.quote.quote.entity.QuoteCostType;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.repository.QuoteCostSnapshotRepository;
import com.furuiduo.quote.quote.repository.QuoteOrderRepository;
import com.furuiduo.quote.quote.support.QuoteCostRiskSupport;
import com.furuiduo.quote.quote.support.QuoteLibraryModeSupport;
import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class QuoteCostRiskService {

  private final QuoteCostSnapshotRepository quoteCostSnapshotRepository;
  private final QuoteOrderRepository quoteOrderRepository;

  public QuoteCostRiskService(
      QuoteCostSnapshotRepository quoteCostSnapshotRepository,
      QuoteOrderRepository quoteOrderRepository) {
    this.quoteCostSnapshotRepository = quoteCostSnapshotRepository;
    this.quoteOrderRepository = quoteOrderRepository;
  }

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
    Set<Long> quoteIds = new HashSet<>();
    for (QuoteCostSnapshot snapshot : snapshots) {
      if (snapshot.getQuoteOrder() != null && snapshot.getQuoteOrder().getId() != null) {
        quoteIds.add(snapshot.getQuoteOrder().getId());
      }
    }
    LocalDateTime now = LocalDateTime.now();
    String modeToken = mode.name().toLowerCase();
    for (Long quoteId : quoteIds) {
      quoteOrderRepository
          .findById(quoteId)
          .ifPresent(
              order -> {
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
