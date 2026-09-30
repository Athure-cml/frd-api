package com.furuiduo.quote.quote.support;

import java.math.BigDecimal;

import com.furuiduo.quote.quote.entity.QuoteOakType;

/** 卡车费取值：不熏蒸 ALL IN；熏蒸后按 OAK / NON-OAK 取对应 ALL IN。 */
public final class QuoteRoadAllInSupport {

  private QuoteRoadAllInSupport() {}

  public static QuoteOakType parseOakType(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return QuoteOakType.valueOf(raw.trim().toUpperCase().replace('-', '_'));
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  public static BigDecimal pick(
      BigDecimal allInNoFm,
      BigDecimal allInNonOak,
      BigDecimal allInOak,
      boolean fumigationEnabled,
      String oakType) {
    return pick(allInNoFm, allInNonOak, allInOak, fumigationEnabled, parseOakType(oakType));
  }

  public static BigDecimal pick(
      BigDecimal allInNoFm,
      BigDecimal allInNonOak,
      BigDecimal allInOak,
      boolean fumigationEnabled,
      QuoteOakType oakType) {
    if (!fumigationEnabled) {
      return allInNoFm;
    }
    if (oakType == QuoteOakType.OAK) {
      return allInOak;
    }
    if (oakType == QuoteOakType.NON_OAK) {
      return allInNonOak;
    }
    return null;
  }
}
