package com.furuiduo.quote.quote.support;

import java.util.HashMap;
import java.util.Map;

import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.entity.CostSea;
import com.furuiduo.quote.quote.dto.QuoteCostMatchItemDto;
import com.furuiduo.quote.quote.entity.QuoteCostType;

public final class QuoteCostSnapshotMapper {

  private QuoteCostSnapshotMapper() {}

  public static QuoteCostMatchItemDto fromRoad(CostRoad road, Map<String, Object> keys) {
    return new QuoteCostMatchItemDto(
        QuoteCostType.ROAD.name(),
        road.getId(),
        road.getValidDate(),
        keys,
        roadSnapshot(road));
  }

  public static QuoteCostMatchItemDto fromSea(CostSea sea, Map<String, Object> keys) {
    return new QuoteCostMatchItemDto(
        QuoteCostType.SEA.name(),
        sea.getId(),
        sea.getValidDate(),
        keys,
        seaSnapshot(sea));
  }

  public static QuoteCostMatchItemDto fromFumigation(
      CostFumigation fum, Map<String, Object> keys) {
    return new QuoteCostMatchItemDto(
        QuoteCostType.FUMIGATION.name(),
        fum.getId(),
        fum.getUpdatedAt() != null ? fum.getUpdatedAt().toString() : null,
        keys,
        fumigationSnapshot(fum));
  }

  public static Map<String, Object> roadSnapshot(CostRoad road) {
    Map<String, Object> map = new HashMap<>();
    map.put("validDate", nullToEmpty(road.getValidDate()));
    map.put("city", nullToEmpty(road.getCity()));
    map.put("state", nullToEmpty(road.getState()));
    map.put("por", nullToEmpty(road.getPor()));
    map.put("pol", nullToEmpty(road.getPol()));
    map.put("allInNonOak", road.getAllInNonOak());
    map.put("allInOak", road.getAllInOak());
    map.put("fscFreight", road.getFsc());
    map.put("supplier", nullToEmpty(road.getSupplier()));
    return map;
  }

  public static Map<String, Object> seaSnapshot(CostSea sea) {
    Map<String, Object> map = new HashMap<>();
    map.put("validDate", nullToEmpty(sea.getValidDate()));
    map.put("pol", nullToEmpty(sea.getOrigin()));
    map.put("pod", nullToEmpty(sea.getDestination()));
    map.put("ssl", nullToEmpty(sea.getCarrier()));
    map.put("ofRateUsd", formatOfRate(sea));
    map.put("unitPrice", sea.getUnitPrice());
    map.put("spec", nullToEmpty(sea.getSpec()));
    map.put("currency", nullToEmpty(sea.getCurrency()));
    return map;
  }

  public static Map<String, Object> fumigationSnapshot(CostFumigation fum) {
    Map<String, Object> map = new HashMap<>();
    map.put("port", nullToEmpty(fum.getPort()));
    map.put("station", nullToEmpty(fum.getStation()));
    map.put("nonOakOutdoor", fum.getNonOakOutdoor());
    map.put("nonOakIndoor", fum.getNonOakIndoor());
    map.put("oakOutdoor", fum.getOakOutdoor());
    map.put("oakIndoor", fum.getOakIndoor());
    map.put("remark", nullToEmpty(fum.getRemark()));
    return map;
  }

  private static String formatOfRate(CostSea sea) {
    if (sea.getUnitPrice() == null) {
      return "";
    }
    String spec = sea.getSpec() != null ? sea.getSpec() : "";
    return sea.getUnitPrice().stripTrailingZeros().toPlainString()
        + (spec.isBlank() ? "" : "/" + spec);
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
