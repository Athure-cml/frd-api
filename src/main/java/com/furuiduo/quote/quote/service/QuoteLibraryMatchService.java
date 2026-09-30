package com.furuiduo.quote.quote.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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
import com.furuiduo.quote.quote.dto.QuoteCostMatchItemDto;
import com.furuiduo.quote.quote.dto.QuoteSheetFieldsDto;
import com.furuiduo.quote.quote.entity.QuoteCostType;
import com.furuiduo.quote.quote.support.QuoteCostMatchSupport;
import com.furuiduo.quote.quote.support.QuoteCostSnapshotMapper;
import com.furuiduo.quote.quote.support.QuoteLibraryModeSupport;
import com.furuiduo.quote.quote.support.QuoteRoadAllInSupport;
import com.furuiduo.quote.quoterule.QuoteRuleContext;

/** 报价单匹配：仅使用已入报价库的数据（规则 + override 后的价格）。 */
@Service
public class QuoteLibraryMatchService {

  private static final String ROAD_EXTRA_CHASSIS_KEY = "cf_road_extra_chassis";

  private static final Pattern VALIDITY_RANGE =
      Pattern.compile(
          "^(\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})\\s*[-–—]\\s*(\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})$");

  private static final DateTimeFormatter[] VALIDITY_FORMATS =
      new DateTimeFormatter[] {
        DateTimeFormatter.ofPattern("yyyy/M/d"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("yyyy-M-d"),
        DateTimeFormatter.ISO_LOCAL_DATE
      };

  private final QuoteLibraryEntryService quoteLibraryEntryService;
  private final QuoteLibraryApplyService quoteLibraryApplyService;
  private final QuoteLibraryOverrideService quoteLibraryOverrideService;
  private final CostRoadRepository costRoadRepository;
  private final CostSeaRepository costSeaRepository;
  private final CostFumigationRepository costFumigationRepository;

  public QuoteLibraryMatchService(
      QuoteLibraryEntryService quoteLibraryEntryService,
      QuoteLibraryApplyService quoteLibraryApplyService,
      QuoteLibraryOverrideService quoteLibraryOverrideService,
      CostRoadRepository costRoadRepository,
      CostSeaRepository costSeaRepository,
      CostFumigationRepository costFumigationRepository) {
    this.quoteLibraryEntryService = quoteLibraryEntryService;
    this.quoteLibraryApplyService = quoteLibraryApplyService;
    this.quoteLibraryOverrideService = quoteLibraryOverrideService;
    this.costRoadRepository = costRoadRepository;
    this.costSeaRepository = costSeaRepository;
    this.costFumigationRepository = costFumigationRepository;
  }

  public boolean isInLibrary(QuoteCostType type, Long costId) {
    return costId != null
        && quoteLibraryEntryService
            .entryCostIds(QuoteLibraryModeSupport.toMode(type))
            .contains(costId);
  }

  public void requireInLibrary(QuoteCostType type, Long costId) {
    if (!isInLibrary(type, costId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "该成本尚未入报价库，无法用于报价单");
    }
  }

  public List<CostRoad> filterRoadInLibrary(List<CostRoad> roads) {
    Set<Long> ids = quoteLibraryEntryService.entryCostIds(CostHighlightMode.road);
    return roads.stream().filter(road -> ids.contains(road.getId())).toList();
  }

  public List<CostSea> filterSeaInLibrary(List<CostSea> seas) {
    Set<Long> ids = quoteLibraryEntryService.entryCostIds(CostHighlightMode.sea);
    return seas.stream().filter(sea -> ids.contains(sea.getId())).toList();
  }

  public List<CostFumigation> filterFumigationInLibrary(List<CostFumigation> fums) {
    Set<Long> ids = quoteLibraryEntryService.entryCostIds(CostHighlightMode.fumigation);
    return fums.stream().filter(fum -> ids.contains(fum.getId())).toList();
  }

  public Optional<QuoteCostMatchItemDto> matchRoad(CostRoad road, Map<String, Object> keys) {
    if (road == null || !isInLibrary(QuoteCostType.ROAD, road.getId())) {
      return Optional.empty();
    }
    return Optional.of(
        QuoteCostSnapshotMapper.fromLibraryRoad(loadRoadRow(road.getId()), keys));
  }

  public Optional<QuoteCostMatchItemDto> matchSea(CostSea sea, Map<String, Object> keys) {
    if (sea == null || !isInLibrary(QuoteCostType.SEA, sea.getId())) {
      return Optional.empty();
    }
    return Optional.of(
        QuoteCostSnapshotMapper.fromLibrarySea(loadSeaRow(sea.getId()), keys));
  }

  public Optional<QuoteCostMatchItemDto> matchFumigation(
      CostFumigation fum, Map<String, Object> keys) {
    if (fum == null || !isInLibrary(QuoteCostType.FUMIGATION, fum.getId())) {
      return Optional.empty();
    }
    return Optional.of(
        QuoteCostSnapshotMapper.fromLibraryFumigation(loadFumigationRow(fum.getId()), keys));
  }

  public QuoteSheetFieldsDto buildImportFields(
      QuoteCostType type,
      Long costId,
      QuoteRuleContext ruleContext,
      LocalDate quoteDate,
      boolean fumigationEnabled,
      String oakType) {
    requireInLibrary(type, costId);
    return switch (type) {
      case ROAD -> buildRoadSheetFields(loadRoadRow(costId), fumigationEnabled, oakType);
      case SEA -> buildSeaSheetFields(loadSeaRow(costId));
      case FUMIGATION -> buildFumigationSheetFields(loadFumigationRow(costId), quoteDate);
    };
  }

  public QuoteSheetFieldsDto buildRoadSheetFields(
      RoadCostResponse row, boolean fumigationEnabled, String oakType) {
    BigDecimal truckingFee =
        QuoteRoadAllInSupport.pick(
            row.allInNoFm(),
            row.allInFmOneWay(),
            row.allInFmRound(),
            fumigationEnabled,
            oakType);
    BigDecimal truckingNonOakUsd = null;
    BigDecimal truckingOakUsd = null;
    return new QuoteSheetFieldsDto(
        row.zipCode(),
        row.city(),
        row.state(),
        row.logYardNameAddress(),
        row.por(),
        row.pol(),
        null,
        null,
        null,
        truckingFee,
        row.nsLift(),
        resolveExtraChassis(row),
        row.waitingFee(),
        row.redelivery(),
        QuoteCostMatchSupport.resolveRoadRemarkFromSnapshot(
            QuoteCostSnapshotMapper.roadSnapshotFromResponse(row)),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        truckingNonOakUsd,
        truckingOakUsd,
        null,
        null);
  }

  public QuoteSheetFieldsDto buildSeaSheetFields(FreightCostResponse row) {
    String oceanFreight = row.allIn() != null ? row.allIn().toPlainString() : null;
    return new QuoteSheetFieldsDto(
        null,
        null,
        null,
        null,
        row.por(),
        row.pol(),
        row.pod(),
        oceanFreight,
        row.ssl(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public QuoteSheetFieldsDto buildFumigationSheetFields(
      FumigationCostResponse row, LocalDate quoteDate) {
    FumigationRates rates = resolveFumigationRates(row, quoteDate);
    return new QuoteSheetFieldsDto(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        rates.nonOak(),
        rates.oak(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public RoadCostResponse loadRoadRow(Long id) {
    CostRoad entity =
        costRoadRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在"));
    RoadCostResponse raw = RoadCostResponse.from(entity).withInQuoteLibrary(true);
    RoadCostResponse ruled = quoteLibraryApplyService.applyRoad(raw);
    Map<String, BigDecimal> overrides =
        quoteLibraryOverrideService
            .loadOverrides(CostHighlightMode.road, List.of(id))
            .getOrDefault(id, Map.of());
    return quoteLibraryApplyService.applyRoadOverrides(ruled, overrides);
  }

  public FreightCostResponse loadSeaRow(Long id) {
    CostSea entity =
        costSeaRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在"));
    FreightCostResponse raw = FreightCostResponse.fromSea(entity).withInQuoteLibrary(true);
    FreightCostResponse ruled = quoteLibraryApplyService.applySea(raw);
    Map<String, BigDecimal> overrides =
        quoteLibraryOverrideService
            .loadOverrides(CostHighlightMode.sea, List.of(id))
            .getOrDefault(id, Map.of());
    return quoteLibraryApplyService.applySeaOverrides(ruled, overrides);
  }

  public FumigationCostResponse loadFumigationRow(Long id) {
    CostFumigation entity =
        costFumigationRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在"));
    FumigationCostResponse raw = FumigationCostResponse.from(entity).withInQuoteLibrary(true);
    FumigationCostResponse ruled = quoteLibraryApplyService.applyFumigation(raw);
    Map<String, BigDecimal> overrides =
        quoteLibraryOverrideService
            .loadOverrides(CostHighlightMode.fumigation, List.of(id))
            .getOrDefault(id, Map.of());
    return quoteLibraryApplyService.applyFumigationOverrides(ruled, overrides);
  }

  public Map<Long, RoadCostResponse> loadRoadRows(Set<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return Map.of();
    }
    return ids.stream().collect(Collectors.toMap(id -> id, this::loadRoadRow));
  }

  public Map<Long, FreightCostResponse> loadSeaRows(Set<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return Map.of();
    }
    return ids.stream().collect(Collectors.toMap(id -> id, this::loadSeaRow));
  }

  public Map<Long, FumigationCostResponse> loadFumigationRows(Set<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return Map.of();
    }
    return ids.stream().collect(Collectors.toMap(id -> id, this::loadFumigationRow));
  }

  private BigDecimal resolveExtraChassis(RoadCostResponse row) {
    if (row.extraFields() != null) {
      Object extra = row.extraFields().get(ROAD_EXTRA_CHASSIS_KEY);
      BigDecimal parsed = toBigDecimal(extra);
      if (parsed != null) {
        return parsed;
      }
    }
    return row.chassis();
  }

  private FumigationRates resolveFumigationRates(FumigationCostResponse row, LocalDate quoteDate) {
    LocalDate outdoorEnd = parseValidityEnd(row.outdoorValidity());
    LocalDate indoorEnd = parseValidityEnd(row.indoorValidity());
    boolean outdoorValid = outdoorEnd == null || !quoteDate.isAfter(outdoorEnd);
    boolean indoorValid = indoorEnd == null || !quoteDate.isAfter(indoorEnd);
    if (outdoorValid) {
      return new FumigationRates(row.outdoorNonOak(), row.outdoorOak());
    }
    if (indoorValid) {
      return new FumigationRates(row.indoorNonOak(), row.indoorOak());
    }
    return new FumigationRates(row.outdoorNonOak(), row.outdoorOak());
  }

  private LocalDate parseValidityEnd(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String text = raw.trim();
    Matcher matcher = VALIDITY_RANGE.matcher(text);
    if (matcher.matches()) {
      text = matcher.group(2);
    }
    for (DateTimeFormatter formatter : VALIDITY_FORMATS) {
      try {
        return LocalDate.parse(text, formatter);
      } catch (DateTimeParseException ignored) {
        // try next
      }
    }
    return null;
  }

  private BigDecimal toBigDecimal(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    if (value instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    try {
      return new BigDecimal(String.valueOf(value).trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private record FumigationRates(BigDecimal nonOak, BigDecimal oak) {}
}
