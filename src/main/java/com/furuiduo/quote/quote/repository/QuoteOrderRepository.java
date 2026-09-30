package com.furuiduo.quote.quote.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.quote.entity.QuoteCostType;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.entity.QuoteStatus;
import com.furuiduo.quote.quote.entity.QuoteTransportMode;

public interface QuoteOrderRepository extends JpaRepository<QuoteOrder, Long> {

  @EntityGraph(attributePaths = "lines")
  Optional<QuoteOrder> findWithLinesById(Long id);

  @Query(
      """
      SELECT q FROM QuoteOrder q WHERE
      (:quoteNo = '' OR UPPER(q.quoteNo) LIKE UPPER(CONCAT('%', :quoteNo, '%')))
      AND (:customerName = '' OR UPPER(q.customerName) LIKE UPPER(CONCAT('%', :customerName, '%')))
      AND (:transportMode IS NULL OR q.transportMode = :transportMode)
      AND (:status IS NULL OR q.status = :status)
      AND (:zipCode = '' OR UPPER(q.zipCode) LIKE UPPER(CONCAT('%', :zipCode, '%')))
      AND (:city = '' OR UPPER(q.city) LIKE UPPER(CONCAT('%', :city, '%')))
      AND (:state = '' OR UPPER(TRIM(q.state)) = UPPER(:state))
      AND (:por = '' OR UPPER(TRIM(q.por)) = UPPER(:por))
      AND (:pol = '' OR UPPER(TRIM(q.pol)) = UPPER(:pol))
      AND (:pod = '' OR UPPER(TRIM(q.pod)) = UPPER(:pod))
      AND (:pickUpAddress = '' OR UPPER(q.pickUpAddress) LIKE UPPER(CONCAT('%', :pickUpAddress, '%')))
      AND (:fumigationPoint = '' OR UPPER(TRIM(q.fumigationPoint)) = UPPER(:fumigationPoint))
      AND (:ssl = '' OR UPPER(TRIM(q.ssl)) LIKE UPPER(CONCAT('%', :ssl, '%')))
      AND (:followUpByName = '' OR UPPER(q.followUpByName) LIKE UPPER(CONCAT('%', :followUpByName, '%')))
      AND q.deletedAt IS NULL
      AND (
        :scopeAll = TRUE OR
        (:scopeDept = TRUE AND q.deptId = :deptId) OR
        (:scopeSelf = TRUE AND q.createdBy = :userId)
      )
      """)
  Page<QuoteOrder> search(
      @Param("quoteNo") String quoteNo,
      @Param("customerName") String customerName,
      @Param("transportMode") QuoteTransportMode transportMode,
      @Param("status") QuoteStatus status,
      @Param("zipCode") String zipCode,
      @Param("city") String city,
      @Param("state") String state,
      @Param("por") String por,
      @Param("pol") String pol,
      @Param("pod") String pod,
      @Param("pickUpAddress") String pickUpAddress,
      @Param("fumigationPoint") String fumigationPoint,
      @Param("ssl") String ssl,
      @Param("followUpByName") String followUpByName,
      @Param("scopeAll") boolean scopeAll,
      @Param("scopeDept") boolean scopeDept,
      @Param("scopeSelf") boolean scopeSelf,
      @Param("deptId") Long deptId,
      @Param("userId") Long userId,
      Pageable pageable);

  @Query(
      "SELECT q.quoteNo FROM QuoteOrder q WHERE q.quoteNo LIKE :prefix ORDER BY q.quoteNo DESC")
  Page<String> findQuoteNosByPrefix(@Param("prefix") String prefix, Pageable pageable);

  long countByCustomerId(Long customerId);

  boolean existsByCurrency(String currency);

  /** 报价库：仅返回已关联指定类型成本快照的报价单。 */
  @Query(
      """
      SELECT q FROM QuoteOrder q WHERE
      EXISTS (
        SELECT 1 FROM QuoteCostSnapshot s
        WHERE s.quoteOrder = q AND s.costType = :costType
      )
      AND (:quoteNo = '' OR UPPER(q.quoteNo) LIKE UPPER(CONCAT('%', :quoteNo, '%')))
      AND (:customerName = '' OR UPPER(q.customerName) LIKE UPPER(CONCAT('%', :customerName, '%')))
      AND (:transportMode IS NULL OR q.transportMode = :transportMode)
      AND (:status IS NULL OR q.status = :status)
      AND (:zipCode = '' OR UPPER(q.zipCode) LIKE UPPER(CONCAT('%', :zipCode, '%')))
      AND (:city = '' OR UPPER(q.city) LIKE UPPER(CONCAT('%', :city, '%')))
      AND (:state = '' OR UPPER(TRIM(q.state)) = UPPER(:state))
      AND (:por = '' OR UPPER(TRIM(q.por)) = UPPER(:por))
      AND (:pol = '' OR UPPER(TRIM(q.pol)) = UPPER(:pol))
      AND (:pod = '' OR UPPER(TRIM(q.pod)) = UPPER(:pod))
      AND (:pickUpAddress = '' OR UPPER(q.pickUpAddress) LIKE UPPER(CONCAT('%', :pickUpAddress, '%')))
      AND (:fumigationPoint = '' OR UPPER(TRIM(q.fumigationPoint)) = UPPER(:fumigationPoint))
      AND (:ssl = '' OR UPPER(TRIM(q.ssl)) LIKE UPPER(CONCAT('%', :ssl, '%')))
      AND (:followUpByName = '' OR UPPER(q.followUpByName) LIKE UPPER(CONCAT('%', :followUpByName, '%')))
      AND q.deletedAt IS NULL
      AND (
        :scopeAll = TRUE OR
        (:scopeDept = TRUE AND q.deptId = :deptId) OR
        (:scopeSelf = TRUE AND q.createdBy = :userId)
      )
      """)
  Page<QuoteOrder> searchWithCostSnapshot(
      @Param("costType") QuoteCostType costType,
      @Param("quoteNo") String quoteNo,
      @Param("customerName") String customerName,
      @Param("transportMode") QuoteTransportMode transportMode,
      @Param("status") QuoteStatus status,
      @Param("zipCode") String zipCode,
      @Param("city") String city,
      @Param("state") String state,
      @Param("por") String por,
      @Param("pol") String pol,
      @Param("pod") String pod,
      @Param("pickUpAddress") String pickUpAddress,
      @Param("fumigationPoint") String fumigationPoint,
      @Param("ssl") String ssl,
      @Param("followUpByName") String followUpByName,
      @Param("scopeAll") boolean scopeAll,
      @Param("scopeDept") boolean scopeDept,
      @Param("scopeSelf") boolean scopeSelf,
      @Param("deptId") Long deptId,
      @Param("userId") Long userId,
      Pageable pageable);

  @Query(
      """
      SELECT q FROM QuoteOrder q WHERE
      (
        q.status IN (
          com.furuiduo.quote.quote.entity.QuoteStatus.PENDING_APPROVAL,
          com.furuiduo.quote.quote.entity.QuoteStatus.PENDING
        ) OR
        q.approvedAt IS NOT NULL OR
        EXISTS (
          SELECT 1 FROM QuoteApprovalLog l
          WHERE l.quoteOrderId = q.id AND l.action IN (
            com.furuiduo.quote.quote.entity.QuoteApprovalAction.SUBMIT,
            com.furuiduo.quote.quote.entity.QuoteApprovalAction.APPROVE,
            com.furuiduo.quote.quote.entity.QuoteApprovalAction.REJECT,
            com.furuiduo.quote.quote.entity.QuoteApprovalAction.ROLLBACK
          )
        )
      )
      AND (:approvalNo = '' OR UPPER(CONCAT('AP-', q.quoteNo)) LIKE UPPER(CONCAT('%', :approvalNo, '%')))
      AND (
        :statusFilter = '' OR
        (
          :statusFilter = 'PENDING' AND q.status IN (
            com.furuiduo.quote.quote.entity.QuoteStatus.PENDING_APPROVAL,
            com.furuiduo.quote.quote.entity.QuoteStatus.PENDING
          )
        ) OR
        (
          :statusFilter = 'APPROVED' AND q.approvedAt IS NOT NULL
        ) OR
        (
          :statusFilter = 'REJECTED' AND EXISTS (
            SELECT 1 FROM QuoteApprovalLog l
            WHERE l.quoteOrderId = q.id
            AND l.action = com.furuiduo.quote.quote.entity.QuoteApprovalAction.REJECT
          )
        ) OR
        (
          :statusFilter = 'WITHDRAWN' AND EXISTS (
            SELECT 1 FROM QuoteApprovalLog l
            WHERE l.quoteOrderId = q.id
            AND l.action = com.furuiduo.quote.quote.entity.QuoteApprovalAction.ROLLBACK
          )
        )
      )
      AND (
        :scopeAll = TRUE OR
        (:scopeDept = TRUE AND q.deptId = :deptId) OR
        (:scopeSelf = TRUE AND q.createdBy = :userId)
      )
      """)
  Page<QuoteOrder> searchApprovalList(
      @Param("approvalNo") String approvalNo,
      @Param("statusFilter") String statusFilter,
      @Param("scopeAll") boolean scopeAll,
      @Param("scopeDept") boolean scopeDept,
      @Param("scopeSelf") boolean scopeSelf,
      @Param("deptId") Long deptId,
      @Param("userId") Long userId,
      Pageable pageable);

  boolean existsByRootQuoteIdAndRevisionNoGreaterThanAndStatusIn(
      Long rootQuoteId, Integer revisionNo, java.util.Collection<QuoteStatus> statuses);

  @Query(
      """
      SELECT COUNT(q) > 0 FROM QuoteOrder q
      WHERE q.rootQuoteId = :rootQuoteId
        AND q.revisionNo > :revisionNo
        AND q.status IN :statuses
        AND q.deletedAt IS NULL
      """)
  boolean existsOpenRevision(
      @Param("rootQuoteId") Long rootQuoteId,
      @Param("revisionNo") Integer revisionNo,
      @Param("statuses") java.util.Collection<QuoteStatus> statuses);

  @Query(
      """
      SELECT COALESCE(MAX(q.revisionNo), 0) FROM QuoteOrder q
      WHERE q.rootQuoteId = :rootQuoteId OR q.id = :rootQuoteId
      """)
  int findMaxRevisionNo(@Param("rootQuoteId") Long rootQuoteId);

  java.util.List<QuoteOrder> findByRootQuoteIdOrderByRevisionNoAscIdAsc(Long rootQuoteId);

  java.util.Optional<QuoteOrder> findFirstByRootQuoteIdAndCurrentVersionTrue(Long rootQuoteId);

  /** 超过有效期且非终态 → 作废；历史 EXPIRED → 作废；同时清除成本风险 */
  @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      UPDATE QuoteOrder q
      SET q.status = com.furuiduo.quote.quote.entity.QuoteStatus.VOIDED,
          q.costRiskActive = false,
          q.costRiskReason = null,
          q.costRiskAt = null,
          q.updatedAt = CURRENT_TIMESTAMP
      WHERE (q.deletedAt IS NULL)
        AND (
          q.status = com.furuiduo.quote.quote.entity.QuoteStatus.EXPIRED
         OR (
              q.validUntil IS NOT NULL
          AND q.validUntil < CURRENT_DATE
          AND q.status NOT IN (
              com.furuiduo.quote.quote.entity.QuoteStatus.VOIDED,
              com.furuiduo.quote.quote.entity.QuoteStatus.REJECTED,
              com.furuiduo.quote.quote.entity.QuoteStatus.WON,
              com.furuiduo.quote.quote.entity.QuoteStatus.SUPERSEDED,
              com.furuiduo.quote.quote.entity.QuoteStatus.REVISING,
              com.furuiduo.quote.quote.entity.QuoteStatus.EXPIRED
          )
         )
        )
      """)
  int voidQuotesPastValidUntil();
}
