package com.furuiduo.quote.quote.entity;

public enum QuoteStatus {
  /** 草稿 */
  DRAFT,
  /** @deprecated 兼容旧数据，映射为 FOLLOWING */
  PENDING,
  /** @deprecated 兼容旧数据，映射为 EFFECTIVE */
  SENT,
  /** 已生效 */
  EFFECTIVE,
  /** 跟进中 */
  FOLLOWING,
  /** 已成交 */
  WON,
  /** @deprecated 兼容旧数据 */
  LOST,
  /** 已过期 */
  EXPIRED,
  /** 已作废 */
  VOIDED
}
