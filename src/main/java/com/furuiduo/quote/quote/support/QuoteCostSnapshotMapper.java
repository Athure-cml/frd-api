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
        sea.getFreightValidDate(),
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
    map.put("zipCode", road.getZipCode());
    map.put("city", road.getCity());
    map.put("state", road.getState());
    map.put("por", road.getPor());
    map.put("pol", road.getPol());
    map.put("supplier", road.getSupplier());
    map.put("baseFreight", road.getBaseFreight());
    map.put("fsc", road.getFsc());
    map.put("chassis", road.getChassis());
    map.put("triTandemAxle", road.getTriTandemAxle());
    map.put("split", road.getSplit());
    map.put("stopOff", road.getStopOff());
    map.put("allInNoFm", road.getAllInNoFm());
    map.put("allInFmOneWay", road.getAllInFmOneWay());
    map.put("allInFmRound", road.getAllInFmRound());
    map.put("waitingFee", road.getWaitingFee());
    map.put("redelivery", road.getRedelivery());
    map.put("prepull", road.getPrepull());
    map.put("nsLift", road.getNsLift());
    map.put("otherFee", road.getOtherFee());
    map.put("remark", road.getRemark());
    map.put("validDate", road.getValidDate());
    map.put("logYardNameAddress", road.getLogYardNameAddress());
    if (road.getExtraFields() != null && !road.getExtraFields().isEmpty()) {
      map.put("extraFields", road.getExtraFields());
    }
    return map;
  }

  /** 字段名与成本库列表一致 */
  public static Map<String, Object> seaSnapshot(CostSea sea) {
    Map<String, Object> map = new HashMap<>();
    map.put("por", sea.getPor());
    map.put("pol", sea.getPol());
    map.put("pod", sea.getPod());
    map.put("cnShortName", sea.getCnShortName());
    map.put("enProductName", sea.getEnProductName());
    map.put("containerType", sea.getContainerType());
    map.put("freight", sea.getFreight());
    map.put("freightValidDate", sea.getFreightValidDate());
    map.put("buc", sea.getBuc());
    map.put("bucValidDate", sea.getBucValidDate());
    map.put("ebs", sea.getEbs());
    map.put("ebsValidDate", sea.getEbsValidDate());
    map.put("gri", sea.getGri());
    map.put("griValidDate", sea.getGriValidDate());
    map.put("others", sea.getOthers());
    map.put("othersValidDate", sea.getOthersValidDate());
    map.put("allIn", sea.getAllIn());
    map.put("ssl", sea.getSsl());
    map.put("agent", sea.getAgent());
    map.put("remark", sea.getRemark());
    if (sea.getExtraFields() != null && !sea.getExtraFields().isEmpty()) {
      map.put("extraFields", sea.getExtraFields());
    }
    return map;
  }

  /** 字段名与成本库列表一致 */
  public static Map<String, Object> fumigationSnapshot(CostFumigation fum) {
    Map<String, Object> map = new HashMap<>();
    map.put("region", fum.getRegion());
    map.put("station", fum.getStation());
    map.put("outdoorNonOak", fum.getOutdoorNonOak());
    map.put("outdoorOak", fum.getOutdoorOak());
    map.put("outdoorValidity", fum.getOutdoorValidity());
    map.put("indoorNonOak", fum.getIndoorNonOak());
    map.put("indoorOak", fum.getIndoorOak());
    map.put("indoorValidity", fum.getIndoorValidity());
    map.put("address", fum.getAddress());
    map.put("updatedAt", fum.getUpdatedAt() != null ? fum.getUpdatedAt().toString() : null);
    if (fum.getExtraFields() != null && !fum.getExtraFields().isEmpty()) {
      map.put("extraFields", fum.getExtraFields());
    }
    return map;
  }
}
