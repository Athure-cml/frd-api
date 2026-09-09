package com.furuiduo.quote.quote.entity;

public enum QuoteStatus {
  /** 草稿 */
  DRAFT,
  /** 待审批 */
  PENDING_APPROVAL,
  /** 已发送 */
  SENT,
  /** 已成交 */
  WON,
  /** 已拒绝（已放弃） */
  REJECTED,
  /** 已过期（已放弃） */
  EXPIRED,
  /** 已作废（已放弃） */
  VOIDED,
  /** @deprecated 兼容旧数据，映射为 PENDING_APPROVAL */
  PENDING,
  /** @deprecated 兼容旧数据，映射为 SENT */
  EFFECTIVE,
  /** @deprecated 兼容旧数据，映射为 SENT */
  FOLLOWING,
  /** @deprecated 兼容旧数据，映射为 REJECTED */
  LOST
}
