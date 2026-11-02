package com.furuiduo.quote.quote.entity;

/** 未发信原因。 */
public enum QuoteMailSkipReason {
  /** quote.mail.enabled=false */
  MAIL_DISABLED,
  /** 报价单未关联询价单 */
  NO_INQUIRY,
  /** 询价单操作人未绑定邮箱 */
  NO_OPERATOR_EMAIL,
  /** 未配置发件邮箱 */
  NO_FROM
}
