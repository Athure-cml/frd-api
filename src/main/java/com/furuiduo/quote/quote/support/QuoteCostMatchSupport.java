package com.furuiduo.quote.quote.support;

import java.util.List;
import java.util.Optional;

import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.entity.CostSea;
import com.furuiduo.quote.cost.entity.CostStatus;
import com.furuiduo.quote.cost.support.CostValidityStatus;

/** 报价单引入成本：仅匹配/允许「生效中」记录。 */
public final class QuoteCostMatchSupport {

  private QuoteCostMatchSupport() {}

  public static Optional<CostRoad> firstActiveRoad(List<CostRoad> items) {
    return items.stream().filter(QuoteCostMatchSupport::isActive).findFirst();
  }

  public static Optional<CostSea> firstActiveSea(List<CostSea> items) {
    return items.stream().filter(QuoteCostMatchSupport::isActive).findFirst();
  }

  public static Optional<CostFumigation> firstActiveFumigation(List<CostFumigation> items) {
    return items.stream().filter(QuoteCostMatchSupport::isActive).findFirst();
  }

  public static boolean isActive(CostRoad road) {
    return CostValidityStatus.resolveRoad(road.getStatus(), road.getExtraFields(), road.getValidDate())
        == CostStatus.active;
  }

  public static boolean isActive(CostSea sea) {
    return CostValidityStatus.resolve(sea.getStatus(), sea.getFreightValidDate())
        == CostStatus.active;
  }

  public static boolean isActive(CostFumigation fum) {
    return CostValidityStatus.resolve(
            fum.getStatus(), fum.getOutdoorValidity(), fum.getIndoorValidity())
        == CostStatus.active;
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
        .filter(QuoteCostMatchSupport::isActive)
        .findFirst();
  }

  /** 卡车成本匹配要求 city、state 均已填写。 */
  public static boolean hasRoadLocationKeys(String city, String state) {
    return city != null
        && !city.isBlank()
        && state != null
        && !state.isBlank();
  }

  /** 卡车 REMARK：优先 extraFields.cf_road_remark，否则 fallback remark 列。 */
  public static String resolveRoadRemark(CostRoad road) {
    if (road.getExtraFields() != null) {
      Object custom = road.getExtraFields().get("cf_road_remark");
      if (custom != null && !String.valueOf(custom).isBlank()) {
        return String.valueOf(custom).trim();
      }
    }
    if (road.getRemark() == null || road.getRemark().isBlank()) {
      return null;
    }
    return road.getRemark().trim();
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
    remark = snap.get("remark");
    if (remark != null && !String.valueOf(remark).isBlank()) {
      return String.valueOf(remark).trim();
    }
    return null;
  }
}
