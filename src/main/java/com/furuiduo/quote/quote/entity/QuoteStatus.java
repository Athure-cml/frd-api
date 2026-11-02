package com.furuiduo.quote.quote.entity;

public enum QuoteStatus {
  /** 草稿（兼容；展示为待报价） */
  DRAFT,
  /** 待审批（兼容；展示为待报价） */
  PENDING_APPROVAL,
  /** 已确认报价（兼容；展示为已报价） */
  SENT,
  /** 已成交（兼容；展示为已报价） */
  WON,
  /** 已拒绝（兼容；展示为已作废） */
  REJECTED,
  /** 已过期（兼容；展示/迁移为已归档） */
  EXPIRED,
  /** 已作废 */
  VOIDED,
  /** 变更进行中（兼容；展示为待报价） */
  REVISING,
  /** 已被变更单替代（兼容；展示/迁移为已归档） */
  SUPERSEDED,
  /** 待报价（新主状态） */
  PENDING_QUOTE,
  /** 已报价（新主状态） */
  QUOTED,
  /** 已归档（有效期到期 / 旧版变更单） */
  ARCHIVED,
  /** @deprecated 兼容旧数据，映射为 PENDING_APPROVAL */
  PENDING,
  /** @deprecated 兼容旧数据，映射为 SENT */
  EFFECTIVE,
  /** @deprecated 兼容旧数据，映射为 SENT */
  FOLLOWING,
  /** @deprecated 兼容旧数据，映射为 REJECTED */
  LOST
}
