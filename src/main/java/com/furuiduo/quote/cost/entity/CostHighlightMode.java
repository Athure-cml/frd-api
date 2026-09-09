package com.furuiduo.quote.cost.entity;

public enum CostHighlightMode {
  road,
  sea,
  fumigation;

  public static CostHighlightMode parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("cost mode required");
    }
    return CostHighlightMode.valueOf(raw.trim().toLowerCase());
  }
}
