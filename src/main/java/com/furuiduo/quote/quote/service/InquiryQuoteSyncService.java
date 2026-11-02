package com.furuiduo.quote.quote.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.quote.entity.InquiryOrder;
import com.furuiduo.quote.quote.entity.InquiryStatus;
import com.furuiduo.quote.quote.entity.QuoteArchiveReason;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.entity.QuoteStatus;
import com.furuiduo.quote.quote.repository.InquiryOrderRepository;
import com.furuiduo.quote.quote.repository.QuoteOrderRepository;
import com.furuiduo.quote.quote.support.QuoteStatusSupport;

/**
 * 报价单状态变更时同步关联询价单。
 *
 * <p>历史报价无 inquiry_id 时不处理。
 */
@Service
public class InquiryQuoteSyncService {

  private final InquiryOrderRepository inquiryOrderRepository;
  private final QuoteOrderRepository quoteOrderRepository;

  public InquiryQuoteSyncService(
      InquiryOrderRepository inquiryOrderRepository, QuoteOrderRepository quoteOrderRepository) {
    this.inquiryOrderRepository = inquiryOrderRepository;
    this.quoteOrderRepository = quoteOrderRepository;
  }

  /** 列表入口：先归档过期报价，再同步询价为已过期。 */
  @Transactional
  public void syncExpiredQuotesAndInquiries() {
    quoteOrderRepository.archiveQuotesPastValidUntil();
    inquiryOrderRepository.expireLinkedToExpiredQuotes();
    inquiryOrderRepository.expireByOwnValidUntil();
  }

  @Transactional
  public void onQuoteConfirmed(QuoteOrder quote) {
    InquiryOrder inquiry = resolveInquiry(quote);
    if (inquiry == null) {
      return;
    }
    if (inquiry.getStatus() == InquiryStatus.VOIDED) {
      return;
    }
    inquiry.setStatus(InquiryStatus.QUOTED);
    // 询价始终挂首次递交生成的原单（版本族根单）；变更确认不改写指向
    Long rootId = QuoteStatusSupport.resolveRootQuoteId(quote);
    boolean isRevision =
        quote.getParentQuoteId() != null
            || (quote.getRevisionNo() != null && quote.getRevisionNo() > 0);
    if (inquiry.getQuoteId() == null) {
      Long bindId = isRevision && rootId != null ? rootId : quote.getId();
      if (bindId != null) {
        inquiry.setQuoteId(bindId);
      }
    } else if (
        isRevision
            && rootId != null
            && quote.getId() != null
            && quote.getId().equals(inquiry.getQuoteId())) {
      // 历史误挂到变更新单时，拨回根单
      inquiry.setQuoteId(rootId);
    }
    syncValidUntilOnto(inquiry, quote);
    markExpiredIfPastValidUntil(inquiry);
    inquiry.touch();
    inquiryOrderRepository.save(inquiry);
  }

  @Transactional
  public void onQuoteVoided(QuoteOrder quote) {
    InquiryOrder inquiry = resolveInquiry(quote);
    if (inquiry == null) {
      return;
    }
    inquiry.setStatus(InquiryStatus.VOIDED);
    inquiry.touch();
    inquiryOrderRepository.save(inquiry);
  }

  /**
   * 报价单因有效期归档后：关联询价同步为已过期。
   *
   * <p>已作废询价不覆盖。
   */
  @Transactional
  public void onQuoteExpired(QuoteOrder quote) {
    InquiryOrder inquiry = resolveInquiry(quote);
    if (inquiry == null) {
      return;
    }
    if (inquiry.getStatus() == InquiryStatus.VOIDED
        || inquiry.getStatus() == InquiryStatus.EXPIRED) {
      syncValidUntilOnto(inquiry, quote);
      if (inquiry.getValidUntil() != null) {
        inquiry.touch();
        inquiryOrderRepository.save(inquiry);
      }
      return;
    }
    if (inquiry.getStatus() != InquiryStatus.PENDING_QUOTE
        && inquiry.getStatus() != InquiryStatus.QUOTED) {
      return;
    }
    inquiry.setStatus(InquiryStatus.EXPIRED);
    syncValidUntilOnto(inquiry, quote);
    inquiry.touch();
    inquiryOrderRepository.save(inquiry);
  }

  /**
   * 报价单填写有效期后回填询价单（询价常不填有效期）。
   *
   * <p>仅在报价侧有有效期时写入；已有相同日期则跳过。
   */
  @Transactional
  public void syncValidUntilFromQuote(QuoteOrder quote) {
    InquiryOrder inquiry = resolveInquiry(quote);
    if (inquiry == null) {
      return;
    }
    boolean changed = syncValidUntilOnto(inquiry, quote);
    changed = markExpiredIfPastValidUntil(inquiry) || changed;
    if (!changed) {
      return;
    }
    inquiry.touch();
    inquiryOrderRepository.save(inquiry);
  }

  /**
   * 待报价退回：取消关联报价单后，询价单回到待递交，可再次递交生成新报价单。
   *
   * <p>变更草稿退回不影响询价单（询价仍对应已报价主单）。
   */
  @Transactional
  public void onQuoteReturned(QuoteOrder quote) {
    if (quote == null) {
      return;
    }
    // 变更草稿：只取消草稿本身，不回退询价单
    if (quote.getParentQuoteId() != null
        || (quote.getRevisionNo() != null && quote.getRevisionNo() > 0)) {
      return;
    }
    InquiryOrder inquiry = resolveInquiry(quote);
    if (inquiry == null) {
      return;
    }
    if (inquiry.getStatus() != InquiryStatus.PENDING_QUOTE) {
      return;
    }
    inquiry.setStatus(InquiryStatus.PENDING_SUBMIT);
    inquiry.setQuoteId(null);
    inquiry.setSubmittedAt(null);
    inquiry.touch();
    inquiryOrderRepository.save(inquiry);
  }

  /** @return 是否改写了有效期 */
  private boolean syncValidUntilOnto(InquiryOrder inquiry, QuoteOrder quote) {
    if (inquiry == null || quote == null || quote.getValidUntil() == null) {
      return false;
    }
    if (quote.getValidUntil().equals(inquiry.getValidUntil())) {
      return false;
    }
    // 询价未填，或报价更新了有效期：以报价为准回填
    inquiry.setValidUntil(quote.getValidUntil());
    return true;
  }

  /** 有效期已过且仍为待报价/已报价时，立即置为已过期 */
  private boolean markExpiredIfPastValidUntil(InquiryOrder inquiry) {
    if (inquiry == null || inquiry.getValidUntil() == null) {
      return false;
    }
    if (inquiry.getStatus() != InquiryStatus.PENDING_QUOTE
        && inquiry.getStatus() != InquiryStatus.QUOTED) {
      return false;
    }
    if (!inquiry.getValidUntil().isBefore(LocalDate.now())) {
      return false;
    }
    inquiry.setStatus(InquiryStatus.EXPIRED);
    return true;
  }

  private InquiryOrder resolveInquiry(QuoteOrder quote) {
    if (quote == null) {
      return null;
    }
    if (quote.getInquiryId() != null) {
      return inquiryOrderRepository.findByIdAndDeletedAtIsNull(quote.getInquiryId()).orElse(null);
    }
    if (quote.getId() != null) {
      return inquiryOrderRepository.findByQuoteIdAndDeletedAtIsNull(quote.getId()).orElse(null);
    }
    return null;
  }

  /** 判断报价是否属于「过期归档」语义（用于单条懒归档后同步）。 */
  public static boolean isExpiredArchive(QuoteOrder quote) {
    if (quote == null) {
      return false;
    }
    QuoteStatus status = QuoteStatusSupport.normalize(quote.getStatus());
    if (status != QuoteStatus.ARCHIVED) {
      return false;
    }
    QuoteArchiveReason reason = quote.getArchiveReason();
    return reason == QuoteArchiveReason.QUOTED_EXPIRED
        || reason == QuoteArchiveReason.PENDING_EXPIRED;
  }
}
