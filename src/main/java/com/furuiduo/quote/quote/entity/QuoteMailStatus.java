package com.furuiduo.quote.quote.entity;

/** 已报价发信状态。SKIPPED 表示未实际发信（配置或数据不满足）。 */
public enum QuoteMailStatus {
  SENDING,
  SENT,
  FAILED,
  SKIPPED
}
