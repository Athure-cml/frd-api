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
      EnumSet.of(QuoteStatus.DRAFT);

  private static final Set<QuoteStatus> OPEN_REVISION =
      EnumSet.of(QuoteStatus.DRAFT, QuoteStatus.PENDING_APPROVAL);

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

  public static QuoteStatus parseDisplayStatus(String displayStatus) {
    if (displayStatus == null || displayStatus.isBlank()) {
      return QuoteStatus.DRAFT;
    }
    try {
      return normalize(QuoteStatus.valueOf(displayStatus));
    } catch (IllegalArgumentException ex) {
      return QuoteStatus.DRAFT;
    }
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

  public static boolean isSuperseded(QuoteStatus status) {
    return normalize(status) == QuoteStatus.SUPERSEDED;
  }

  public static boolean isRevising(QuoteStatus status) {
    return normalize(status) == QuoteStatus.REVISING;
  }

  public static boolean isOpenRevisionStatus(QuoteStatus status) {
    return OPEN_REVISION.contains(normalize(status));
  }

  public static Long resolveRootQuoteId(QuoteOrder order) {
    if (order == null) {
      return null;
    }
    if (order.getRootQuoteId() != null) {
      return order.getRootQuoteId();
    }
    return order.getId();
  }

  public static String revisionLabel(Integer revisionNo) {
    if (revisionNo == null || revisionNo <= 0) {
      return null;
    }
    return "R" + revisionNo;
  }

  public static boolean isExpired(QuoteOrder order) {
    if (order.getValidUntil() == null) {
      return false;
    }
    QuoteStatus status = normalize(order.getStatus());
    if (isAbandoned(status) || status == QuoteStatus.WON
        || status == QuoteStatus.SUPERSEDED
        || status == QuoteStatus.REVISING) {
      return false;
    }
    return order.getValidUntil().isBefore(LocalDate.now());
  }

  /**
   * 超过有效期自动作废；历史 EXPIRED 一并转为 VOIDED。
   *
   * @return 是否变更了状态
   */
  public static boolean applyExpiredAsVoided(QuoteOrder order) {
    if (order == null) {
      return false;
    }
    QuoteStatus status = normalize(order.getStatus());
    boolean changed = false;
    if (status == QuoteStatus.EXPIRED) {
      order.setStatus(QuoteStatus.VOIDED);
      changed = true;
    } else if (order.getValidUntil() != null
        && order.getValidUntil().isBefore(LocalDate.now())
        && status != QuoteStatus.VOIDED
        && status != QuoteStatus.REJECTED
        && status != QuoteStatus.WON
        && status != QuoteStatus.SUPERSEDED
        && status != QuoteStatus.REVISING) {
      order.setStatus(QuoteStatus.VOIDED);
      changed = true;
    }
    if (changed) {
      order.setCostRiskActive(false);
      order.setCostRiskReason(null);
      order.setCostRiskAt(null);
    }
    return changed;
  }

  /** 列表/详情「已放弃」样式（拒绝、过期、作废） */
  public static boolean isVoided(QuoteOrder order) {
    return isAbandoned(order.getStatus());
  }
}
