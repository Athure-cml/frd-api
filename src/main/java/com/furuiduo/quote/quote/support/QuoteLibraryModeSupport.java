package com.furuiduo.quote.quote.support;

import com.furuiduo.quote.cost.entity.CostHighlightMode;
import com.furuiduo.quote.quote.entity.QuoteCostType;

public final class QuoteLibraryModeSupport {

  private QuoteLibraryModeSupport() {}

  public static CostHighlightMode toMode(QuoteCostType type) {
    return switch (type) {
      case ROAD -> CostHighlightMode.road;
      case SEA -> CostHighlightMode.sea;
      case FUMIGATION -> CostHighlightMode.fumigation;
    };
  }

  public static QuoteCostType toCostType(CostHighlightMode mode) {
    return switch (mode) {
      case road -> QuoteCostType.ROAD;
      case sea -> QuoteCostType.SEA;
      case fumigation -> QuoteCostType.FUMIGATION;
    };
  }
}
