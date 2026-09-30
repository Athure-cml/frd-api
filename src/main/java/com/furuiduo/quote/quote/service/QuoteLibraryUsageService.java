package com.furuiduo.quote.quote.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.cost.entity.CostHighlightMode;
import com.furuiduo.quote.quote.dto.QuoteCostMatchItemDto;
import com.furuiduo.quote.quote.entity.QuoteCostType;
import com.furuiduo.quote.quote.entity.QuoteLibraryUsage;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.repository.QuoteLibraryUsageRepository;
import com.furuiduo.quote.quote.support.QuoteLibraryModeSupport;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class QuoteLibraryUsageService {

  private final QuoteLibraryUsageRepository usageRepository;

  @PersistenceContext private EntityManager entityManager;

  public QuoteLibraryUsageService(QuoteLibraryUsageRepository usageRepository) {
    this.usageRepository = usageRepository;
  }

  public boolean isLocked(CostHighlightMode mode, Long costId) {
    return costId != null && usageRepository.existsByCostModeAndCostId(mode, costId);
  }

  public Set<Long> loadLockedIds(CostHighlightMode mode, Collection<Long> costIds) {
    if (costIds == null || costIds.isEmpty()) {
      return Set.of();
    }
    Set<Long> locked = new HashSet<>();
    usageRepository
        .findByCostModeAndCostIdIn(mode, costIds)
        .forEach(usage -> locked.add(usage.getCostId()));
    return locked;
  }

  public void ensureEditable(CostHighlightMode mode, Long costId) {
    if (isLocked(mode, costId)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "该报价库记录已被报价单引用，无法修改或删除");
    }
  }

  public void ensureEditable(CostHighlightMode mode, List<Long> costIds) {
    if (costIds == null || costIds.isEmpty()) {
      return;
    }
    long locked = usageRepository.countByCostModeAndCostIdIn(mode, costIds);
    if (locked > 0) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "所选报价库记录中有 " + locked + " 条已被报价单引用，无法修改或删除");
    }
  }

  @Transactional
  public void replaceUsage(QuoteOrder order, List<QuoteCostMatchItemDto> matches) {
    Long quoteId = order.getId();
    Map<QuoteCostType, QuoteCostMatchItemDto> latestByType = buildLatestByType(matches);
    if (latestByType.isEmpty()) {
      usageRepository.deleteByQuoteOrderId(quoteId);
      return;
    }

    Set<CostHighlightMode> desiredModes = new HashSet<>();
    for (QuoteCostType type : latestByType.keySet()) {
      desiredModes.add(QuoteLibraryModeSupport.toMode(type));
    }

    Map<CostHighlightMode, QuoteLibraryUsage> existingByMode = new HashMap<>();
    for (QuoteLibraryUsage usage : usageRepository.findByQuoteOrderId(quoteId)) {
      if (!desiredModes.contains(usage.getCostMode())) {
        usageRepository.delete(usage);
        continue;
      }
      existingByMode.putIfAbsent(usage.getCostMode(), usage);
    }
    entityManager.flush();

    LocalDateTime lockedAt = LocalDateTime.now();
    for (Map.Entry<QuoteCostType, QuoteCostMatchItemDto> entry : latestByType.entrySet()) {
      CostHighlightMode mode = QuoteLibraryModeSupport.toMode(entry.getKey());
      Long costId = entry.getValue().costRefId();
      QuoteLibraryUsage usage = existingByMode.get(mode);
      if (usage != null) {
        usage.setCostId(costId);
        usage.setLockedAt(lockedAt);
        continue;
      }
      usage = new QuoteLibraryUsage();
      usage.setQuoteOrder(order);
      usage.setCostMode(mode);
      usage.setCostId(costId);
      usage.setLockedAt(lockedAt);
      usageRepository.save(usage);
    }
  }

  private Map<QuoteCostType, QuoteCostMatchItemDto> buildLatestByType(
      List<QuoteCostMatchItemDto> matches) {
    Map<QuoteCostType, QuoteCostMatchItemDto> latestByType = new HashMap<>();
    if (matches == null || matches.isEmpty()) {
      return latestByType;
    }
    for (QuoteCostMatchItemDto item : matches) {
      if (item.costType() == null || item.costRefId() == null) {
        continue;
      }
      QuoteCostType type = QuoteCostType.valueOf(item.costType().trim().toUpperCase());
      latestByType.putIfAbsent(type, item);
    }
    return latestByType;
  }

  @Transactional
  public void releaseByQuoteId(Long quoteId) {
    usageRepository.deleteByQuoteOrderId(quoteId);
  }
}
