package com.furuiduo.quote.quote.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.cost.dto.FreightCostResponse;
import com.furuiduo.quote.cost.dto.FumigationCostResponse;
import com.furuiduo.quote.cost.dto.RoadCostResponse;
import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostHighlightMode;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.entity.CostSea;
import com.furuiduo.quote.cost.repository.CostFumigationRepository;
import com.furuiduo.quote.cost.repository.CostRoadRepository;
import com.furuiduo.quote.cost.repository.CostSeaRepository;
import com.furuiduo.quote.quote.dto.QuoteLibraryBatchDeleteRequest;
import com.furuiduo.quote.quote.dto.QuoteLibraryBatchUpdateRequest;
import com.furuiduo.quote.quote.dto.QuoteLibraryBatchUpdateResult;
import com.furuiduo.quote.quote.dto.QuoteLibraryEditContext;
import com.furuiduo.quote.quote.dto.QuoteLibraryPromoteRequest;
import com.furuiduo.quote.quote.dto.QuoteLibraryPromoteResult;
import com.furuiduo.quote.quote.dto.QuoteLibraryUpdateRequest;
import com.furuiduo.quote.quote.support.QuoteLibraryFeeSupport;
import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class QuoteLibraryMutationService {

  private final CostRoadRepository costRoadRepository;
  private final CostSeaRepository costSeaRepository;
  private final CostFumigationRepository costFumigationRepository;
  private final QuoteLibraryApplyService quoteLibraryApplyService;
  private final QuoteLibraryOverrideService quoteLibraryOverrideService;
  private final QuoteLibraryEntryService quoteLibraryEntryService;
  private final QuoteLibraryUsageService quoteLibraryUsageService;

  public QuoteLibraryMutationService(
      CostRoadRepository costRoadRepository,
      CostSeaRepository costSeaRepository,
      CostFumigationRepository costFumigationRepository,
      QuoteLibraryApplyService quoteLibraryApplyService,
      QuoteLibraryOverrideService quoteLibraryOverrideService,
      QuoteLibraryEntryService quoteLibraryEntryService,
      QuoteLibraryUsageService quoteLibraryUsageService) {
    this.costRoadRepository = costRoadRepository;
    this.costSeaRepository = costSeaRepository;
    this.costFumigationRepository = costFumigationRepository;
    this.quoteLibraryApplyService = quoteLibraryApplyService;
    this.quoteLibraryOverrideService = quoteLibraryOverrideService;
    this.quoteLibraryEntryService = quoteLibraryEntryService;
    this.quoteLibraryUsageService = quoteLibraryUsageService;
  }

  @Transactional
  public QuoteLibraryPromoteResult promoteRoad(QuoteLibraryPromoteRequest request, SysUser user) {
    return quoteLibraryEntryService.promote(CostHighlightMode.road, request.ids(), user);
  }

  @Transactional
  public QuoteLibraryPromoteResult promoteSea(QuoteLibraryPromoteRequest request, SysUser user) {
    return quoteLibraryEntryService.promote(CostHighlightMode.sea, request.ids(), user);
  }

  @Transactional
  public QuoteLibraryPromoteResult promoteFumigation(
      QuoteLibraryPromoteRequest request, SysUser user) {
    return quoteLibraryEntryService.promote(CostHighlightMode.fumigation, request.ids(), user);
  }

  public QuoteLibraryEditContext getEditContext(CostHighlightMode mode, Long id) {
    quoteLibraryEntryService.requireInLibrary(mode, id);
    return switch (mode) {
      case road -> getRoadEditContext(id);
      case sea -> getSeaEditContext(id);
      case fumigation -> getFumigationEditContext(id);
    };
  }

  @Transactional
  public QuoteLibraryEditContext resetRoad(Long id) {
    quoteLibraryUsageService.ensureEditable(CostHighlightMode.road, id);
    return reset(CostHighlightMode.road, id);
  }

  @Transactional
  public QuoteLibraryEditContext resetSea(Long id) {
    quoteLibraryUsageService.ensureEditable(CostHighlightMode.sea, id);
    return reset(CostHighlightMode.sea, id);
  }

  @Transactional
  public QuoteLibraryEditContext resetFumigation(Long id) {
    quoteLibraryUsageService.ensureEditable(CostHighlightMode.fumigation, id);
    return reset(CostHighlightMode.fumigation, id);
  }

  @Transactional
  public RoadCostResponse updateRoad(Long id, QuoteLibraryUpdateRequest request, SysUser user) {
    quoteLibraryEntryService.requireInLibrary(CostHighlightMode.road, id);
    quoteLibraryUsageService.ensureEditable(CostHighlightMode.road, id);
    CostRoad entity = requireRoad(id);
    Map<String, BigDecimal> fields =
        QuoteLibraryFeeSupport.normalizeFields(CostHighlightMode.road, request.fields());
    if (fields.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写要修改的费用");
    }
    QuoteLibraryFeeSupport.validateFloors(QuoteLibraryFeeSupport.roadCostFloors(entity), fields);
    quoteLibraryOverrideService.mergeOverride(CostHighlightMode.road, id, fields, user);
    return loadRoadRow(id);
  }

  @Transactional
  public FreightCostResponse updateSea(Long id, QuoteLibraryUpdateRequest request, SysUser user) {
    quoteLibraryEntryService.requireInLibrary(CostHighlightMode.sea, id);
    quoteLibraryUsageService.ensureEditable(CostHighlightMode.sea, id);
    CostSea entity = requireSea(id);
    Map<String, BigDecimal> fields =
        syncSeaFeeFields(
            QuoteLibraryFeeSupport.normalizeFields(CostHighlightMode.sea, request.fields()));
    if (fields.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写要修改的费用");
    }
    QuoteLibraryFeeSupport.validateFloors(QuoteLibraryFeeSupport.seaCostFloors(entity), fields);
    quoteLibraryOverrideService.mergeOverride(CostHighlightMode.sea, id, fields, user);
    return loadSeaRow(id);
  }

  @Transactional
  public FumigationCostResponse updateFumigation(
      Long id, QuoteLibraryUpdateRequest request, SysUser user) {
    quoteLibraryEntryService.requireInLibrary(CostHighlightMode.fumigation, id);
    quoteLibraryUsageService.ensureEditable(CostHighlightMode.fumigation, id);
    CostFumigation entity = requireFumigation(id);
    Map<String, BigDecimal> fields =
        QuoteLibraryFeeSupport.normalizeFields(CostHighlightMode.fumigation, request.fields());
    if (fields.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写要修改的费用");
    }
    QuoteLibraryFeeSupport.validateFloors(
        QuoteLibraryFeeSupport.fumigationCostFloors(entity), fields);
    quoteLibraryOverrideService.mergeOverride(CostHighlightMode.fumigation, id, fields, user);
    return loadFumigationRow(id);
  }

  @Transactional
  public QuoteLibraryBatchUpdateResult batchUpdateRoad(
      QuoteLibraryBatchUpdateRequest request, SysUser user) {
    return batchUpdate(CostHighlightMode.road, request, user);
  }

  @Transactional
  public QuoteLibraryBatchUpdateResult batchUpdateSea(
      QuoteLibraryBatchUpdateRequest request, SysUser user) {
    return batchUpdate(CostHighlightMode.sea, request, user);
  }

  @Transactional
  public QuoteLibraryBatchUpdateResult batchUpdateFumigation(
      QuoteLibraryBatchUpdateRequest request, SysUser user) {
    return batchUpdate(CostHighlightMode.fumigation, request, user);
  }

  @Transactional
  public void deleteRoad(Long id) {
    quoteLibraryEntryService.requireInLibrary(CostHighlightMode.road, id);
    quoteLibraryUsageService.ensureEditable(CostHighlightMode.road, id);
    quoteLibraryEntryService.removeEntries(CostHighlightMode.road, List.of(id));
    quoteLibraryOverrideService.deleteByCostId(CostHighlightMode.road, id);
  }

  @Transactional
  public void deleteSea(Long id) {
    quoteLibraryEntryService.requireInLibrary(CostHighlightMode.sea, id);
    quoteLibraryUsageService.ensureEditable(CostHighlightMode.sea, id);
    quoteLibraryEntryService.removeEntries(CostHighlightMode.sea, List.of(id));
    quoteLibraryOverrideService.deleteByCostId(CostHighlightMode.sea, id);
  }

  @Transactional
  public void deleteFumigation(Long id) {
    quoteLibraryEntryService.requireInLibrary(CostHighlightMode.fumigation, id);
    quoteLibraryUsageService.ensureEditable(CostHighlightMode.fumigation, id);
    quoteLibraryEntryService.removeEntries(CostHighlightMode.fumigation, List.of(id));
    quoteLibraryOverrideService.deleteByCostId(CostHighlightMode.fumigation, id);
  }

  @Transactional
  public void batchDeleteRoad(QuoteLibraryBatchDeleteRequest request) {
    batchDelete(CostHighlightMode.road, request);
  }

  @Transactional
  public void batchDeleteSea(QuoteLibraryBatchDeleteRequest request) {
    batchDelete(CostHighlightMode.sea, request);
  }

  @Transactional
  public void batchDeleteFumigation(QuoteLibraryBatchDeleteRequest request) {
    batchDelete(CostHighlightMode.fumigation, request);
  }

  private QuoteLibraryEditContext reset(CostHighlightMode mode, Long id) {
    quoteLibraryEntryService.requireInLibrary(mode, id);
    quoteLibraryOverrideService.deleteByCostId(mode, id);
    return getEditContext(mode, id);
  }

  private QuoteLibraryEditContext getRoadEditContext(Long id) {
    RoadCostResponse ruled = loadRuledRoadRow(id);
    RoadCostResponse row = loadRoadRow(id);
    CostRoad entity = requireRoad(id);
    Map<String, BigDecimal> defaultValues = QuoteLibraryFeeSupport.currentRoadValues(ruled);
    return new QuoteLibraryEditContext(
        id,
        QuoteLibraryFeeSupport.currentRoadValues(row),
        defaultValues,
        QuoteLibraryFeeSupport.roadCostFloors(entity),
        QuoteLibraryFeeSupport.editableFields(CostHighlightMode.road));
  }

  private QuoteLibraryEditContext getSeaEditContext(Long id) {
    FreightCostResponse ruled = loadRuledSeaRow(id);
    FreightCostResponse row = loadSeaRow(id);
    CostSea entity = requireSea(id);
    Map<String, BigDecimal> defaultValues = QuoteLibraryFeeSupport.currentSeaValues(ruled);
    return new QuoteLibraryEditContext(
        id,
        QuoteLibraryFeeSupport.currentSeaValues(row),
        defaultValues,
        QuoteLibraryFeeSupport.seaCostFloors(entity),
        QuoteLibraryFeeSupport.editableFields(CostHighlightMode.sea));
  }

  private QuoteLibraryEditContext getFumigationEditContext(Long id) {
    FumigationCostResponse ruled = loadRuledFumigationRow(id);
    FumigationCostResponse row = loadFumigationRow(id);
    CostFumigation entity = requireFumigation(id);
    Map<String, BigDecimal> defaultValues =
        QuoteLibraryFeeSupport.currentFumigationValues(ruled);
    return new QuoteLibraryEditContext(
        id,
        QuoteLibraryFeeSupport.currentFumigationValues(row),
        defaultValues,
        QuoteLibraryFeeSupport.fumigationCostFloors(entity),
        QuoteLibraryFeeSupport.editableFields(CostHighlightMode.fumigation));
  }

  private RoadCostResponse loadRuledRoadRow(Long id) {
    CostRoad entity = requireRoad(id);
    RoadCostResponse raw = RoadCostResponse.from(entity).withInQuoteLibrary(true);
    return quoteLibraryApplyService.applyRoad(raw);
  }

  private FreightCostResponse loadRuledSeaRow(Long id) {
    CostSea entity = requireSea(id);
    FreightCostResponse raw = FreightCostResponse.fromSea(entity).withInQuoteLibrary(true);
    return quoteLibraryApplyService.applySea(raw);
  }

  private FumigationCostResponse loadRuledFumigationRow(Long id) {
    CostFumigation entity = requireFumigation(id);
    FumigationCostResponse raw = FumigationCostResponse.from(entity).withInQuoteLibrary(true);
    return quoteLibraryApplyService.applyFumigation(raw);
  }

  private RoadCostResponse loadRoadRow(Long id) {
    CostRoad entity = requireRoad(id);
    RoadCostResponse raw = RoadCostResponse.from(entity).withInQuoteLibrary(true);
    RoadCostResponse ruled = quoteLibraryApplyService.applyRoad(raw);
    Map<String, BigDecimal> overrides =
        quoteLibraryOverrideService
            .loadOverrides(CostHighlightMode.road, List.of(id))
            .getOrDefault(id, Map.of());
    return quoteLibraryApplyService.applyRoadOverrides(ruled, overrides);
  }

  private FreightCostResponse loadSeaRow(Long id) {
    CostSea entity = requireSea(id);
    FreightCostResponse raw = FreightCostResponse.fromSea(entity).withInQuoteLibrary(true);
    FreightCostResponse ruled = quoteLibraryApplyService.applySea(raw);
    Map<String, BigDecimal> overrides =
        quoteLibraryOverrideService
            .loadOverrides(CostHighlightMode.sea, List.of(id))
            .getOrDefault(id, Map.of());
    return quoteLibraryApplyService.applySeaOverrides(ruled, overrides);
  }

  private FumigationCostResponse loadFumigationRow(Long id) {
    CostFumigation entity = requireFumigation(id);
    FumigationCostResponse raw = FumigationCostResponse.from(entity).withInQuoteLibrary(true);
    FumigationCostResponse ruled = quoteLibraryApplyService.applyFumigation(raw);
    Map<String, BigDecimal> overrides =
        quoteLibraryOverrideService
            .loadOverrides(CostHighlightMode.fumigation, List.of(id))
            .getOrDefault(id, Map.of());
    return quoteLibraryApplyService.applyFumigationOverrides(ruled, overrides);
  }

  private QuoteLibraryBatchUpdateResult batchUpdate(
      CostHighlightMode mode, QuoteLibraryBatchUpdateRequest request, SysUser user) {
    List<Long> ids = request.ids();
    if (ids == null || ids.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择要修改的记录");
    }
    quoteLibraryEntryService.requireInLibrary(mode, ids);
    quoteLibraryUsageService.ensureEditable(mode, ids);
    Map<String, BigDecimal> fields = QuoteLibraryFeeSupport.normalizeFields(mode, request.fields());
    if (mode == CostHighlightMode.sea) {
      fields = syncSeaFeeFields(fields);
    }
    if (fields.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写要修改的费用");
    }
    for (Long id : ids) {
      Map<String, BigDecimal> floors =
          switch (mode) {
            case road ->
                QuoteLibraryFeeSupport.roadCostFloors(
                    costRoadRepository
                        .findById(id)
                        .orElseThrow(
                            () ->
                                new ResponseStatusException(
                                    HttpStatus.NOT_FOUND, "记录不存在: " + id)));
            case sea ->
                QuoteLibraryFeeSupport.seaCostFloors(
                    costSeaRepository
                        .findById(id)
                        .orElseThrow(
                            () ->
                                new ResponseStatusException(
                                    HttpStatus.NOT_FOUND, "记录不存在: " + id)));
            case fumigation ->
                QuoteLibraryFeeSupport.fumigationCostFloors(
                    costFumigationRepository
                        .findById(id)
                        .orElseThrow(
                            () ->
                                new ResponseStatusException(
                                    HttpStatus.NOT_FOUND, "记录不存在: " + id)));
          };
      QuoteLibraryFeeSupport.validateFloors(floors, fields);
    }
    int updated =
        quoteLibraryOverrideService.batchMergeOverrides(mode, ids, fields, user);
    return new QuoteLibraryBatchUpdateResult(updated);
  }

  private void batchDelete(CostHighlightMode mode, QuoteLibraryBatchDeleteRequest request) {
    List<Long> ids = request.ids();
    if (ids == null || ids.isEmpty()) {
      return;
    }
    quoteLibraryEntryService.requireInLibrary(mode, ids);
    quoteLibraryUsageService.ensureEditable(mode, ids);
    quoteLibraryEntryService.removeEntries(mode, ids);
    quoteLibraryOverrideService.deleteByCostIds(mode, ids);
  }

  private CostRoad requireRoad(Long id) {
    return costRoadRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在"));
  }

  private CostSea requireSea(Long id) {
    return costSeaRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在"));
  }

  private CostFumigation requireFumigation(Long id) {
    return costFumigationRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在"));
  }

  private static Map<String, BigDecimal> syncSeaFeeFields(Map<String, BigDecimal> fields) {
    if (fields.containsKey("allIn") && !fields.containsKey("freight")) {
      fields.put("freight", fields.get("allIn"));
    } else if (fields.containsKey("freight") && !fields.containsKey("allIn")) {
      fields.put("allIn", fields.get("freight"));
    }
    return fields;
  }
}
