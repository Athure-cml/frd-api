package com.furuiduo.quote.quote.entity;

/**
 * 已归档原因（对外「已归档」是合集，统计需按原因拆分）。
 *
 * <ul>
 *   <li>{@link #SUPERSEDED} — 变更确认后，旧版本被新版本替代
 *   <li>{@link #QUOTED_EXPIRED} — 已报价后超过有效期
 *   <li>{@link #PENDING_EXPIRED} — 待报价（含草稿/变更草稿）未确认就超过有效期
 * </ul>
 */
public enum QuoteArchiveReason {
  /** 变更替换归档 */
  SUPERSEDED,
  /** 已报价过期归档 */
  QUOTED_EXPIRED,
  /** 待报价过期归档 */
  PENDING_EXPIRED
}
