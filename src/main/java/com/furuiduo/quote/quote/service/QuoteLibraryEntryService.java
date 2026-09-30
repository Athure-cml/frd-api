package com.furuiduo.quote.quote.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.cost.dto.FreightCostResponse;
import com.furuiduo.quote.cost.dto.FumigationCostResponse;
import com.furuiduo.quote.cost.dto.RoadCostResponse;
import com.furuiduo.quote.cost.entity.CostHighlightMode;
import com.furuiduo.quote.cost.repository.CostFumigationRepository;
import com.furuiduo.quote.cost.repository.CostRoadRepository;
import com.furuiduo.quote.cost.repository.CostSeaRepository;
import com.furuiduo.quote.quote.dto.QuoteLibraryPromoteResult;
import com.furuiduo.quote.quote.entity.QuoteLibraryEntry;
import com.furuiduo.quote.quote.repository.QuoteCostSnapshotRepository;
import com.furuiduo.quote.quote.repository.QuoteLibraryEntryRepository;
import com.furuiduo.quote.quote.support.QuoteLibraryModeSupport;
import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class QuoteLibraryEntryService {

  private final QuoteLibraryEntryRepository entryRepository;
  private final QuoteLibraryOverrideService quoteLibraryOverrideService;
  private final QuoteCostRiskService quoteCostRiskService;
  private final QuoteCostSnapshotRepository quoteCostSnapshotRepository;
  private final CostRoadRepository costRoadRepository;
  private final CostSeaRepository costSeaRepository;
  private final CostFumigationRepository costFumigationRepository;

  public QuoteLibraryEntryService(
      QuoteLibraryEntryRepository entryRepository,
      QuoteLibraryOverrideService quoteLibraryOverrideService,
      QuoteCostRiskService quoteCostRiskService,
      QuoteCostSnapshotRepository quoteCostSnapshotRepository,
      CostRoadRepository costRoadRepository,
      CostSeaRepository costSeaRepository,
      CostFumigationRepository costFumigationRepository) {
    this.entryRepository = entryRepository;
    this.quoteLibraryOverrideService = quoteLibraryOverrideService;
    this.quoteCostRiskService = quoteCostRiskService;
    this.quoteCostSnapshotRepository = quoteCostSnapshotRepository;
    this.costRoadRepository = costRoadRepository;
    this.costSeaRepository = costSeaRepository;
    this.costFumigationRepository = costFumigationRepository;
  }

  public Set<Long> entryCostIds(CostHighlightMode mode) {
    Set<Long> ids = entryRepository.findCostIdsByCostMode(mode);
    return ids == null ? Set.of() : ids;
  }

  public Set<Long> resolveQuoteLibraryRestrictIds(
      CostHighlightMode mode, Set<Long> highlightRestrictIds) {
    Set<Long> entryIds = entryCostIds(mode);
    if (entryIds.isEmpty()) {
      return Set.of();
    }
    if (highlightRestrictIds == null) {
      return entryIds;
    }
    Set<Long> intersection = new HashSet<>(entryIds);
    intersection.retainAll(highlightRestrictIds);
    return intersection;
  }

  /**
   * 成本库列表「是否生成报价」筛选：true=仅已入报价库；false=排除已入报价库；null=不限。
   * 返回值语义与 highlight restrict 相同：null 不限制，空集合表示无匹配。
   */
  public Set<Long> resolveInQuoteLibraryRestrictIds(
      CostHighlightMode mode, Set<Long> highlightRestrictIds, Boolean inQuoteLibrary) {
    if (inQuoteLibrary == null) {
      return highlightRestrictIds;
    }
    if (Boolean.TRUE.equals(inQuoteLibrary)) {
      return resolveQuoteLibraryRestrictIds(mode, highlightRestrictIds);
    }
    Set<Long> entryIds = entryCostIds(mode);
    if (entryIds.isEmpty()) {
      return highlightRestrictIds;
    }
    if (highlightRestrictIds != null) {
      Set<Long> result = new HashSet<>(highlightRestrictIds);
      result.removeAll(entryIds);
      return result;
    }
    Set<Long> allIds = loadAllCostIds(mode);
    allIds.removeAll(entryIds);
    return allIds;
  }

  private Set<Long> loadAllCostIds(CostHighlightMode mode) {
    return switch (mode) {
      case road -> new HashSet<>(costRoadRepository.findAllIds());
      case sea -> new HashSet<>(costSeaRepository.findAllIds());
      case fumigation -> new HashSet<>(costFumigationRepository.findAllIds());
    };
  }

  public Map<Long, Boolean> loadFlags(CostHighlightMode mode, Collection<Long> costIds) {
    if (costIds == null || costIds.isEmpty()) {
      return Map.of();
    }
    Set<Long> inLibrary =
        entryRepository.findByCostModeAndCostIdIn(mode, costIds).stream()
            .map(QuoteLibraryEntry::getCostId)
            .collect(Collectors.toSet());
    return costIds.stream().collect(Collectors.toMap(id -> id, inLibrary::contains));
  }

  /** 被已成交报价单引用的成本 ID（成本库禁止改删） */
  public Set<Long> loadWonLockedCostIds(CostHighlightMode mode, Collection<Long> costIds) {
    if (costIds == null || costIds.isEmpty()) {
      return Set.of();
    }
    List<Long> locked =
        quoteCostSnapshotRepository.findCostRefIdsLockedByWonQuotes(
            QuoteLibraryModeSupport.toCostType(mode), costIds);
    return locked == null || locked.isEmpty() ? Set.of() : new HashSet<>(locked);
  }

  public PageResult<RoadCostResponse> enrichRoadPage(PageResult<RoadCostResponse> page) {
    if (page.items().isEmpty()) {
      return page;
    }
    List<Long> ids = page.items().stream().map(RoadCostResponse::id).toList();
    Map<Long, Boolean> flags = loadFlags(CostHighlightMode.road, ids);
    Set<Long> wonLocked = loadWonLockedCostIds(CostHighlightMode.road, ids);
    return new PageResult<>(
        page.items().stream()
            .map(
                row ->
                    row.withInQuoteLibrary(Boolean.TRUE.equals(flags.get(row.id())))
                        .withQuoteOrderLocked(wonLocked.contains(row.id())))
            .toList(),
        page.total());
  }

  public PageResult<FreightCostResponse> enrichSeaPage(PageResult<FreightCostResponse> page) {
    if (page.items().isEmpty()) {
      return page;
    }
    List<Long> ids = page.items().stream().map(FreightCostResponse::id).toList();
    Map<Long, Boolean> flags = loadFlags(CostHighlightMode.sea, ids);
    Set<Long> wonLocked = loadWonLockedCostIds(CostHighlightMode.sea, ids);
    return new PageResult<>(
        page.items().stream()
            .map(
                row ->
                    row.withInQuoteLibrary(Boolean.TRUE.equals(flags.get(row.id())))
                        .withQuoteOrderLocked(wonLocked.contains(row.id())))
            .toList(),
        page.total());
  }

  public PageResult<FumigationCostResponse> enrichFumigationPage(
      PageResult<FumigationCostResponse> page) {
    if (page.items().isEmpty()) {
      return page;
    }
    List<Long> ids = page.items().stream().map(FumigationCostResponse::id).toList();
    Map<Long, Boolean> flags = loadFlags(CostHighlightMode.fumigation, ids);
    Set<Long> wonLocked = loadWonLockedCostIds(CostHighlightMode.fumigation, ids);
    return new PageResult<>(
        page.items().stream()
            .map(
                row ->
                    row.withInQuoteLibrary(Boolean.TRUE.equals(flags.get(row.id())))
                        .withQuoteOrderLocked(wonLocked.contains(row.id())))
            .toList(),
        page.total());
  }

  /** 已被成交报价单引用的成本不可修改或删除。 */
  public void ensureNotWonLocked(CostHighlightMode mode, Long costId) {
    if (costId == null) {
      return;
    }
    if (quoteCostSnapshotRepository.existsWonLockByCostTypeAndCostRefId(
        QuoteLibraryModeSupport.toCostType(mode), costId)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "该成本已被成交报价单引用，无法修改或删除");
    }
  }

  public void ensureNotWonLocked(CostHighlightMode mode, Collection<Long> costIds) {
    if (costIds == null || costIds.isEmpty()) {
      return;
    }
    Set<Long> locked = loadWonLockedCostIds(mode, costIds);
    if (!locked.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "所选成本中有 " + locked.size() + " 条已被成交报价单引用，无法修改或删除");
    }
  }

  /** 成本已入报价库时不允许删除。 */
  public void ensureEditable(CostHighlightMode mode, Long costId) {
    ensureNotWonLocked(mode, costId);
    if (entryRepository.existsByCostModeAndCostId(mode, costId)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "该成本已入报价库，请先在报价库中删除后再删除成本");
    }
  }

  /** 成本已入报价库时不允许批量删除。 */
  public void ensureEditable(CostHighlightMode mode, Collection<Long> costIds) {
    ensureNotWonLocked(mode, costIds);
    if (costIds == null || costIds.isEmpty()) {
      return;
    }
    List<Long> locked =
        entryRepository.findByCostModeAndCostIdIn(mode, costIds).stream()
            .map(QuoteLibraryEntry::getCostId)
            .toList();
    if (!locked.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "所选成本中有 "
              + locked.size()
              + " 条已入报价库，请先在报价库中删除后再删除成本");
    }
  }

  /**
   * 成本修改后同步报价库：清除该成本在报价库的手动加价覆盖，使展示值按最新成本重新套用报价规则。
   */
  @Transactional
  public void syncAfterCostUpdate(CostHighlightMode mode, Long costId) {
    if (costId == null || !entryRepository.existsByCostModeAndCostId(mode, costId)) {
      return;
    }
    quoteLibraryOverrideService.deleteByCostId(mode, costId);
    quoteCostRiskService.onUnderlyingCostChanged(mode, costId);
  }

  @Transactional
  public void syncAfterCostUpdate(CostHighlightMode mode, Collection<Long> costIds) {
    if (costIds == null || costIds.isEmpty()) {
      return;
    }
    List<Long> inLibrary =
        entryRepository.findByCostModeAndCostIdIn(mode, costIds).stream()
            .map(QuoteLibraryEntry::getCostId)
            .toList();
    if (!inLibrary.isEmpty()) {
      quoteLibraryOverrideService.deleteByCostIds(mode, inLibrary);
      for (Long costId : inLibrary) {
        quoteCostRiskService.onUnderlyingCostChanged(mode, costId);
      }
    }
  }

  public void requireInLibrary(CostHighlightMode mode, Long costId) {
    if (!entryRepository.existsByCostModeAndCostId(mode, costId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "该记录不在报价库中");
    }
  }

  public void requireInLibrary(CostHighlightMode mode, Collection<Long> costIds) {
    if (costIds == null || costIds.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择报价库记录");
    }
    Set<Long> existing =
        entryRepository.findByCostModeAndCostIdIn(mode, costIds).stream()
            .map(QuoteLibraryEntry::getCostId)
            .collect(Collectors.toSet());
    if (existing.size() != new HashSet<>(costIds).size()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "部分记录不在报价库中");
    }
  }

  @Transactional
  public QuoteLibraryPromoteResult promote(CostHighlightMode mode, List<Long> ids, SysUser user) {
    if (ids == null || ids.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择要入库的成本");
    }
    int promoted = 0;
    int skipped = 0;
    int notFound = 0;
    for (Long id : ids) {
      if (id == null) {
        continue;
      }
      if (!costExists(mode, id)) {
        notFound++;
        continue;
      }
      if (entryRepository.existsByCostModeAndCostId(mode, id)) {
        skipped++;
        continue;
      }
      QuoteLibraryEntry entry = new QuoteLibraryEntry();
      entry.setCostMode(mode);
      entry.setCostId(id);
      entry.setPromotedBy(user);
      entryRepository.save(entry);
      promoted++;
    }
    return new QuoteLibraryPromoteResult(promoted, skipped, notFound);
  }

  @Transactional
  public void removeEntries(CostHighlightMode mode, Collection<Long> costIds) {
    if (costIds == null || costIds.isEmpty()) {
      return;
    }
    entryRepository.deleteByCostModeAndCostIdIn(mode, costIds);
  }

  private boolean costExists(CostHighlightMode mode, Long id) {
    return switch (mode) {
      case road -> costRoadRepository.existsById(id);
      case sea -> costSeaRepository.existsById(id);
      case fumigation -> costFumigationRepository.existsById(id);
    };
  }
}
