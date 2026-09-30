package com.furuiduo.quote.quote.entity;

public enum QuoteServiceType {
  SEA,
  FUMIGATION,
  TRUCK,
  INSURANCE,
  TRADE,
  OTHER;

  public String displayName() {
    return switch (this) {
      case SEA -> "海运";
      case FUMIGATION -> "熏蒸";
      case TRUCK -> "卡车";
      case INSURANCE -> "保险";
      case TRADE -> "贸易";
      case OTHER -> "其他";
    };
  }
}
