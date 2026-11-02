package com.furuiduo.quote.quote.entity;

/** 触发方式。 */
public enum QuoteMailTrigger {
  /** 确认报价 */
  CONFIRM,
  /** 审批通过 */
  APPROVE,
  /** 变更保存 */
  REVISION,
  /** 人工重发 */
  RESEND,
  /** 人工指定邮箱发送 */
  MANUAL
}
