package com.furuiduo.quote.quote.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

import com.furuiduo.quote.common.PageResult;
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
import com.furuiduo.quote.cost.service.CostDeptHighlightService;
import com.furuiduo.quote.cost.service.CostFumigationService;
import com.furuiduo.quote.cost.service.CostRoadService;
import com.furuiduo.quote.cost.service.CostSeaService;
import com.furuiduo.quote.cost.support.CostHighlightQuerySupport;
import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class QuoteLibraryService {

  private static final int EXPORT_BATCH_SIZE = 200;

  private final CostRoadService costRoadService;
  private final CostSeaService costSeaService;
  private final CostFumigationService costFumigationService;
  private final CostRoadRepository costRoadRepository;
  private final CostSeaRepository costSeaRepository;
  private final CostFumigationRepository costFumigationRepository;
  private final CostDeptHighlightService highlightService;
  private final CostHighlightQuerySupport highlightQuerySupport;
  private final QuoteLibraryApplyService quoteLibraryApplyService;
  private final QuoteLibraryOverrideService quoteLibraryOverrideService;
  private final QuoteLibraryEntryService quoteLibraryEntryService;
  private final QuoteLibraryUsageService quoteLibraryUsageService;

  public QuoteLibraryService(
      CostRoadService costRoadService,
      CostSeaService costSeaService,
      CostFumigationService costFumigationService,
      CostRoadRepository costRoadRepository,
      CostSeaRepository costSeaRepository,
      CostFumigationRepository costFumigationRepository,
      CostDeptHighlightService highlightService,
      CostHighlightQuerySupport highlightQuerySupport,
      QuoteLibraryApplyService quoteLibraryApplyService,
      QuoteLibraryOverrideService quoteLibraryOverrideService,
      QuoteLibraryEntryService quoteLibraryEntryService,
      QuoteLibraryUsageService quoteLibraryUsageService) {
    this.costRoadService = costRoadService;
    this.costSeaService = costSeaService;
    this.costFumigationService = costFumigationService;
    this.costRoadRepository = costRoadRepository;
    this.costSeaRepository = costSeaRepository;
    this.costFumigationRepository = costFumigationRepository;
    this.highlightService = highlightService;
    this.highlightQuerySupport = highlightQuerySupport;
    this.quoteLibraryApplyService = quoteLibraryApplyService;
    this.quoteLibraryOverrideService = quoteLibraryOverrideService;
    this.quoteLibraryEntryService = quoteLibraryEntryService;
    this.quoteLibraryUsageService = quoteLibraryUsageService;
  }

  public PageResult<RoadCostResponse> listRoad(
      SysUser user,
      int page,
      int pageSize,
      String zipCode,
      String city,
      String state,
      String por,
      String pol,
      String supplier,
      BigDecimal redelivery,
      String effectiveDate,
      String validDate,
      String status,
      String sortField,
      String sortOrder,
      Boolean highlightOnly) {
    Set<Long> restrictIds =
        quoteLibraryEntryService.resolveQuoteLibraryRestrictIds(
            CostHighlightMode.road,
            highlightQuerySupport.resolveRestrictIds(CostHighlightMode.road, user, highlightOnly));
    PageResult<RoadCostResponse> result =
        highlightService.enrichRoadPage(
            user,
            costRoadService.list(
                page,
                pageSize,
                zipCode,
                city,
                state,
                por,
                pol,
                supplier,
                redelivery,
                effectiveDate,
                validDate,
                status,
                sortField,
                sortOrder,
                restrictIds));
    return applyRoadPage(result);
  }

  public PageResult<FreightCostResponse> listSea(
      SysUser user,
      int page,
      int pageSize,
      String por,
      String pol,
      String pod,
      String ssl,
      String containerType,
      String agent,
      String freightValidDate,
      String freightEffDate,
      String status,
      String remark,
      String sortField,
      String sortOrder,
      Boolean highlightOnly) {
    Set<Long> restrictIds =
        quoteLibraryEntryService.resolveQuoteLibraryRestrictIds(
            CostHighlightMode.sea,
            highlightQuerySupport.resolveRestrictIds(CostHighlightMode.sea, user, highlightOnly));
    PageResult<FreightCostResponse> result =
        highlightService.enrichSeaPage(
            user,
            costSeaService.list(
                page,
                pageSize,
                por,
                pol,
                pod,
                ssl,
                containerType,
                agent,
                freightValidDate,
                freightEffDate,
                status,
                remark,
                sortField,
                sortOrder,
                restrictIds));
    return applySeaPage(result);
  }

  public PageResult<FumigationCostResponse> listFumigation(
      SysUser user,
      int page,
      int pageSize,
      String region,
      String station,
      String outdoorValidity,
      String indoorValidity,
      String status,
      String sortField,
      String sortOrder,
      Boolean highlightOnly) {
    Set<Long> restrictIds =
        quoteLibraryEntryService.resolveQuoteLibraryRestrictIds(
            CostHighlightMode.fumigation,
            highlightQuerySupport.resolveRestrictIds(
                CostHighlightMode.fumigation, user, highlightOnly));
    PageResult<FumigationCostResponse> result =
        highlightService.enrichFumigationPage(
            user,
            costFumigationService.list(
                page,
                pageSize,
                region,
                station,
                outdoorValidity,
                indoorValidity,
                status,
                sortField,
                sortOrder,
                restrictIds));
    return applyFumigationPage(result);
  }

  public List<RoadCostResponse> listRoadForExport(
      SysUser user,
      String zipCode,
      String city,
      String state,
      String por,
      String pol,
      String supplier,
      BigDecimal redelivery,
      String effectiveDate,
      String validDate,
      String status,
      Boolean highlightOnly,
      List<Long> ids) {
    if (ids != null && !ids.isEmpty()) {
      return listRoadByIds(user, ids);
    }
    return listAllRoadForExport(
        user,
        zipCode,
        city,
        state,
        por,
        pol,
        supplier,
        redelivery,
        effectiveDate,
        validDate,
        status,
        highlightOnly);
  }

  public List<FreightCostResponse> listSeaForExport(
      SysUser user,
      String por,
      String pol,
      String pod,
      String ssl,
      String containerType,
      String agent,
      String freightValidDate,
      String freightEffDate,
      String status,
      String remark,
      Boolean highlightOnly,
      List<Long> ids) {
    if (ids != null && !ids.isEmpty()) {
      return listSeaByIds(user, ids);
    }
    return listAllSeaForExport(
        user,
        por,
        pol,
        pod,
        ssl,
        containerType,
        agent,
        freightValidDate,
        freightEffDate,
        status,
        remark,
        highlightOnly);
  }

  public List<FumigationCostResponse> listFumigationForExport(
      SysUser user,
      String region,
      String station,
      String outdoorValidity,
      String indoorValidity,
      String status,
      Boolean highlightOnly,
      List<Long> ids) {
    if (ids != null && !ids.isEmpty()) {
      return listFumigationByIds(user, ids);
    }
    return listAllFumigationForExport(
        user, region, station, outdoorValidity, indoorValidity, status, highlightOnly);
  }

  public List<RoadCostResponse> applyRoadQuoteRows(List<RoadCostResponse> rows) {
    if (rows == null || rows.isEmpty()) {
      return List.of();
    }
    return applyRoadPage(new PageResult<>(rows, rows.size())).items();
  }

  public List<FreightCostResponse> applySeaQuoteRows(List<FreightCostResponse> rows) {
    if (rows == null || rows.isEmpty()) {
      return List.of();
    }
    return applySeaPage(new PageResult<>(rows, rows.size())).items();
  }

  public List<FumigationCostResponse> applyFumigationQuoteRows(
      List<FumigationCostResponse> rows) {
    if (rows == null || rows.isEmpty()) {
      return List.of();
    }
    return applyFumigationPage(new PageResult<>(rows, rows.size())).items();
  }

  private List<RoadCostResponse> listRoadByIds(SysUser user, List<Long> ids) {
    List<Long> exportIds = filterExportIds(CostHighlightMode.road, user, ids);
    if (exportIds.isEmpty()) {
      return List.of();
    }
    List<RoadCostResponse> rows =
        highlightService
            .enrichRoadPage(
                user,
                new PageResult<>(
                    costRoadRepository.findAllById(exportIds).stream()
                        .sorted(Comparator.comparing(CostRoad::getId))
                        .map(RoadCostResponse::from)
                        .toList(),
                    exportIds.size()))
            .items();
    return applyRoadQuoteRows(rows);
  }

  private List<FreightCostResponse> listSeaByIds(SysUser user, List<Long> ids) {
    List<Long> exportIds = filterExportIds(CostHighlightMode.sea, user, ids);
    if (exportIds.isEmpty()) {
      return List.of();
    }
    List<FreightCostResponse> rows =
        highlightService
            .enrichSeaPage(
                user,
                new PageResult<>(
                    costSeaRepository.findAllById(exportIds).stream()
                        .sorted(Comparator.comparing(CostSea::getId))
                        .map(FreightCostResponse::fromSea)
                        .toList(),
                    exportIds.size()))
            .items();
    return applySeaQuoteRows(rows);
  }

  private List<FumigationCostResponse> listFumigationByIds(SysUser user, List<Long> ids) {
    List<Long> exportIds = filterExportIds(CostHighlightMode.fumigation, user, ids);
    if (exportIds.isEmpty()) {
      return List.of();
    }
    List<FumigationCostResponse> rows =
        highlightService
            .enrichFumigationPage(
                user,
                new PageResult<>(
                    costFumigationRepository.findAllById(exportIds).stream()
                        .sorted(Comparator.comparing(CostFumigation::getId))
                        .map(FumigationCostResponse::from)
                        .toList(),
                    exportIds.size()))
            .items();
    return applyFumigationQuoteRows(rows);
  }

  private List<Long> filterExportIds(CostHighlightMode mode, SysUser user, List<Long> ids) {
    Set<Long> libraryIds = quoteLibraryEntryService.entryCostIds(mode);
    List<Long> exportIds =
        ids.stream().filter(libraryIds::contains).distinct().sorted().toList();
    if (exportIds.isEmpty()) {
      return List.of();
    }
    Set<Long> restrict =
        quoteLibraryEntryService.resolveQuoteLibraryRestrictIds(
            mode, highlightQuerySupport.resolveRestrictIds(mode, user, false));
    if (restrict == null || restrict.isEmpty()) {
      return exportIds;
    }
    return exportIds.stream().filter(restrict::contains).toList();
  }

  private List<RoadCostResponse> listAllRoadForExport(
      SysUser user,
      String zipCode,
      String city,
      String state,
      String por,
      String pol,
      String supplier,
      BigDecimal redelivery,
      String effectiveDate,
      String validDate,
      String status,
      Boolean highlightOnly) {
    java.util.ArrayList<RoadCostResponse> rows = new java.util.ArrayList<>();
    int page = 1;
    long total = Long.MAX_VALUE;
    while (rows.size() < total) {
      PageResult<RoadCostResponse> batch =
          listRoad(
              user,
              page,
              EXPORT_BATCH_SIZE,
              zipCode,
              city,
              state,
              por,
              pol,
              supplier,
              redelivery,
              effectiveDate,
              validDate,
              status,
              null,
              null,
              highlightOnly);
      rows.addAll(batch.items());
      total = batch.total();
      if (batch.items().isEmpty()) {
        break;
      }
      page++;
    }
    return rows;
  }

  private List<FreightCostResponse> listAllSeaForExport(
      SysUser user,
      String por,
      String pol,
      String pod,
      String ssl,
      String containerType,
      String agent,
      String freightValidDate,
      String freightEffDate,
      String status,
      String remark,
      Boolean highlightOnly) {
    java.util.ArrayList<FreightCostResponse> rows = new java.util.ArrayList<>();
    int page = 1;
    long total = Long.MAX_VALUE;
    while (rows.size() < total) {
      PageResult<FreightCostResponse> batch =
          listSea(
              user,
              page,
              EXPORT_BATCH_SIZE,
              por,
              pol,
              pod,
              ssl,
              containerType,
              agent,
              freightValidDate,
              freightEffDate,
              status,
              remark,
              null,
              null,
              highlightOnly);
      rows.addAll(batch.items());
      total = batch.total();
      if (batch.items().isEmpty()) {
        break;
      }
      page++;
    }
    return rows;
  }

  private List<FumigationCostResponse> listAllFumigationForExport(
      SysUser user,
      String region,
      String station,
      String outdoorValidity,
      String indoorValidity,
      String status,
      Boolean highlightOnly) {
    java.util.ArrayList<FumigationCostResponse> rows = new java.util.ArrayList<>();
    int page = 1;
    long total = Long.MAX_VALUE;
    while (rows.size() < total) {
      PageResult<FumigationCostResponse> batch =
          listFumigation(
              user,
              page,
              EXPORT_BATCH_SIZE,
              region,
              station,
              outdoorValidity,
              indoorValidity,
              status,
              null,
              null,
              highlightOnly);
      rows.addAll(batch.items());
      total = batch.total();
      if (batch.items().isEmpty()) {
        break;
      }
      page++;
    }
    return rows;
  }

  private PageResult<RoadCostResponse> applyRoadPage(PageResult<RoadCostResponse> source) {
    List<Long> ids = source.items().stream().map(RoadCostResponse::id).toList();
    Map<Long, Long> quoteCounts =
        quoteLibraryUsageService.loadQuoteCounts(CostHighlightMode.road, ids);
    Map<Long, Map<String, BigDecimal>> overrides =
        quoteLibraryOverrideService.loadOverrides(CostHighlightMode.road, ids);
    return new PageResult<>(
        source.items().stream()
            .map(
                row -> {
                  RoadCostResponse ruled = quoteLibraryApplyService.applyRoad(row);
                  RoadCostResponse merged =
                      quoteLibraryApplyService.applyRoadOverrides(
                          ruled, overrides.getOrDefault(row.id(), Map.of()));
                  long count = quoteCounts.getOrDefault(row.id(), 0L);
                  return merged.withQuoteUsage(count > 0, count);
                })
            .toList(),
        source.total());
  }

  private PageResult<FreightCostResponse> applySeaPage(PageResult<FreightCostResponse> source) {
    List<Long> ids = source.items().stream().map(FreightCostResponse::id).toList();
    Map<Long, Long> quoteCounts =
        quoteLibraryUsageService.loadQuoteCounts(CostHighlightMode.sea, ids);
    Map<Long, Map<String, BigDecimal>> overrides =
        quoteLibraryOverrideService.loadOverrides(CostHighlightMode.sea, ids);
    return new PageResult<>(
        source.items().stream()
            .map(
                row -> {
                  FreightCostResponse ruled = quoteLibraryApplyService.applySea(row);
                  FreightCostResponse merged =
                      quoteLibraryApplyService.applySeaOverrides(
                          ruled, overrides.getOrDefault(row.id(), Map.of()));
                  long count = quoteCounts.getOrDefault(row.id(), 0L);
                  return merged.withQuoteUsage(count > 0, count);
                })
            .toList(),
        source.total());
  }

  private PageResult<FumigationCostResponse> applyFumigationPage(
      PageResult<FumigationCostResponse> source) {
    List<Long> ids = source.items().stream().map(FumigationCostResponse::id).toList();
    Map<Long, Long> quoteCounts =
        quoteLibraryUsageService.loadQuoteCounts(CostHighlightMode.fumigation, ids);
    Map<Long, Map<String, BigDecimal>> overrides =
        quoteLibraryOverrideService.loadOverrides(CostHighlightMode.fumigation, ids);
    return new PageResult<>(
        source.items().stream()
            .map(
                row -> {
                  FumigationCostResponse ruled = quoteLibraryApplyService.applyFumigation(row);
                  FumigationCostResponse merged =
                      quoteLibraryApplyService.applyFumigationOverrides(
                          ruled, overrides.getOrDefault(row.id(), Map.of()));
                  long count = quoteCounts.getOrDefault(row.id(), 0L);
                  return merged.withQuoteUsage(count > 0, count);
                })
            .toList(),
        source.total());
  }
}
