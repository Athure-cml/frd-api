package com.furuiduo.quote.sys.entity;

public enum AnnouncementDisplayType {
  MODAL,
  TICKER,
  BOTH;

  public boolean showsModal() {
    return this == MODAL || this == BOTH;
  }

  public boolean showsTicker() {
    return this == TICKER || this == BOTH;
  }
}
