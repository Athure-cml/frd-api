package com.furuiduo.quote.quoterule;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 海运费：舍弃小数部分，再向上取整到 10 的倍数（如 1490.1→1490，1491.6→1500）。 */
public final class OceanFreightRounding {

  private static final BigDecimal STEP = new BigDecimal("10");

  private OceanFreightRounding() {}

  public static BigDecimal apply(BigDecimal value) {
    if (value == null) {
      return null;
    }
    BigDecimal truncated = value.setScale(0, RoundingMode.DOWN);
    return truncated.divide(STEP, 0, RoundingMode.CEILING).multiply(STEP);
  }
}
