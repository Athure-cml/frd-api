package com.furuiduo.quote.quote.support;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.entity.QuoteStatus;

public final class QuoteStatusSupport {

  private static final Set<QuoteStatus> ABANDONED =
      EnumSet.of(
          QuoteStatus.REJECTED,
          QuoteStatus.EXPIRED,
          QuoteStatus.VOIDED,
          QuoteStatus.LOST);

  private static final Set<QuoteStatus> EDITABLE = EnumSet.of(QuoteStatus.DRAFT);

  private static final Set<QuoteStatus> DELETABLE =
      EnumSet.of(QuoteStatus.DRAFT, QuoteStatus.VOIDED);

  private QuoteStatusSupport() {}

  /** 对外展示状态（兼容旧枚举） */
  public static String displayStatus(QuoteStatus status) {
    if (status == null) {
      return QuoteStatus.DRAFT.name();
    }
    return switch (status) {
      case PENDING -> QuoteStatus.PENDING_APPROVAL.name();
      case EFFECTIVE, FOLLOWING -> QuoteStatus.SENT.name();
      case LOST -> QuoteStatus.REJECTED.name();
      default -> status.name();
    };
  }

  public static QuoteStatus normalize(QuoteStatus status) {
    if (status == null) {
      return QuoteStatus.DRAFT;
    }
    return switch (status) {
      case PENDING -> QuoteStatus.PENDING_APPROVAL;
      case EFFECTIVE, FOLLOWING -> QuoteStatus.SENT;
      case LOST -> QuoteStatus.REJECTED;
      default -> status;
    };
  }

  public static boolean isEditable(QuoteStatus status) {
    return EDITABLE.contains(normalize(status));
  }

  public static boolean isDeletable(QuoteStatus status) {
    return DELETABLE.contains(normalize(status));
  }

  public static boolean isAbandoned(QuoteStatus status) {
    QuoteStatus normalized = normalize(status);
    return ABANDONED.contains(normalized);
  }

  public static boolean isExpired(QuoteOrder order) {
    if (order.getValidUntil() == null) {
      return false;
    }
    QuoteStatus status = normalize(order.getStatus());
    if (isAbandoned(status) || status == QuoteStatus.WON) {
      return false;
    }
    return order.getValidUntil().isBefore(LocalDate.now());
  }

  /** 列表/详情「已放弃」样式（拒绝、过期、作废） */
  public static boolean isVoided(QuoteOrder order) {
    return isAbandoned(order.getStatus());
  }
}
