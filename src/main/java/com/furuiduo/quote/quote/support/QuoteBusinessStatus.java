package com.furuiduo.quote.quote.support;

/**
 * 报价单对外四态（列表/详情展示）。
 *
 * <p>库内可仍存历史枚举；展示与筛选统一走本枚举。
 *
 * <p>「已归档」是展示合集，细分原因见 {@code QuoteArchiveReason}：
 *
 * <ul>
 *   <li>SUPERSEDED — 变更确认后旧版归档
 *   <li>QUOTED_EXPIRED — 已报价过有效期
 *   <li>PENDING_EXPIRED — 待报价未确认就过有效期
 * </ul>
 *
 * <p>业务统计建议按 {@code rootQuoteId} 计「一单」：变更前后版本族只算一张已报价单据。
 */
public enum QuoteBusinessStatus {
  /** 待报价 */
  PENDING_QUOTE,
  /** 已报价 */
  QUOTED,
  /** 已作废 */
  VOIDED,
  /** 已归档（合集，详见 archiveReason） */
  ARCHIVED
}
