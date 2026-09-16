package com.furuiduo.quote.quote.support;

import com.furuiduo.quote.quote.dto.QuoteGenerateSheetRequest;
import com.furuiduo.quote.quote.dto.QuoteMatchCostsRequest;

/** 报价单成本匹配键解析。 */
public final class QuoteCostMatchKeys {

  private QuoteCostMatchKeys() {}

  /** 熏蒸点：对应熏蒸成本库 STATION。 */
  public static String fumigationStation(QuoteMatchCostsRequest request) {
    return trimToNull(request.fumigationPoint());
  }

  public static String fumigationStation(QuoteGenerateSheetRequest request) {
    return trimToNull(request.fumigationPoint());
  }

  public static String seaPor(QuoteMatchCostsRequest request) {
    return trimToNull(request.por());
  }

  public static String seaPol(QuoteMatchCostsRequest request) {
    return trimToNull(request.pol());
  }

  public static String seaPor(QuoteGenerateSheetRequest request) {
    return trimToNull(request.por());
  }

  public static String seaPol(QuoteGenerateSheetRequest request) {
    return trimToNull(request.pol());
  }

  private static String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
