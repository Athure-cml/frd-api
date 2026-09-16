package com.furuiduo.quote.quoterule;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 卡车费：舍弃小数部分，再向上取整到 5 的倍数（如 1495.6→1495，1491.6→1495，1496.6→1500）。 */
public final class TruckingFeeRounding {

  private static final BigDecimal STEP = new BigDecimal("5");

  private TruckingFeeRounding() {}

  public static BigDecimal apply(BigDecimal value) {
    if (value == null) {
      return null;
    }
    BigDecimal truncated = value.setScale(0, RoundingMode.DOWN);
    return truncated.divide(STEP, 0, RoundingMode.CEILING).multiply(STEP);
  }
}
