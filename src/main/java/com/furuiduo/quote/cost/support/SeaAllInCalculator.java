package com.furuiduo.quote.cost.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.furuiduo.quote.cost.entity.CostSea;

/** 海运 ALL IN = 运费 + BUC + EBS + GRI + OTHERS */
public final class SeaAllInCalculator {

  private SeaAllInCalculator() {}

  public static BigDecimal compute(CostSea entity) {
    if (entity == null) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    return nullToZero(entity.getFreight())
        .add(nullToZero(entity.getBuc()))
        .add(nullToZero(entity.getEbs()))
        .add(nullToZero(entity.getGri()))
        .add(nullToZero(entity.getOthers()))
        .setScale(2, RoundingMode.HALF_UP);
  }

  private static BigDecimal nullToZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
