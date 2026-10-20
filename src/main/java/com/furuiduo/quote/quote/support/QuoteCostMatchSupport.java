package com.furuiduo.quote.quote.support;

import java.math.BigDecimal;
import java.util.Comparator;
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
    return lowestAllInSea(items.stream().filter(QuoteCostMatchSupport::isImportable).toList());
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

  /**
   * 海运 POL 匹配：
   * <ul>
   *   <li>报价单未填 POL：不限制 POL，可仅按 POR+POD 匹配
   *   <li>已填 POL：成本库 POL 支持单值或 / 拼接多港
   * </ul>
   */
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

  /**
   * 在 POR/POD 已筛结果上按 POL 过滤，并取 ALL IN 最低的可引入海运成本。
   * 未填 POL 时等同仅按 POR+POD 匹配后取最低价。
   */
  public static Optional<CostSea> firstActiveSeaByPol(List<CostSea> items, String pol) {
    List<CostSea> candidates =
        items.stream()
            .filter(item -> polMatches(item.getPol(), pol))
            .filter(QuoteCostMatchSupport::isImportable)
            .toList();
    return lowestAllInSea(candidates);
  }

  /** ALL IN 为空视为最大，避免抢占有价记录；同价时保持原列表顺序中的第一条。 */
  private static Optional<CostSea> lowestAllInSea(List<CostSea> items) {
    return items.stream()
        .min(
            Comparator.comparing(
                QuoteCostMatchSupport::seaAllInOrMax, Comparator.naturalOrder()));
  }

  private static BigDecimal seaAllInOrMax(CostSea sea) {
    BigDecimal allIn = sea.getAllIn();
    return allIn != null ? allIn : BigDecimal.valueOf(Long.MAX_VALUE);
  }

  /**
   * 卡车报价库匹配前置条件：
   * <ul>
   *   <li>有 zip code：可匹配（有 city/state 时一并纳入）
   *   <li>无 zip code：city 与 state 都必须有
   * </ul>
   */
  public static boolean hasRoadLocationKeys(String zipCode, String city, String state) {
    if (isNotBlank(zipCode)) {
      return true;
    }
    return isNotBlank(city) && isNotBlank(state);
  }

  private static boolean isNotBlank(String value) {
    return value != null && !value.isBlank();
  }

  /** 熏蒸成本匹配要求已填写熏蒸点（对应成本库 STATION）。 */
  public static boolean hasFumigationStationKey(String fumigationPoint) {
    return fumigationPoint != null && !fumigationPoint.isBlank();
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
