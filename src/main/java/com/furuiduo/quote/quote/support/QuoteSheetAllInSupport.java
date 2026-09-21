package com.furuiduo.quote.quote.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import com.furuiduo.quote.quote.entity.QuoteOrder;

/** 报价单业务表 ALL IN：汇总 sheet 上可解析的美元费用项。 */
public final class QuoteSheetAllInSupport {

  private QuoteSheetAllInSupport() {}

  public static BigDecimal computeAllIn(QuoteOrder order) {
    if (order == null) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    BigDecimal total = BigDecimal.ZERO;
    total = add(total, parseUsdAmount(order.getOfUsd()));

    if (isFumigationEnabled(order)) {
      total = add(total, order.getTruckingNonOakUsd());
      total = add(total, order.getTruckingOakUsd());
      total = add(total, order.getFmNonOak());
      total = add(total, order.getFmOak());
    } else {
      total = add(total, defaultTruckingFee(order));
    }

    total = add(total, parseUsdAmount(order.getDocUsd()));
    total = add(total, parseUsdAmount(order.getCargoInsurancePremium()));
    total = add(total, parseUsdAmount(order.getCargoAgentFee()));
    total = add(total, order.getNsLift());
    total = add(total, order.getChassis());
    total = add(total, order.getWaiting());
    total = add(total, order.getRedeliveryFee());

    return total.setScale(2, RoundingMode.HALF_UP);
  }

  private static boolean isFumigationEnabled(QuoteOrder order) {
    if (Boolean.TRUE.equals(order.getFumigationEnabled())) {
      return true;
    }
    String point = order.getFumigationPoint();
    return point != null && !point.isBlank();
  }

  private static BigDecimal defaultTruckingFee(QuoteOrder order) {
    if (order.getTruckingFee() != null) {
      return order.getTruckingFee();
    }
    return order.getTruckingNonOakUsd();
  }

  private static BigDecimal add(BigDecimal total, BigDecimal value) {
    if (value == null) {
      return total;
    }
    return total.add(value);
  }

  /** 解析 US$ / 千分位；含 CIF 公式占位则跳过。 */
  static BigDecimal parseUsdAmount(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String text = raw.trim();
    if (text.toUpperCase(Locale.ROOT).contains("CIF")) {
      return null;
    }
    text = text.replaceAll("(?i)US\\$", "").replace(",", "").trim();
    if (text.isEmpty()) {
      return null;
    }
    try {
      return new BigDecimal(text);
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
