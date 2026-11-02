package com.furuiduo.quote.quote.entity;

/** 发信失败阶段。 */
public enum QuoteMailStage {
  /** 前置校验（收件人、询价单等） */
  PREPARE,
  /** 打印模板渲染 PDF 失败 */
  PDF,
  /** SMTP 提交失败 */
  SMTP
}
