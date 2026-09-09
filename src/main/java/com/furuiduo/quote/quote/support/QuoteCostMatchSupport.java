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
}
