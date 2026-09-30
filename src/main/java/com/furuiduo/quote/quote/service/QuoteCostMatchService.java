package com.furuiduo.quote.quote.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.common.SearchText;
import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.entity.CostSea;
import com.furuiduo.quote.cost.repository.CostFumigationRepository;
import com.furuiduo.quote.cost.repository.CostRoadRepository;
import com.furuiduo.quote.cost.repository.CostSeaRepository;
import com.furuiduo.quote.quote.dto.QuoteCostMatchItemDto;
import com.furuiduo.quote.quote.dto.QuoteMatchCostsRequest;
import com.furuiduo.quote.quote.dto.QuoteMatchCostsResponse;
import com.furuiduo.quote.quote.dto.QuoteSheetFieldsDto;
import com.furuiduo.quote.quote.entity.QuoteCostSnapshot;
import com.furuiduo.quote.quote.entity.QuoteCostType;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.repository.QuoteCostSnapshotRepository;
import com.furuiduo.quote.quote.support.QuoteCostMatchKeys;
import com.furuiduo.quote.quote.support.QuoteCostMatchSupport;
import com.furuiduo.quote.quote.support.QuoteCostSnapshotMapper;
import com.furuiduo.quote.quote.support.QuoteLibraryRowSupport;

@Service
public class QuoteCostMatchService {

  private final CostRoadRepository costRoadRepository;
  private final CostSeaRepository costSeaRepository;
  private final CostFumigationRepository costFumigationRepository;
  private final QuoteCostSnapshotRepository quoteCostSnapshotRepository;
  private final QuoteLibraryMatchService quoteLibraryMatchService;
  private final QuoteLibraryUsageService quoteLibraryUsageService;

  public QuoteCostMatchService(
      CostRoadRepository costRoadRepository,
      CostSeaRepository costSeaRepository,
      CostFumigationRepository costFumigationRepository,
      QuoteCostSnapshotRepository quoteCostSnapshotRepository,
      QuoteLibraryMatchService quoteLibraryMatchService,
      QuoteLibraryUsageService quoteLibraryUsageService) {
    this.costRoadRepository = costRoadRepository;
    this.costSeaRepository = costSeaRepository;
    this.costFumigationRepository = costFumigationRepository;
    this.quoteCostSnapshotRepository = quoteCostSnapshotRepository;
    this.quoteLibraryMatchService = quoteLibraryMatchService;
    this.quoteLibraryUsageService = quoteLibraryUsageService;
  }

  public QuoteMatchCostsResponse match(QuoteMatchCostsRequest request) {
    if (!hasAnyKey(request)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请至少填写一个匹配字段");
    }

    Map<String, Object> keys = buildMatchKeys(request);
    String costType = request.costType();
    if (costType != null && !costType.isBlank()) {
      return matchByType(request, keys, QuoteCostType.valueOf(costType.trim().toUpperCase()));
    }

    List<QuoteCostMatchItemDto> matches = new ArrayList<>();

    if (QuoteCostMatchSupport.hasRoadLocationKeys(request.city(), request.state())) {
      List<CostRoad> roads =
          costRoadRepository.matchByRoute(
              SearchText.orEmpty(request.zipCode()),
              SearchText.orEmpty(request.city()),
              SearchText.orEmpty(request.state()),
              "",
              "",
              SearchText.orEmpty(roadSupplier(request)));
      QuoteCostMatchSupport.firstActiveRoad(quoteLibraryMatchService.filterRoadInLibrary(roads))
          .flatMap(road -> quoteLibraryMatchService.matchRoad(road, keys))
          .ifPresent(matches::add);
    }

    List<CostSea> seas =
        costSeaRepository.matchByRoute(
            SearchText.orEmpty(QuoteCostMatchKeys.seaPor(request)),
            SearchText.orEmpty(request.pod()),
            SearchText.orEmpty(seaSsl(request)));
    QuoteCostMatchSupport.firstActiveSeaByPol(
            quoteLibraryMatchService.filterSeaInLibrary(seas), QuoteCostMatchKeys.seaPol(request))
        .flatMap(sea -> quoteLibraryMatchService.matchSea(sea, keys))
        .ifPresent(matches::add);

    List<CostFumigation> fums =
        costFumigationRepository.matchByStation(
            SearchText.orEmpty(QuoteCostMatchKeys.fumigationStation(request)));
    QuoteCostMatchSupport.firstActiveFumigation(
            quoteLibraryMatchService.filterFumigationInLibrary(fums))
        .flatMap(fum -> quoteLibraryMatchService.matchFumigation(fum, keys))
        .ifPresent(matches::add);

    if (matches.isEmpty()) {
      return new QuoteMatchCostsResponse(false, emptySuggested(), List.of());
    }

    return new QuoteMatchCostsResponse(true, buildSuggested(matches), matches);
  }

  private QuoteMatchCostsResponse matchByType(
      QuoteMatchCostsRequest request, Map<String, Object> keys, QuoteCostType type) {
    QuoteCostMatchItemDto match =
        switch (type) {
          case ROAD -> {
            if (!QuoteCostMatchSupport.hasRoadLocationKeys(request.city(), request.state())) {
              yield null;
            }
            List<CostRoad> roads =
                costRoadRepository.matchByRoute(
                    SearchText.orEmpty(request.zipCode()),
                    SearchText.orEmpty(request.city()),
                    SearchText.orEmpty(request.state()),
                    "",
                    "",
                    SearchText.orEmpty(roadSupplier(request)));
            yield QuoteCostMatchSupport.firstActiveRoad(
                    quoteLibraryMatchService.filterRoadInLibrary(roads))
                .flatMap(road -> quoteLibraryMatchService.matchRoad(road, keys))
                .orElse(null);
          }
          case SEA -> {
            List<CostSea> seas =
                costSeaRepository.matchByRoute(
                    SearchText.orEmpty(QuoteCostMatchKeys.seaPor(request)),
                    SearchText.orEmpty(request.pod()),
                    SearchText.orEmpty(seaSsl(request)));
            yield QuoteCostMatchSupport.firstActiveSeaByPol(
                    quoteLibraryMatchService.filterSeaInLibrary(seas),
                    QuoteCostMatchKeys.seaPol(request))
                .flatMap(sea -> quoteLibraryMatchService.matchSea(sea, keys))
                .orElse(null);
          }
          case FUMIGATION -> {
            List<CostFumigation> fums =
                costFumigationRepository.matchByStation(
                    SearchText.orEmpty(QuoteCostMatchKeys.fumigationStation(request)));
            yield QuoteCostMatchSupport.firstActiveFumigation(
                    quoteLibraryMatchService.filterFumigationInLibrary(fums))
                .flatMap(fum -> quoteLibraryMatchService.matchFumigation(fum, keys))
                .orElse(null);
          }
        };
    if (match == null) {
      return new QuoteMatchCostsResponse(false, emptySuggested(), List.of());
    }
    return new QuoteMatchCostsResponse(
        true, buildSuggested(List.of(match)), List.of(match));
  }

  @Transactional
  public void replaceSnapshots(QuoteOrder order, List<QuoteCostMatchItemDto> matches) {
    if (matches == null || matches.isEmpty()) {
      quoteCostSnapshotRepository.deleteByQuoteOrderId(order.getId());
      quoteLibraryUsageService.releaseByQuoteId(order.getId());
      return;
    }
    quoteCostSnapshotRepository.deleteByQuoteOrderId(order.getId());
    persistSnapshots(order, matches);
    quoteLibraryUsageService.replaceUsage(order, matches);
  }

  public void persistSnapshots(QuoteOrder order, List<QuoteCostMatchItemDto> matches) {
    for (QuoteCostMatchItemDto item : matches) {
      QuoteCostSnapshot snapshot = new QuoteCostSnapshot();
      snapshot.setQuoteOrder(order);
      snapshot.setCostType(QuoteCostType.valueOf(item.costType()));
      snapshot.setCostRefId(item.costRefId());
      snapshot.setCostVersion(item.costVersion());
      snapshot.setMatchKeysJson(item.matchKeys() != null ? item.matchKeys() : Map.of());
      snapshot.setSnapshotJson(item.snapshot() != null ? item.snapshot() : Map.of());
      quoteCostSnapshotRepository.save(snapshot);
    }
  }

  public List<QuoteCostMatchItemDto> listSnapshots(Long quoteId, String costType) {
    return listSnapshotEntities(quoteId, costType).stream().map(this::toDto).toList();
  }

  public List<QuoteCostSnapshot> listSnapshotEntities(Long quoteId, String costType) {
    if (costType == null || costType.isBlank()) {
      return quoteCostSnapshotRepository.findByQuoteOrderIdOrderByCreatedAtDesc(quoteId);
    }
    return quoteCostSnapshotRepository.findByQuoteOrderIdAndCostTypeOrderByCreatedAtDesc(
        quoteId, QuoteCostType.valueOf(costType));
  }

  /** 报价库列表：按报价单 ID 批量取各类型最新一条成本快照实体。 */
  @Transactional(readOnly = true)
  public Map<Long, QuoteCostSnapshot> firstSnapshotEntitiesByQuoteIds(
      Collection<Long> quoteIds, QuoteCostType costType) {
    if (quoteIds == null || quoteIds.isEmpty()) {
      return Map.of();
    }
    var snapshots =
        quoteCostSnapshotRepository.findByQuoteOrderIdInAndCostTypeOrderByCreatedAtDesc(
            quoteIds, costType);
    Map<Long, QuoteCostSnapshot> result = new HashMap<>();
    for (QuoteCostSnapshot snapshot : snapshots) {
      Long quoteId = snapshot.getQuoteOrder().getId();
      result.putIfAbsent(quoteId, snapshot);
    }
    return result;
  }

  /** 报价库行：从成本库同步最新数据，并写入报价规则处理后的费用。 */
  @Transactional(readOnly = true)
  public Map<Long, Map<String, Object>> buildLibraryRows(
      List<QuoteOrder> orders, QuoteCostType costType) {
    if (orders == null || orders.isEmpty()) {
      return Map.of();
    }
    List<Long> quoteIds = orders.stream().map(QuoteOrder::getId).toList();
    Map<Long, QuoteCostSnapshot> snapshots =
        firstSnapshotEntitiesByQuoteIds(quoteIds, costType);
    if (snapshots.isEmpty()) {
      return Map.of();
    }

    Set<Long> refIds =
        snapshots.values().stream().map(QuoteCostSnapshot::getCostRefId).collect(Collectors.toSet());

    Map<Long, CostRoad> roads = Map.of();
    Map<Long, CostSea> seas = Map.of();
    Map<Long, CostFumigation> fumigations = Map.of();
    switch (costType) {
      case ROAD ->
          roads =
              costRoadRepository.findAllById(refIds).stream()
                  .collect(Collectors.toMap(CostRoad::getId, Function.identity()));
      case SEA ->
          seas =
              costSeaRepository.findAllById(refIds).stream()
                  .collect(Collectors.toMap(CostSea::getId, Function.identity()));
      case FUMIGATION ->
          fumigations =
              costFumigationRepository.findAllById(refIds).stream()
                  .collect(Collectors.toMap(CostFumigation::getId, Function.identity()));
    }

    Map<Long, Map<String, Object>> result = new HashMap<>();
    for (QuoteOrder order : orders) {
      QuoteCostSnapshot snapshot = snapshots.get(order.getId());
      if (snapshot == null) {
        continue;
      }
      Long refId = snapshot.getCostRefId();
      Map<String, Object> row =
          QuoteLibraryRowSupport.build(
              order,
              snapshot,
              costType,
              roads.get(refId),
              seas.get(refId),
              fumigations.get(refId));
      if (row != null && !row.isEmpty()) {
        result.put(order.getId(), row);
      }
    }
    return result;
  }

  public List<QuoteCostMatchItemDto> listSnapshotsByRefId(
      Long quoteId, QuoteCostType type, Long costRefId) {
    return quoteCostSnapshotRepository
        .findByQuoteOrderIdAndCostTypeOrderByCreatedAtDesc(quoteId, type)
        .stream()
        .filter(s -> s.getCostRefId().equals(costRefId))
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  public QuoteCostMatchItemDto toDto(QuoteCostSnapshot snapshot) {
    return new QuoteCostMatchItemDto(
        snapshot.getCostType().name(),
        snapshot.getCostRefId(),
        snapshot.getCostVersion(),
        snapshot.getMatchKeysJson(),
        snapshot.getSnapshotJson());
  }

  private QuoteSheetFieldsDto buildSuggested(List<QuoteCostMatchItemDto> matches) {
    String zipCode = null;
    String city = null;
    String state = null;
    String pickUpAddress = null;
    String por = null;
    String pol = null;
    String pod = null;
    String oceanFreight = null;
    String ssl = null;
    BigDecimal truckingFee = null;
    BigDecimal nsLift = null;
    BigDecimal chassis = null;
    BigDecimal waiting = null;
    BigDecimal redeliveryFee = null;
    String truckRemark = null;
    BigDecimal truckingOak = null;

    for (QuoteCostMatchItemDto item : matches) {
      Map<String, Object> snap = item.snapshot();
      if (snap == null) {
        continue;
      }
      if ("SEA".equals(item.costType())) {
        Object rate = snap.get("ofRateUsd");
        if (rate == null || String.valueOf(rate).isBlank()) {
          rate = formatOfRateFromSnapshot(snap);
        }
        if (rate != null && !String.valueOf(rate).isBlank()) {
          oceanFreight = String.valueOf(rate);
        }
        Object carrier = snap.get("supplier");
        if (carrier == null || String.valueOf(carrier).isBlank()) {
          carrier = snap.get("ssl");
        }
        if (carrier == null || String.valueOf(carrier).isBlank()) {
          carrier = snap.get("carrier");
        }
        if (carrier != null && !String.valueOf(carrier).isBlank()) {
          ssl = String.valueOf(carrier);
        }
        pod = firstNonBlank(pod, text(snap.get("pod")));
        pol = firstNonBlank(pol, text(snap.get("pol")));
        por = firstNonBlank(por, text(snap.get("por")));
      }
      if ("ROAD".equals(item.costType())) {
        zipCode = firstNonBlank(zipCode, text(snap.get("zipCode")));
        city = firstNonBlank(city, text(snap.get("city")));
        state = firstNonBlank(state, text(snap.get("state")));
        pickUpAddress = firstNonBlank(pickUpAddress, text(snap.get("logYardNameAddress")));
        por = firstNonBlank(por, text(snap.get("por")));
        pol = firstNonBlank(pol, text(snap.get("pol")));
        truckingFee = toBigDecimal(snap.get("allInNoFm"));
        truckingOak = toBigDecimal(snap.get("allInFmOneWay"));
        nsLift = toBigDecimal(snap.get("nsLift"));
        chassis = toBigDecimal(snap.get("chassis"));
        waiting = toBigDecimal(snap.get("waitingFee"));
        redeliveryFee = toBigDecimal(snap.get("redelivery"));
        truckRemark =
            firstNonBlank(
                truckRemark, QuoteCostMatchSupport.resolveRoadRemarkFromSnapshot(snap));
      }
    }

    return new QuoteSheetFieldsDto(
        zipCode,
        city,
        state,
        pickUpAddress,
        por,
        pol,
        pod,
        oceanFreight,
        ssl,
        truckingFee,
        nsLift,
        chassis,
        waiting,
        redeliveryFee,
        truckRemark,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        truckingFee,
        truckingOak,
        null,
        null);
  }

  private QuoteSheetFieldsDto emptySuggested() {
    return new QuoteSheetFieldsDto(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null, null, null);
  }

  private String text(Object value) {
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value).trim();
    return text.isEmpty() ? null : text;
  }

  private String firstNonBlank(String current, String next) {
    if (current != null && !current.isBlank()) {
      return current;
    }
    return next;
  }

  private boolean hasAnyKey(QuoteMatchCostsRequest request) {
    return isNotBlank(request.por())
        || isNotBlank(request.pol())
        || isNotBlank(request.pod())
        || isNotBlank(request.supplier())
        || isNotBlank(request.city())
        || isNotBlank(request.state())
        || isNotBlank(request.zipCode())
        || isNotBlank(request.ssl())
        || isNotBlank(request.fumigationPoint());
  }

  private Map<String, Object> buildMatchKeys(QuoteMatchCostsRequest request) {
    Map<String, Object> keys = new HashMap<>();
    putIfPresent(keys, "zipCode", request.zipCode());
    putIfPresent(keys, "city", request.city());
    putIfPresent(keys, "state", request.state());
    putIfPresent(keys, "pod", request.pod());
    putIfPresent(keys, "pol", request.pol());
    putIfPresent(keys, "supplier", roadSupplier(request));
    putIfPresent(keys, "por", request.por());
    putIfPresent(keys, "fumigationPoint", request.fumigationPoint());
    putIfPresent(keys, "station", QuoteCostMatchKeys.fumigationStation(request));
    putIfPresent(keys, "ssl", seaSsl(request));
    return keys;
  }

  /** 卡车成本库按 supplier 匹配；兼容旧入参 por/zipCode */
  private String roadSupplier(QuoteMatchCostsRequest request) {
    if (isNotBlank(request.supplier())) {
      return request.supplier();
    }
    return null;
  }

  /** 海运成本库按 SSL 匹配；兼容旧入参 supplier */
  private String seaSsl(QuoteMatchCostsRequest request) {
    if (isNotBlank(request.ssl())) {
      return request.ssl();
    }
    return request.supplier();
  }

  private void putIfPresent(Map<String, Object> map, String key, String value) {
    if (isNotBlank(value)) {
      map.put(key, value.trim());
    }
  }

  private boolean isNotBlank(String value) {
    return value != null && !value.isBlank();
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
      return new BigDecimal(String.valueOf(value));
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private String formatOfRateFromSnapshot(Map<String, Object> snap) {
    Object priceObj = snap.get("allIn");
    if (priceObj == null) {
      priceObj = snap.get("baseFreight");
    }
    if (priceObj == null) {
      priceObj = snap.get("unitPrice");
    }
    if (priceObj == null) {
      return "";
    }
    String price = String.valueOf(priceObj);
    return price.isBlank() ? "" : price;
  }
}
