package com.furuiduo.quote.quote.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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
import com.furuiduo.quote.quote.support.QuoteCostSnapshotMapper;

@Service
public class QuoteCostMatchService {

  private final CostRoadRepository costRoadRepository;
  private final CostSeaRepository costSeaRepository;
  private final CostFumigationRepository costFumigationRepository;
  private final QuoteCostSnapshotRepository quoteCostSnapshotRepository;

  public QuoteCostMatchService(
      CostRoadRepository costRoadRepository,
      CostSeaRepository costSeaRepository,
      CostFumigationRepository costFumigationRepository,
      QuoteCostSnapshotRepository quoteCostSnapshotRepository) {
    this.costRoadRepository = costRoadRepository;
    this.costSeaRepository = costSeaRepository;
    this.costFumigationRepository = costFumigationRepository;
    this.quoteCostSnapshotRepository = quoteCostSnapshotRepository;
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

    List<CostRoad> roads =
        costRoadRepository.matchByRoute(
            SearchText.orEmpty(request.city()),
            SearchText.orEmpty(request.state()),
            SearchText.orEmpty(request.por()),
            SearchText.orEmpty(request.pol()));
    if (!roads.isEmpty()) {
      matches.add(QuoteCostSnapshotMapper.fromRoad(roads.getFirst(), keys));
    }

    List<CostSea> seas =
        costSeaRepository.matchByRoute(
            SearchText.orEmpty(request.pol()),
            SearchText.orEmpty(request.pod()),
            SearchText.orEmpty(request.ssl()));
    if (!seas.isEmpty()) {
      matches.add(QuoteCostSnapshotMapper.fromSea(seas.getFirst(), keys));
    }

    List<CostFumigation> fums =
        costFumigationRepository.matchByPort(SearchText.orEmpty(request.pod()));
    if (!fums.isEmpty()) {
      matches.add(QuoteCostSnapshotMapper.fromFumigation(fums.getFirst(), keys));
    }

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
            List<CostRoad> roads =
                costRoadRepository.matchByRoute(
                    SearchText.orEmpty(request.city()),
                    SearchText.orEmpty(request.state()),
                    SearchText.orEmpty(request.por()),
                    SearchText.orEmpty(request.pol()));
            if (roads.isEmpty()) {
              yield null;
            }
            yield QuoteCostSnapshotMapper.fromRoad(roads.getFirst(), keys);
          }
          case SEA -> {
            List<CostSea> seas =
                costSeaRepository.matchByRoute(
                    SearchText.orEmpty(request.pol()),
                    SearchText.orEmpty(request.pod()),
                    SearchText.orEmpty(request.ssl()));
            if (seas.isEmpty()) {
              yield null;
            }
            yield QuoteCostSnapshotMapper.fromSea(seas.getFirst(), keys);
          }
          case FUMIGATION -> {
            List<CostFumigation> fums =
                costFumigationRepository.matchByPort(SearchText.orEmpty(request.pod()));
            if (fums.isEmpty()) {
              yield null;
            }
            yield QuoteCostSnapshotMapper.fromFumigation(fums.getFirst(), keys);
          }
        };
    if (match == null) {
      return new QuoteMatchCostsResponse(false, emptySuggested(), List.of());
    }
    return new QuoteMatchCostsResponse(
        true, buildSuggested(List.of(match)), List.of(match));
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
    var list =
        costType == null || costType.isBlank()
            ? quoteCostSnapshotRepository.findByQuoteOrderIdOrderByCreatedAtDesc(quoteId)
            : quoteCostSnapshotRepository.findByQuoteOrderIdAndCostTypeOrderByCreatedAtDesc(
                quoteId, QuoteCostType.valueOf(costType));
    return list.stream().map(this::toDto).toList();
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

  private QuoteCostMatchItemDto toDto(QuoteCostSnapshot snapshot) {
    return new QuoteCostMatchItemDto(
        snapshot.getCostType().name(),
        snapshot.getCostRefId(),
        snapshot.getCostVersion(),
        snapshot.getMatchKeysJson(),
        snapshot.getSnapshotJson());
  }

  private QuoteSheetFieldsDto buildSuggested(List<QuoteCostMatchItemDto> matches) {
    String ofUsd = null;
    String ssl = null;
    BigDecimal truckingNonOak = null;
    BigDecimal truckingOak = null;
    BigDecimal fmNonOak = null;
    BigDecimal fmOak = null;

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
          ofUsd = String.valueOf(rate);
        }
        Object carrier = snap.get("ssl");
        if (carrier == null || String.valueOf(carrier).isBlank()) {
          carrier = snap.get("carrier");
        }
        if (carrier != null && !String.valueOf(carrier).isBlank()) {
          ssl = String.valueOf(carrier);
        }
      }
      if ("ROAD".equals(item.costType())) {
        truckingNonOak = toBigDecimal(snap.get("allInNonOak"));
        truckingOak = toBigDecimal(snap.get("allInOak"));
        BigDecimal fsc = toBigDecimal(snap.get("fscFreight"));
        if (fsc == null) {
          fsc = toBigDecimal(snap.get("fsc"));
        }
        fmNonOak = fsc;
        fmOak = fsc;
      }
    }

    return new QuoteSheetFieldsDto(
        null, null, null, null, null, null, ofUsd, ssl,
        truckingNonOak, truckingOak, fmNonOak, fmOak,
        null, null, null);
  }

  private QuoteSheetFieldsDto emptySuggested() {
    return new QuoteSheetFieldsDto(
        null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null);
  }

  private boolean hasAnyKey(QuoteMatchCostsRequest request) {
    return isNotBlank(request.zipCode())
        || isNotBlank(request.city())
        || isNotBlank(request.state())
        || isNotBlank(request.por())
        || isNotBlank(request.pol())
        || isNotBlank(request.pod())
        || isNotBlank(request.ssl());
  }

  private Map<String, Object> buildMatchKeys(QuoteMatchCostsRequest request) {
    Map<String, Object> keys = new HashMap<>();
    putIfPresent(keys, "zipCode", request.zipCode());
    putIfPresent(keys, "city", request.city());
    putIfPresent(keys, "state", request.state());
    putIfPresent(keys, "por", request.por());
    putIfPresent(keys, "pol", request.pol());
    putIfPresent(keys, "pod", request.pod());
    putIfPresent(keys, "ssl", request.ssl());
    return keys;
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
      priceObj = snap.get("unitPrice");
    }
    if (priceObj == null) {
      return "";
    }
    String price = String.valueOf(priceObj);
    if (price.isBlank()) {
      return "";
    }
    Object spec = snap.get("spec");
    Object unit = snap.get("unit");
    String denom =
        spec != null && !String.valueOf(spec).isBlank()
            ? String.valueOf(spec).trim()
            : unit != null && !String.valueOf(unit).isBlank()
                ? String.valueOf(unit).trim()
                : "";
    return denom.isBlank() ? price : price + "/" + denom;
  }
}
