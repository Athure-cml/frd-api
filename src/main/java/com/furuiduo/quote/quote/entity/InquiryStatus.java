package com.furuiduo.quote.quote.entity;

/** 询价单状态 */
public enum InquiryStatus {
  /** 待递交 */
  PENDING_SUBMIT,
  /** 待报价（已递交报价员，已生成报价单） */
  PENDING_QUOTE,
  /** 已报价 */
  QUOTED,
  /** 已作废 */
  VOIDED,
  /** 已过期（关联报价单过期，或有效期已过） */
  EXPIRED
}
