package com.furuiduo.quote.quote.support;

import java.time.LocalDate;

import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.entity.QuoteStatus;

public final class QuoteStatusSupport {

  private QuoteStatusSupport() {}

  /** 对外展示状态（兼容旧枚举） */
  public static String displayStatus(QuoteStatus status) {
    if (status == null) {
      return QuoteStatus.DRAFT.name();
    }
    return switch (status) {
      case PENDING -> QuoteStatus.FOLLOWING.name();
      case SENT -> QuoteStatus.EFFECTIVE.name();
      case LOST -> QuoteStatus.VOIDED.name();
      default -> status.name();
    };
  }

  public static boolean isExpired(QuoteOrder order) {
    if (order.getValidUntil() == null) {
      return false;
    }
    if (order.getStatus() == QuoteStatus.VOIDED || order.getStatus() == QuoteStatus.WON) {
      return false;
    }
    return order.getValidUntil().isBefore(LocalDate.now());
  }

  public static boolean isVoided(QuoteOrder order) {
    QuoteStatus status = order.getStatus();
    return status == QuoteStatus.VOIDED || status == QuoteStatus.LOST;
  }
}
