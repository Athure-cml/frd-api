package com.furuiduo.quote.quote.support;

import java.util.HashMap;
import java.util.Map;

import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.entity.CostSea;
import com.furuiduo.quote.cost.entity.CostStatus;
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

  /** 字段名与成本库列表一致 */
  public static Map<String, Object> roadSnapshot(CostRoad road) {
    Map<String, Object> map = new HashMap<>();
    map.put("validDate", road.getValidDate());
    map.put("supplier", road.getSupplier());
    map.put("logYardNameAddress", road.getLogYardNameAddress());
    map.put("zipCode", road.getZipCode());
    map.put("city", road.getCity());
    map.put("state", road.getState());
    map.put("por", road.getPor());
    map.put("pol", road.getPol());
    map.put("baseFreight", road.getBaseFreight());
    map.put("fsc", road.getFsc());
    map.put("chassis", road.getChassis());
    map.put("owTriAxle", road.getOwTriAxle());
    map.put("split", road.getSplit());
    map.put("stopOff", road.getStopOff());
    map.put("allIn", road.getAllIn());
    map.put("allInNonOak", road.getAllInNonOak());
    map.put("allInOak", road.getAllInOak());
    map.put("waitingFee", road.getWaitingFee());
    map.put("redelivery", road.getRedelivery());
    map.put("prepull", road.getPrepull());
    map.put("nsLift", road.getNsLift());
    map.put("remark", road.getRemark());
    if (road.getExtraFields() != null && !road.getExtraFields().isEmpty()) {
      map.put("extraFields", road.getExtraFields());
    }
    return map;
  }

  /** 字段名与成本库列表一致 */
  public static Map<String, Object> seaSnapshot(CostSea sea) {
    Map<String, Object> map = new HashMap<>();
    map.put("origin", sea.getOrigin());
    map.put("destination", sea.getDestination());
    map.put("unitPrice", sea.getUnitPrice());
    map.put("buc", sea.getBuc());
    map.put("surchargeValidDate", sea.getSurchargeValidDate());
    map.put("allIn", sea.getAllIn());
    map.put("carrier", sea.getCarrier());
    map.put("remark", sea.getRemark());
    map.put("validDate", sea.getValidDate());
    map.put("status", sea.getStatus() != null ? sea.getStatus().name() : CostStatus.draft.name());
    map.put("currency", sea.getCurrency());
    map.put("spec", sea.getSpec());
    map.put("unit", sea.getUnit());
    map.put("validFrom", sea.getValidFrom());
    map.put("validTo", sea.getValidTo());
    if (sea.getExtraFields() != null && !sea.getExtraFields().isEmpty()) {
      map.put("extraFields", sea.getExtraFields());
    }
    return map;
  }

  /** 字段名与成本库列表一致 */
  public static Map<String, Object> fumigationSnapshot(CostFumigation fum) {
    Map<String, Object> map = new HashMap<>();
    map.put("port", fum.getPort());
    map.put("station", fum.getStation());
    map.put("nonOakOutdoor", fum.getNonOakOutdoor());
    map.put("nonOakIndoor", fum.getNonOakIndoor());
    map.put("nonOakQuoteSummer", fum.getNonOakQuoteSummer());
    map.put("nonOakQuoteWinter", fum.getNonOakQuoteWinter());
    map.put("oakOutdoor", fum.getOakOutdoor());
    map.put("oakIndoor", fum.getOakIndoor());
    map.put("oakQuoteSummer", fum.getOakQuoteSummer());
    map.put("oakQuoteWinter", fum.getOakQuoteWinter());
    map.put("remark", fum.getRemark());
    map.put("updatedAt", fum.getUpdatedAt() != null ? fum.getUpdatedAt().toString() : null);
    if (fum.getExtraFields() != null && !fum.getExtraFields().isEmpty()) {
      map.put("extraFields", fum.getExtraFields());
    }
    return map;
  }
}
