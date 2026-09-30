package com.furuiduo.quote.quote.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.cost.entity.CostHighlightMode;
import com.furuiduo.quote.quote.entity.QuoteLibraryOverride;
import com.furuiduo.quote.quote.repository.QuoteLibraryOverrideRepository;
import com.furuiduo.quote.quote.support.QuoteLibraryFeeSupport;
import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class QuoteLibraryOverrideService {

  private final QuoteLibraryOverrideRepository repository;

  public QuoteLibraryOverrideService(QuoteLibraryOverrideRepository repository) {
    this.repository = repository;
  }

  public Map<Long, Map<String, BigDecimal>> loadOverrides(
      CostHighlightMode mode, Collection<Long> costIds) {
    if (costIds == null || costIds.isEmpty()) {
      return Map.of();
    }
    List<QuoteLibraryOverride> rows =
        repository.findByCostModeAndCostIdIn(mode, costIds);
    Map<Long, Map<String, BigDecimal>> result = new HashMap<>();
    for (QuoteLibraryOverride row : rows) {
      result.put(row.getCostId(), QuoteLibraryFeeSupport.toDecimalMap(row.getFieldOverrides()));
    }
    return result;
  }

  @Transactional
  public void mergeOverride(
      CostHighlightMode mode,
      Long costId,
      Map<String, BigDecimal> fields,
      SysUser user) {
    if (fields == null || fields.isEmpty()) {
      return;
    }
    QuoteLibraryOverride entity =
        repository.findByCostModeAndCostIdIn(mode, List.of(costId)).stream()
            .findFirst()
            .orElseGet(
                () -> {
                  QuoteLibraryOverride created = new QuoteLibraryOverride();
                  created.setCostMode(mode);
                  created.setCostId(costId);
                  return created;
                });
    Map<String, Object> merged = new HashMap<>(entity.getFieldOverrides());
    fields.forEach((key, value) -> merged.put(key, value));
    entity.setFieldOverrides(merged);
    entity.setUpdatedBy(user);
    repository.save(entity);
  }

  @Transactional
  public int batchMergeOverrides(
      CostHighlightMode mode,
      Collection<Long> costIds,
      Map<String, BigDecimal> fields,
      SysUser user) {
    if (costIds == null || costIds.isEmpty() || fields == null || fields.isEmpty()) {
      return 0;
    }
    Map<Long, QuoteLibraryOverride> existing =
        repository.findByCostModeAndCostIdIn(mode, costIds).stream()
            .collect(Collectors.toMap(QuoteLibraryOverride::getCostId, row -> row));
    int updated = 0;
    for (Long costId : costIds) {
      QuoteLibraryOverride entity = existing.get(costId);
      if (entity == null) {
        entity = new QuoteLibraryOverride();
        entity.setCostMode(mode);
        entity.setCostId(costId);
      }
      Map<String, Object> merged = new HashMap<>(entity.getFieldOverrides());
      fields.forEach((key, value) -> merged.put(key, value));
      entity.setFieldOverrides(merged);
      entity.setUpdatedBy(user);
      repository.save(entity);
      updated++;
    }
    return updated;
  }

  @Transactional
  public void deleteByCostId(CostHighlightMode mode, Long costId) {
    repository.deleteByCostModeAndCostId(mode, costId);
  }

  @Transactional
  public void deleteByCostIds(CostHighlightMode mode, Collection<Long> costIds) {
    if (costIds == null || costIds.isEmpty()) {
      return;
    }
    repository.deleteByCostModeAndCostIdIn(mode, costIds);
  }
}
