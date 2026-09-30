package com.furuiduo.quote.quote.entity;

public enum QuoteStatus {
  /** 草稿 */
  DRAFT,
  /** 待审批 */
  PENDING_APPROVAL,
  /** 已确认报价（对外展示；兼容旧「已发送」数据） */
  SENT,
  /** 已成交 */
  WON,
  /** 已拒绝（已放弃） */
  REJECTED,
  /** 已过期（兼容旧数据；新逻辑按有效期自动转为 VOIDED） */
  EXPIRED,
  /** 已作废（已放弃） */
  VOIDED,
  /** 变更进行中（已发起变更单，待新版本确认） */
  REVISING,
  /** 已被变更单替代（归档） */
  SUPERSEDED,
  /** @deprecated 兼容旧数据，映射为 PENDING_APPROVAL */
  PENDING,
  /** @deprecated 兼容旧数据，映射为 SENT */
  EFFECTIVE,
  /** @deprecated 兼容旧数据，映射为 SENT */
  FOLLOWING,
  /** @deprecated 兼容旧数据，映射为 REJECTED */
  LOST
}
