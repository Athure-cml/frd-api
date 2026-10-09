package com.furuiduo.quote.quote.support;

import java.util.List;
import java.util.Optional;

import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.entity.CostSea;
import com.furuiduo.quote.cost.entity.CostStatus;
import com.furuiduo.quote.cost.support.CostValidityStatus;

/** 报价单引入成本：允许「生效中 / 未生效」，排除已过期。 */
public final class QuoteCostMatchSupport {

  private QuoteCostMatchSupport() {}

  public static Optional<CostRoad> firstActiveRoad(List<CostRoad> items) {
    return items.stream().filter(QuoteCostMatchSupport::isImportable).findFirst();
  }

  public static Optional<CostSea> firstActiveSea(List<CostSea> items) {
    return items.stream().filter(QuoteCostMatchSupport::isImportable).findFirst();
  }

  public static Optional<CostFumigation> firstActiveFumigation(List<CostFumigation> items) {
    return items.stream().filter(QuoteCostMatchSupport::isImportable).findFirst();
  }

  public static boolean isActive(CostRoad road) {
    return isImportable(road);
  }

  public static boolean isActive(CostSea sea) {
    return isImportable(sea);
  }

  public static boolean isActive(CostFumigation fum) {
    return isImportable(fum);
  }

  public static boolean isImportable(CostRoad road) {
    CostStatus status =
        CostValidityStatus.resolveRoad(road.getStatus(), road.getExtraFields(), road.getValidDate());
    return status == CostStatus.active || status == CostStatus.pending;
  }

  public static boolean isImportable(CostSea sea) {
    CostStatus status = CostValidityStatus.resolve(sea.getStatus(), sea.getFreightValidDate());
    return status == CostStatus.active || status == CostStatus.pending;
  }

  public static boolean isImportable(CostFumigation fum) {
    CostStatus status =
        CostValidityStatus.resolve(
            fum.getStatus(), fum.getOutdoorValidity(), fum.getIndoorValidity());
    return status == CostStatus.active || status == CostStatus.pending;
  }

  /** 海运 POL 支持单值或 / 拼接多港。 */
  public static boolean polMatches(String recordPol, String searchPol) {
    if (searchPol == null || searchPol.isBlank()) {
      return true;
    }
    if (recordPol == null || recordPol.isBlank()) {
      return false;
    }
    String needle = searchPol.trim();
    for (String part : recordPol.split("/")) {
      if (part.trim().equalsIgnoreCase(needle)) {
        return true;
      }
    }
    return recordPol.trim().equalsIgnoreCase(needle);
  }

  public static Optional<CostSea> firstActiveSeaByPol(List<CostSea> items, String pol) {
    return items.stream()
        .filter(item -> polMatches(item.getPol(), pol))
        .filter(QuoteCostMatchSupport::isImportable)
        .findFirst();
  }

  /** 卡车成本匹配要求 city、state 均已填写。 */
  public static boolean hasRoadLocationKeys(String city, String state) {
    return city != null
        && !city.isBlank()
        && state != null
        && !state.isBlank();
  }

  /** 卡车 REMARK：仅取业务表头 REMARK（extraFields.cf_road_remark），不回退操作备注。 */
  public static String resolveRoadRemark(CostRoad road) {
    if (road == null || road.getExtraFields() == null) {
      return null;
    }
    Object custom = road.getExtraFields().get("cf_road_remark");
    if (custom != null && !String.valueOf(custom).isBlank()) {
      return String.valueOf(custom).trim();
    }
    return null;
  }

  public static String resolveRoadRemarkFromSnapshot(java.util.Map<String, Object> snap) {
    if (snap == null) {
      return null;
    }
    Object extra = snap.get("extraFields");
    if (extra instanceof java.util.Map<?, ?> extraMap) {
      Object custom = extraMap.get("cf_road_remark");
      if (custom != null && !String.valueOf(custom).isBlank()) {
        return String.valueOf(custom).trim();
      }
    }
    Object remark = snap.get("cf_road_remark");
    if (remark != null && !String.valueOf(remark).isBlank()) {
      return String.valueOf(remark).trim();
    }
    return null;
  }
}
