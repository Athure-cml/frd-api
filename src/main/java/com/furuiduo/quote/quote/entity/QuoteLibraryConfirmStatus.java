package com.furuiduo.quote.quote.entity;

/** 报价库确认状态（与成本库 CostStatus 无关）。 */
public enum QuoteLibraryConfirmStatus {
  /** 待确认：新入库或费用已修改 */
  pendingConfirm,
  /** 已确认 */
  confirmed
}
