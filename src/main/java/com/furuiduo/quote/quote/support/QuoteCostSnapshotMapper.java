package com.furuiduo.quote.quote.support;

import java.util.HashMap;
import java.util.Map;

import com.furuiduo.quote.cost.dto.FreightCostResponse;
import com.furuiduo.quote.cost.dto.FumigationCostResponse;
import com.furuiduo.quote.cost.dto.RoadCostResponse;
import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.entity.CostSea;
import com.furuiduo.quote.cost.support.CostValidityStatus;
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

  public static QuoteCostMatchItemDto fromLibraryRoad(
      RoadCostResponse row, Map<String, Object> keys) {
    return new QuoteCostMatchItemDto(
        QuoteCostType.ROAD.name(),
        row.id(),
        row.validDate(),
        keys,
        roadSnapshotFromResponse(row));
  }

  public static QuoteCostMatchItemDto fromLibrarySea(
      FreightCostResponse row, Map<String, Object> keys) {
    return new QuoteCostMatchItemDto(
        QuoteCostType.SEA.name(),
        row.id(),
        row.freightValidDate(),
        keys,
        seaSnapshotFromResponse(row));
  }

  public static QuoteCostMatchItemDto fromLibraryFumigation(
      FumigationCostResponse row, Map<String, Object> keys) {
    return new QuoteCostMatchItemDto(
        QuoteCostType.FUMIGATION.name(),
        row.id(),
        row.updatedAt(),
        keys,
        fumigationSnapshotFromResponse(row));
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
    if (road.getExtraFields() != null) {
      Object roadRemark = road.getExtraFields().get("cf_road_remark");
      if (roadRemark != null && !String.valueOf(roadRemark).isBlank()) {
        map.put("cf_road_remark", String.valueOf(roadRemark).trim());
      }
    }
    map.put("validDate", road.getValidDate());
    map.put("logYardNameAddress", road.getLogYardNameAddress());
    map.put(
        "status",
        CostValidityStatus.resolveRoad(
                road.getStatus(), road.getExtraFields(), road.getValidDate())
            .name());
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
    map.put(
        "status",
        CostValidityStatus.resolve(sea.getStatus(), sea.getFreightValidDate()).name());
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
    map.put(
        "status",
        CostValidityStatus.resolve(
                fum.getStatus(), fum.getOutdoorValidity(), fum.getIndoorValidity())
            .name());
    if (fum.getExtraFields() != null && !fum.getExtraFields().isEmpty()) {
      map.put("extraFields", fum.getExtraFields());
    }
    return map;
  }

  /** 报价库行快照：费用为规则 + 人工 override 后的展示值 */
  public static Map<String, Object> roadSnapshotFromResponse(RoadCostResponse row) {
    Map<String, Object> map = roadSnapshot(toRoadEntity(row));
    map.put("allInNoFm", row.allInNoFm());
    map.put("allInFmOneWay", row.allInFmOneWay());
    map.put("allInFmRound", row.allInFmRound());
    map.put("waitingFee", row.waitingFee());
    map.put("redelivery", row.redelivery());
    map.put("nsLift", row.nsLift());
    map.put("fromQuoteLibrary", true);
    if (row.updatedAt() != null) {
      map.put("updatedAt", row.updatedAt());
    }
    return map;
  }

  public static Map<String, Object> seaSnapshotFromResponse(FreightCostResponse row) {
    Map<String, Object> map = new HashMap<>();
    map.put("por", row.por());
    map.put("pol", row.pol());
    map.put("pod", row.pod());
    map.put("cnShortName", row.cnShortName());
    map.put("enProductName", row.enProductName());
    map.put("containerType", row.containerType());
    map.put("freight", row.freight());
    map.put("freightValidDate", row.freightValidDate());
    map.put("buc", row.buc());
    map.put("bucValidDate", row.bucValidDate());
    map.put("ebs", row.ebs());
    map.put("ebsValidDate", row.ebsValidDate());
    map.put("gri", row.gri());
    map.put("griValidDate", row.griValidDate());
    map.put("others", row.others());
    map.put("othersValidDate", row.othersValidDate());
    map.put("allIn", row.allIn());
    map.put("ofRateUsd", row.allIn());
    map.put("ssl", row.ssl());
    map.put("agent", row.agent());
    map.put("remark", row.remark());
    map.put("status", row.status() != null ? row.status().name() : null);
    map.put("fromQuoteLibrary", true);
    if (row.extraFields() != null && !row.extraFields().isEmpty()) {
      map.put("extraFields", row.extraFields());
    }
    if (row.updatedAt() != null) {
      map.put("updatedAt", row.updatedAt());
    }
    return map;
  }

  public static Map<String, Object> fumigationSnapshotFromResponse(FumigationCostResponse row) {
    Map<String, Object> map = new HashMap<>();
    map.put("region", row.region());
    map.put("station", row.station());
    map.put("outdoorNonOak", row.outdoorNonOak());
    map.put("outdoorOak", row.outdoorOak());
    map.put("outdoorValidity", row.outdoorValidity());
    map.put("indoorNonOak", row.indoorNonOak());
    map.put("indoorOak", row.indoorOak());
    map.put("indoorValidity", row.indoorValidity());
    map.put("address", row.address());
    map.put("updatedAt", row.updatedAt());
    map.put("status", row.status() != null ? row.status().name() : null);
    map.put("fromQuoteLibrary", true);
    if (row.extraFields() != null && !row.extraFields().isEmpty()) {
      map.put("extraFields", row.extraFields());
    }
    return map;
  }

  private static CostRoad toRoadEntity(RoadCostResponse row) {
    CostRoad road = new CostRoad();
    road.setId(row.id());
    road.setZipCode(row.zipCode());
    road.setCity(row.city());
    road.setState(row.state());
    road.setPor(row.por());
    road.setPol(row.pol());
    road.setSupplier(row.supplier());
    road.setBaseFreight(row.baseFreight());
    road.setFsc(row.fsc());
    road.setChassis(row.chassis());
    road.setTriTandemAxle(row.triTandemAxle());
    road.setSplit(row.split());
    road.setStopOff(row.stopOff());
    road.setAllInNoFm(row.allInNoFm());
    road.setAllInFmOneWay(row.allInFmOneWay());
    road.setAllInFmRound(row.allInFmRound());
    road.setWaitingFee(row.waitingFee());
    road.setRedelivery(row.redelivery());
    road.setPrepull(row.prepull());
    road.setNsLift(row.nsLift());
    road.setOtherFee(row.otherFee());
    road.setRemark(row.remark());
    road.setValidDate(row.validDate());
    road.setLogYardNameAddress(row.logYardNameAddress());
    road.setExtraFields(row.extraFields());
    return road;
  }
}
