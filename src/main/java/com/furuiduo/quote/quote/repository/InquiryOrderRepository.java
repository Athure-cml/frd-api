package com.furuiduo.quote.quote.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.quote.entity.InquiryOrder;
import com.furuiduo.quote.quote.entity.InquiryStatus;

public interface InquiryOrderRepository extends JpaRepository<InquiryOrder, Long> {

  Optional<InquiryOrder> findByIdAndDeletedAtIsNull(Long id);

  Optional<InquiryOrder> findByQuoteIdAndDeletedAtIsNull(Long quoteId);

  @Query(
      """
      SELECT i.inquiryNo FROM InquiryOrder i
      WHERE i.inquiryNo LIKE :prefix
      ORDER BY i.inquiryNo DESC
      """)
  Page<String> findInquiryNosByPrefix(@Param("prefix") String prefix, Pageable pageable);

  @Query(
      """
      SELECT i FROM InquiryOrder i
      WHERE i.deletedAt IS NULL
        AND (:status IS NULL OR i.status = :status)
        AND (:inquiryNo = '' OR UPPER(i.inquiryNo) LIKE UPPER(CONCAT('%', :inquiryNo, '%')))
        AND (:customerName = '' OR UPPER(i.customerName) LIKE UPPER(CONCAT('%', :customerName, '%')))
        AND (:por = '' OR UPPER(TRIM(COALESCE(i.por, ''))) = UPPER(:por))
        AND (:pol = '' OR UPPER(TRIM(COALESCE(i.pol, ''))) = UPPER(:pol))
        AND (:pod = '' OR UPPER(TRIM(COALESCE(i.pod, ''))) = UPPER(:pod))
        AND (:zipCode = '' OR UPPER(COALESCE(i.zipCode, '')) LIKE UPPER(CONCAT('%', :zipCode, '%')))
        AND (:city = '' OR UPPER(COALESCE(i.city, '')) LIKE UPPER(CONCAT('%', :city, '%')))
        AND (:state = '' OR UPPER(TRIM(COALESCE(i.state, ''))) = UPPER(:state))
        AND (:pickUpAddress = '' OR UPPER(COALESCE(i.pickUpAddress, '')) LIKE UPPER(CONCAT('%', :pickUpAddress, '%')))
        AND (:fumigationPoint = '' OR UPPER(TRIM(COALESCE(i.fumigationPoint, ''))) = UPPER(:fumigationPoint))
        AND (:createdBy IS NULL OR i.createdBy = :createdBy)
      """)
  Page<InquiryOrder> search(
      @Param("status") InquiryStatus status,
      @Param("inquiryNo") String inquiryNo,
      @Param("customerName") String customerName,
      @Param("por") String por,
      @Param("pol") String pol,
      @Param("pod") String pod,
      @Param("zipCode") String zipCode,
      @Param("city") String city,
      @Param("state") String state,
      @Param("pickUpAddress") String pickUpAddress,
      @Param("fumigationPoint") String fumigationPoint,
      @Param("createdBy") Long createdBy,
      Pageable pageable);

  /**
   * 关联报价单已因有效期归档时，询价同步为已过期（不覆盖已作废）。
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      UPDATE InquiryOrder i
      SET i.status = com.furuiduo.quote.quote.entity.InquiryStatus.EXPIRED,
          i.updatedAt = CURRENT_TIMESTAMP
      WHERE i.deletedAt IS NULL
        AND i.status IN (
            com.furuiduo.quote.quote.entity.InquiryStatus.PENDING_QUOTE,
            com.furuiduo.quote.quote.entity.InquiryStatus.QUOTED
          )
        AND (
          EXISTS (
            SELECT 1 FROM QuoteOrder q
            WHERE q.deletedAt IS NULL
              AND q.status = com.furuiduo.quote.quote.entity.QuoteStatus.ARCHIVED
              AND q.archiveReason IN (
                  com.furuiduo.quote.quote.entity.QuoteArchiveReason.QUOTED_EXPIRED,
                  com.furuiduo.quote.quote.entity.QuoteArchiveReason.PENDING_EXPIRED
                )
              AND (q.id = i.quoteId OR q.inquiryId = i.id)
          )
        )
      """)
  int expireLinkedToExpiredQuotes();

  /**
   * 询价自身有效期已过（通常由报价回填）且仍为待报价/已报价 → 已过期。
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      UPDATE InquiryOrder i
      SET i.status = com.furuiduo.quote.quote.entity.InquiryStatus.EXPIRED,
          i.updatedAt = CURRENT_TIMESTAMP
      WHERE i.deletedAt IS NULL
        AND i.status IN (
            com.furuiduo.quote.quote.entity.InquiryStatus.PENDING_QUOTE,
            com.furuiduo.quote.quote.entity.InquiryStatus.QUOTED
          )
        AND i.validUntil IS NOT NULL
        AND i.validUntil < CURRENT_DATE
      """)
  int expireByOwnValidUntil();
}
