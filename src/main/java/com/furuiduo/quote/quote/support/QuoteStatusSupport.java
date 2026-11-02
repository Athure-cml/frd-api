package com.furuiduo.quote.quote.support;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import com.furuiduo.quote.quote.entity.QuoteArchiveReason;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.entity.QuoteStatus;

public final class QuoteStatusSupport {

  private static final Set<QuoteStatus> ABANDONED =
      EnumSet.of(
          QuoteStatus.REJECTED,
          QuoteStatus.EXPIRED,
          QuoteStatus.VOIDED,
          QuoteStatus.LOST);

  private static final Set<QuoteStatus> EDITABLE =
      EnumSet.of(QuoteStatus.DRAFT, QuoteStatus.PENDING_QUOTE);

  private static final Set<QuoteStatus> DELETABLE =
      EnumSet.of(QuoteStatus.DRAFT, QuoteStatus.PENDING_QUOTE);

  private static final Set<QuoteStatus> OPEN_REVISION =
      EnumSet.of(
          QuoteStatus.DRAFT,
          QuoteStatus.PENDING_APPROVAL,
          QuoteStatus.PENDING_QUOTE);

  private QuoteStatusSupport() {}

  /** 对外展示状态码（四态） */
  public static String displayStatus(QuoteStatus status) {
    return toBusinessStatus(status, null).name();
  }

  public static String displayStatus(QuoteOrder order) {
    if (order == null) {
      return QuoteBusinessStatus.PENDING_QUOTE.name();
    }
    return toBusinessStatus(order.getStatus(), order).name();
  }

  public static QuoteBusinessStatus toBusinessStatus(QuoteStatus status, QuoteOrder order) {
    QuoteStatus normalized = normalize(status);
    if (normalized == QuoteStatus.ARCHIVED
        || normalized == QuoteStatus.SUPERSEDED
        || normalized == QuoteStatus.EXPIRED) {
      return QuoteBusinessStatus.ARCHIVED;
    }
    if (order != null && shouldAutoArchive(order, normalized)) {
      return QuoteBusinessStatus.ARCHIVED;
    }
    return switch (normalized) {
      case VOIDED, REJECTED, LOST -> QuoteBusinessStatus.VOIDED;
      case SENT, QUOTED, WON, EFFECTIVE, FOLLOWING -> QuoteBusinessStatus.QUOTED;
      case DRAFT, PENDING_APPROVAL, PENDING, PENDING_QUOTE -> QuoteBusinessStatus.PENDING_QUOTE;
      case REVISING, SUPERSEDED, EXPIRED, ARCHIVED -> QuoteBusinessStatus.ARCHIVED;
      default -> QuoteBusinessStatus.PENDING_QUOTE;
    };
  }

  /**
   * 有效期已过 → 应归档。
   *
   * <p>含「待报价 / 已报价」；排除成交、变更中（父单）、已作废、已归档。
   * 待报价过期也会归档，统计用 {@link QuoteArchiveReason#PENDING_EXPIRED} 区分。
   */
  public static boolean shouldAutoArchive(QuoteOrder order, QuoteStatus normalized) {
    if (order == null || order.getValidUntil() == null) {
      return false;
    }
    if (!order.getValidUntil().isBefore(LocalDate.now())) {
      return false;
    }
    if (normalized == QuoteStatus.WON
        || normalized == QuoteStatus.REVISING
        || normalized == QuoteStatus.VOIDED
        || normalized == QuoteStatus.REJECTED
        || normalized == QuoteStatus.ARCHIVED
        || normalized == QuoteStatus.SUPERSEDED
        || normalized == QuoteStatus.EXPIRED) {
      return false;
    }
    return true;
  }

  /** 按归档前状态推断过期归档原因 */
  public static QuoteArchiveReason resolveExpireArchiveReason(QuoteStatus beforeStatus) {
    QuoteStatus normalized = normalize(beforeStatus);
    return switch (normalized) {
      case SENT, QUOTED, WON, EFFECTIVE, FOLLOWING -> QuoteArchiveReason.QUOTED_EXPIRED;
      default -> QuoteArchiveReason.PENDING_EXPIRED;
    };
  }

  public static QuoteStatus parseDisplayStatus(String displayStatus) {
    if (displayStatus == null || displayStatus.isBlank()) {
      return QuoteStatus.PENDING_QUOTE;
    }
    String raw = displayStatus.trim();
    try {
      QuoteBusinessStatus business = QuoteBusinessStatus.valueOf(raw);
      return switch (business) {
        case PENDING_QUOTE -> QuoteStatus.PENDING_QUOTE;
        case QUOTED -> QuoteStatus.QUOTED;
        case VOIDED -> QuoteStatus.VOIDED;
        case ARCHIVED -> QuoteStatus.ARCHIVED;
      };
    } catch (IllegalArgumentException ignored) {
      // fall through
    }
    try {
      return normalize(QuoteStatus.valueOf(raw));
    } catch (IllegalArgumentException ex) {
      return QuoteStatus.PENDING_QUOTE;
    }
  }

  /**
   * 列表筛选：业务四态展开为库内可能状态集合；空/空白返回 null（表示不按状态过滤）。
   */
  public static Set<QuoteStatus> statusesForBusinessFilter(String displayStatus) {
    if (displayStatus == null || displayStatus.isBlank()) {
      return null;
    }
    String raw = displayStatus.trim();
    QuoteBusinessStatus business;
    try {
      business = QuoteBusinessStatus.valueOf(raw);
    } catch (IllegalArgumentException ex) {
      QuoteStatus normalized = parseDisplayStatus(raw);
      business = toBusinessStatus(normalized, null);
    }
    return switch (business) {
      case PENDING_QUOTE ->
          EnumSet.of(
              QuoteStatus.DRAFT,
              QuoteStatus.PENDING,
              QuoteStatus.PENDING_APPROVAL,
              QuoteStatus.PENDING_QUOTE);
      case QUOTED ->
          EnumSet.of(
              QuoteStatus.SENT,
              QuoteStatus.QUOTED,
              QuoteStatus.WON,
              QuoteStatus.EFFECTIVE,
              QuoteStatus.FOLLOWING);
      case VOIDED -> EnumSet.of(QuoteStatus.VOIDED, QuoteStatus.REJECTED, QuoteStatus.LOST);
      case ARCHIVED ->
          EnumSet.of(
              QuoteStatus.ARCHIVED,
              QuoteStatus.SUPERSEDED,
              QuoteStatus.EXPIRED,
              QuoteStatus.REVISING);
    };
  }

  public static QuoteStatus normalize(QuoteStatus status) {
    if (status == null) {
      return QuoteStatus.PENDING_QUOTE;
    }
    return switch (status) {
      case PENDING -> QuoteStatus.PENDING_APPROVAL;
      case EFFECTIVE, FOLLOWING -> QuoteStatus.SENT;
      case LOST -> QuoteStatus.REJECTED;
      default -> status;
    };
  }

  /** 工作流内部：是否仍按「草稿/待报价」可编辑 */
  public static boolean isEditable(QuoteStatus status) {
    QuoteStatus normalized = normalize(status);
    return EDITABLE.contains(normalized)
        || normalized == QuoteStatus.PENDING_APPROVAL;
  }

  public static boolean isDeletable(QuoteStatus status) {
    QuoteStatus normalized = normalize(status);
    return DELETABLE.contains(normalized);
  }

  public static boolean isAbandoned(QuoteStatus status) {
    QuoteBusinessStatus business = toBusinessStatus(status, null);
    return business == QuoteBusinessStatus.VOIDED
        || business == QuoteBusinessStatus.ARCHIVED;
  }

  public static boolean isSuperseded(QuoteStatus status) {
    QuoteStatus normalized = normalize(status);
    return normalized == QuoteStatus.SUPERSEDED || normalized == QuoteStatus.ARCHIVED;
  }

  public static boolean isRevising(QuoteStatus status) {
    return normalize(status) == QuoteStatus.REVISING;
  }

  public static boolean isOpenRevisionStatus(QuoteStatus status) {
    return OPEN_REVISION.contains(normalize(status));
  }

  public static boolean isQuotedLike(QuoteStatus status) {
    QuoteStatus normalized = normalize(status);
    return normalized == QuoteStatus.SENT
        || normalized == QuoteStatus.QUOTED
        || normalized == QuoteStatus.WON;
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
    return shouldAutoArchive(order, normalize(order.getStatus()));
  }

  /**
   * 超过有效期或历史 EXPIRED/SUPERSEDED → 已归档。
   *
   * @return 是否变更了状态
   */
  public static boolean applyExpiredAsArchived(QuoteOrder order) {
    if (order == null) {
      return false;
    }
    QuoteStatus status = normalize(order.getStatus());
    boolean changed = false;
    if (status == QuoteStatus.EXPIRED
        || status == QuoteStatus.SUPERSEDED
        || status == QuoteStatus.REVISING) {
      order.setStatus(QuoteStatus.ARCHIVED);
      if (order.getArchiveReason() == null) {
        order.setArchiveReason(
            status == QuoteStatus.EXPIRED
                ? QuoteArchiveReason.QUOTED_EXPIRED
                : QuoteArchiveReason.SUPERSEDED);
      }
      changed = true;
    } else if (shouldAutoArchive(order, status)) {
      order.setArchiveReason(resolveExpireArchiveReason(status));
      order.setStatus(QuoteStatus.ARCHIVED);
      changed = true;
    }
    if (changed) {
      order.setCostRiskActive(false);
      order.setCostRiskReason(null);
      order.setCostRiskAt(null);
    }
    return changed;
  }

  /** @deprecated 使用 {@link #applyExpiredAsArchived(QuoteOrder)} */
  @Deprecated
  public static boolean applyExpiredAsVoided(QuoteOrder order) {
    return applyExpiredAsArchived(order);
  }

  public static boolean isVoided(QuoteOrder order) {
    return toBusinessStatus(order.getStatus(), order) == QuoteBusinessStatus.VOIDED;
  }
}
