package com.furuiduo.quote.quote.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.cost.entity.CostHighlightMode;
import com.furuiduo.quote.quote.entity.QuoteCostType;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.entity.QuoteStatus;
import com.furuiduo.quote.quote.entity.QuoteTransportMode;

public interface QuoteOrderRepository extends JpaRepository<QuoteOrder, Long> {

  @EntityGraph(attributePaths = "lines")
  Optional<QuoteOrder> findWithLinesById(Long id);

  java.util.List<QuoteOrder> findByCostRiskActiveTrueAndDeletedAtIsNull();

  @Query(
      """
      SELECT q FROM QuoteOrder q WHERE
      (:quoteNo = '' OR UPPER(q.quoteNo) LIKE UPPER(CONCAT('%', :quoteNo, '%')))
      AND (:customerName = '' OR UPPER(q.customerName) LIKE UPPER(CONCAT('%', :customerName, '%')))
      AND (:transportMode IS NULL OR q.transportMode = :transportMode)
      AND (:statusFilterEmpty = TRUE OR q.status IN :statuses)
      AND (
        :zipCode = '' OR
        UPPER(COALESCE(q.zipCode, '')) LIKE UPPER(CONCAT('%', :zipCode, '%')) OR
        EXISTS (
          SELECT 1 FROM QuoteLibraryUsage u
          WHERE u.quoteOrder = q
            AND u.costMode = com.furuiduo.quote.cost.entity.CostHighlightMode.road
            AND EXISTS (
              SELECT 1 FROM CostRoad r
              WHERE r.id = u.costId
                AND UPPER(TRIM(COALESCE(r.zipCode, ''))) LIKE UPPER(CONCAT('%', :zipCode, '%'))
            )
        )
      )
      AND (:city = '' OR UPPER(q.city) LIKE UPPER(CONCAT('%', :city, '%')))
      AND (:state = '' OR UPPER(TRIM(q.state)) = UPPER(:state))
      AND (:por = '' OR UPPER(TRIM(q.por)) = UPPER(:por))
      AND (:pol = '' OR UPPER(TRIM(q.pol)) = UPPER(:pol))
      AND (:pod = '' OR UPPER(TRIM(q.pod)) = UPPER(:pod))
      AND (:pickUpAddress = '' OR UPPER(q.pickUpAddress) LIKE UPPER(CONCAT('%', :pickUpAddress, '%')))
      AND (:fumigationPoint = '' OR UPPER(TRIM(q.fumigationPoint)) = UPPER(:fumigationPoint))
      AND (
        :supplier = '' OR
        EXISTS (
          SELECT 1 FROM QuoteLibraryUsage u
          WHERE u.quoteOrder = q
            AND u.costMode = com.furuiduo.quote.cost.entity.CostHighlightMode.road
            AND EXISTS (
              SELECT 1 FROM CostRoad r
              WHERE r.id = u.costId
                AND UPPER(TRIM(COALESCE(r.supplier, ''))) = UPPER(:supplier)
            )
        )
        OR EXISTS (
          SELECT 1 FROM QuoteCostSnapshot s
          WHERE s.quoteOrder = q
            AND s.costType = com.furuiduo.quote.quote.entity.QuoteCostType.ROAD
            AND EXISTS (
              SELECT 1 FROM CostRoad r
              WHERE r.id = s.costRefId
                AND UPPER(TRIM(COALESCE(r.supplier, ''))) = UPPER(:supplier)
            )
        )
      )
      AND (:ssl = '' OR UPPER(TRIM(q.ssl)) LIKE UPPER(CONCAT('%', :ssl, '%')))
      AND (:followUpByName = '' OR UPPER(q.followUpByName) LIKE UPPER(CONCAT('%', :followUpByName, '%')))
      AND (:createdBy IS NULL OR q.createdBy = :createdBy)
      AND q.deletedAt IS NULL
      AND (
        :scopeAll = TRUE OR
        (:scopeDept = TRUE AND q.deptId = :deptId) OR
        (:scopeAssigned = TRUE AND (q.createdBy IS NULL OR q.createdBy IN :assignedUserIds)) OR
        (:scopeSelf = TRUE AND q.createdBy = :userId)
      )
      """)
  Page<QuoteOrder> search(
      @Param("quoteNo") String quoteNo,
      @Param("customerName") String customerName,
      @Param("transportMode") QuoteTransportMode transportMode,
      @Param("statusFilterEmpty") boolean statusFilterEmpty,
      @Param("statuses") java.util.Collection<QuoteStatus> statuses,
      @Param("zipCode") String zipCode,
      @Param("city") String city,
      @Param("state") String state,
      @Param("por") String por,
      @Param("pol") String pol,
      @Param("pod") String pod,
      @Param("pickUpAddress") String pickUpAddress,
      @Param("fumigationPoint") String fumigationPoint,
      @Param("supplier") String supplier,
      @Param("ssl") String ssl,
      @Param("followUpByName") String followUpByName,
      @Param("createdBy") Long createdBy,
      @Param("scopeAll") boolean scopeAll,
      @Param("scopeDept") boolean scopeDept,
      @Param("scopeAssigned") boolean scopeAssigned,
      @Param("scopeSelf") boolean scopeSelf,
      @Param("deptId") Long deptId,
      @Param("userId") Long userId,
      @Param("assignedUserIds") java.util.Collection<Long> assignedUserIds,
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
      AND (:statusFilterEmpty = TRUE OR q.status IN :statuses)
      AND (
        :zipCode = '' OR
        UPPER(COALESCE(q.zipCode, '')) LIKE UPPER(CONCAT('%', :zipCode, '%')) OR
        EXISTS (
          SELECT 1 FROM QuoteLibraryUsage u
          WHERE u.quoteOrder = q
            AND u.costMode = com.furuiduo.quote.cost.entity.CostHighlightMode.road
            AND EXISTS (
              SELECT 1 FROM CostRoad r
              WHERE r.id = u.costId
                AND UPPER(TRIM(COALESCE(r.zipCode, ''))) LIKE UPPER(CONCAT('%', :zipCode, '%'))
            )
        )
      )
      AND (:city = '' OR UPPER(q.city) LIKE UPPER(CONCAT('%', :city, '%')))
      AND (:state = '' OR UPPER(TRIM(q.state)) = UPPER(:state))
      AND (:por = '' OR UPPER(TRIM(q.por)) = UPPER(:por))
      AND (:pol = '' OR UPPER(TRIM(q.pol)) = UPPER(:pol))
      AND (:pod = '' OR UPPER(TRIM(q.pod)) = UPPER(:pod))
      AND (:pickUpAddress = '' OR UPPER(q.pickUpAddress) LIKE UPPER(CONCAT('%', :pickUpAddress, '%')))
      AND (:fumigationPoint = '' OR UPPER(TRIM(q.fumigationPoint)) = UPPER(:fumigationPoint))
      AND (
        :supplier = '' OR
        EXISTS (
          SELECT 1 FROM QuoteLibraryUsage u
          WHERE u.quoteOrder = q
            AND u.costMode = com.furuiduo.quote.cost.entity.CostHighlightMode.road
            AND EXISTS (
              SELECT 1 FROM CostRoad r
              WHERE r.id = u.costId
                AND UPPER(TRIM(COALESCE(r.supplier, ''))) = UPPER(:supplier)
            )
        )
        OR EXISTS (
          SELECT 1 FROM QuoteCostSnapshot s
          WHERE s.quoteOrder = q
            AND s.costType = com.furuiduo.quote.quote.entity.QuoteCostType.ROAD
            AND EXISTS (
              SELECT 1 FROM CostRoad r
              WHERE r.id = s.costRefId
                AND UPPER(TRIM(COALESCE(r.supplier, ''))) = UPPER(:supplier)
            )
        )
      )
      AND (:ssl = '' OR UPPER(TRIM(q.ssl)) LIKE UPPER(CONCAT('%', :ssl, '%')))
      AND (:followUpByName = '' OR UPPER(q.followUpByName) LIKE UPPER(CONCAT('%', :followUpByName, '%')))
      AND (:createdBy IS NULL OR q.createdBy = :createdBy)
      AND q.deletedAt IS NULL
      AND (
        :scopeAll = TRUE OR
        (:scopeDept = TRUE AND q.deptId = :deptId) OR
        (:scopeAssigned = TRUE AND (q.createdBy IS NULL OR q.createdBy IN :assignedUserIds)) OR
        (:scopeSelf = TRUE AND q.createdBy = :userId)
      )
      """)
  Page<QuoteOrder> searchWithCostSnapshot(
      @Param("costType") QuoteCostType costType,
      @Param("quoteNo") String quoteNo,
      @Param("customerName") String customerName,
      @Param("transportMode") QuoteTransportMode transportMode,
      @Param("statusFilterEmpty") boolean statusFilterEmpty,
      @Param("statuses") java.util.Collection<QuoteStatus> statuses,
      @Param("zipCode") String zipCode,
      @Param("city") String city,
      @Param("state") String state,
      @Param("por") String por,
      @Param("pol") String pol,
      @Param("pod") String pod,
      @Param("pickUpAddress") String pickUpAddress,
      @Param("fumigationPoint") String fumigationPoint,
      @Param("supplier") String supplier,
      @Param("ssl") String ssl,
      @Param("followUpByName") String followUpByName,
      @Param("createdBy") Long createdBy,
      @Param("scopeAll") boolean scopeAll,
      @Param("scopeDept") boolean scopeDept,
      @Param("scopeAssigned") boolean scopeAssigned,
      @Param("scopeSelf") boolean scopeSelf,
      @Param("deptId") Long deptId,
      @Param("userId") Long userId,
      @Param("assignedUserIds") java.util.Collection<Long> assignedUserIds,
      Pageable pageable);

  /** 报价库反查：引用了指定报价库成本行的报价单（quote_library_usage）。 */
  @Query(
      """
      SELECT q FROM QuoteOrder q WHERE
      EXISTS (
        SELECT 1 FROM QuoteLibraryUsage u
        WHERE u.quoteOrder = q AND u.costMode = :costMode AND u.costId = :costId
      )
      AND (:quoteNo = '' OR UPPER(q.quoteNo) LIKE UPPER(CONCAT('%', :quoteNo, '%')))
      AND (:customerName = '' OR UPPER(q.customerName) LIKE UPPER(CONCAT('%', :customerName, '%')))
      AND (:transportMode IS NULL OR q.transportMode = :transportMode)
      AND (:statusFilterEmpty = TRUE OR q.status IN :statuses)
      AND (
        :zipCode = '' OR
        UPPER(COALESCE(q.zipCode, '')) LIKE UPPER(CONCAT('%', :zipCode, '%')) OR
        EXISTS (
          SELECT 1 FROM QuoteLibraryUsage u
          WHERE u.quoteOrder = q
            AND u.costMode = com.furuiduo.quote.cost.entity.CostHighlightMode.road
            AND EXISTS (
              SELECT 1 FROM CostRoad r
              WHERE r.id = u.costId
                AND UPPER(TRIM(COALESCE(r.zipCode, ''))) LIKE UPPER(CONCAT('%', :zipCode, '%'))
            )
        )
      )
      AND (:city = '' OR UPPER(q.city) LIKE UPPER(CONCAT('%', :city, '%')))
      AND (:state = '' OR UPPER(TRIM(q.state)) = UPPER(:state))
      AND (:por = '' OR UPPER(TRIM(q.por)) = UPPER(:por))
      AND (:pol = '' OR UPPER(TRIM(q.pol)) = UPPER(:pol))
      AND (:pod = '' OR UPPER(TRIM(q.pod)) = UPPER(:pod))
      AND (:pickUpAddress = '' OR UPPER(q.pickUpAddress) LIKE UPPER(CONCAT('%', :pickUpAddress, '%')))
      AND (:fumigationPoint = '' OR UPPER(TRIM(q.fumigationPoint)) = UPPER(:fumigationPoint))
      AND (
        :supplier = '' OR
        EXISTS (
          SELECT 1 FROM QuoteLibraryUsage u
          WHERE u.quoteOrder = q
            AND u.costMode = com.furuiduo.quote.cost.entity.CostHighlightMode.road
            AND EXISTS (
              SELECT 1 FROM CostRoad r
              WHERE r.id = u.costId
                AND UPPER(TRIM(COALESCE(r.supplier, ''))) = UPPER(:supplier)
            )
        )
        OR EXISTS (
          SELECT 1 FROM QuoteCostSnapshot s
          WHERE s.quoteOrder = q
            AND s.costType = com.furuiduo.quote.quote.entity.QuoteCostType.ROAD
            AND EXISTS (
              SELECT 1 FROM CostRoad r
              WHERE r.id = s.costRefId
                AND UPPER(TRIM(COALESCE(r.supplier, ''))) = UPPER(:supplier)
            )
        )
      )
      AND (:ssl = '' OR UPPER(TRIM(q.ssl)) LIKE UPPER(CONCAT('%', :ssl, '%')))
      AND (:followUpByName = '' OR UPPER(q.followUpByName) LIKE UPPER(CONCAT('%', :followUpByName, '%')))
      AND (:createdBy IS NULL OR q.createdBy = :createdBy)
      AND q.deletedAt IS NULL
      AND (
        :scopeAll = TRUE OR
        (:scopeDept = TRUE AND q.deptId = :deptId) OR
        (:scopeAssigned = TRUE AND (q.createdBy IS NULL OR q.createdBy IN :assignedUserIds)) OR
        (:scopeSelf = TRUE AND q.createdBy = :userId)
      )
      """)
  Page<QuoteOrder> searchByLibraryUsage(
      @Param("costMode") CostHighlightMode costMode,
      @Param("costId") Long costId,
      @Param("quoteNo") String quoteNo,
      @Param("customerName") String customerName,
      @Param("transportMode") QuoteTransportMode transportMode,
      @Param("statusFilterEmpty") boolean statusFilterEmpty,
      @Param("statuses") java.util.Collection<QuoteStatus> statuses,
      @Param("zipCode") String zipCode,
      @Param("city") String city,
      @Param("state") String state,
      @Param("por") String por,
      @Param("pol") String pol,
      @Param("pod") String pod,
      @Param("pickUpAddress") String pickUpAddress,
      @Param("fumigationPoint") String fumigationPoint,
      @Param("supplier") String supplier,
      @Param("ssl") String ssl,
      @Param("followUpByName") String followUpByName,
      @Param("createdBy") Long createdBy,
      @Param("scopeAll") boolean scopeAll,
      @Param("scopeDept") boolean scopeDept,
      @Param("scopeAssigned") boolean scopeAssigned,
      @Param("scopeSelf") boolean scopeSelf,
      @Param("deptId") Long deptId,
      @Param("userId") Long userId,
      @Param("assignedUserIds") java.util.Collection<Long> assignedUserIds,
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
        (:scopeAssigned = TRUE AND (q.createdBy IS NULL OR q.createdBy IN :assignedUserIds)) OR
        (:scopeSelf = TRUE AND q.createdBy = :userId)
      )
      """)
  Page<QuoteOrder> searchApprovalList(
      @Param("approvalNo") String approvalNo,
      @Param("statusFilter") String statusFilter,
      @Param("scopeAll") boolean scopeAll,
      @Param("scopeDept") boolean scopeDept,
      @Param("scopeAssigned") boolean scopeAssigned,
      @Param("scopeSelf") boolean scopeSelf,
      @Param("deptId") Long deptId,
      @Param("userId") Long userId,
      @Param("assignedUserIds") java.util.Collection<Long> assignedUserIds,
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

  /**
   * 超过有效期且非终态 → 已归档；历史 EXPIRED/SUPERSEDED → 已归档；同时写入 archiveReason、清除成本风险。
   * 成交、变更中、已作废/拒绝不自动归档。待报价过期也会归档（PENDING_EXPIRED）。
   */
  @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      UPDATE QuoteOrder q
      SET q.archiveReason = CASE
            WHEN q.status = com.furuiduo.quote.quote.entity.QuoteStatus.SUPERSEDED
              THEN com.furuiduo.quote.quote.entity.QuoteArchiveReason.SUPERSEDED
            WHEN q.status IN (
                com.furuiduo.quote.quote.entity.QuoteStatus.QUOTED,
                com.furuiduo.quote.quote.entity.QuoteStatus.SENT,
                com.furuiduo.quote.quote.entity.QuoteStatus.WON,
                com.furuiduo.quote.quote.entity.QuoteStatus.EFFECTIVE,
                com.furuiduo.quote.quote.entity.QuoteStatus.FOLLOWING,
                com.furuiduo.quote.quote.entity.QuoteStatus.EXPIRED
              )
              THEN com.furuiduo.quote.quote.entity.QuoteArchiveReason.QUOTED_EXPIRED
            ELSE com.furuiduo.quote.quote.entity.QuoteArchiveReason.PENDING_EXPIRED
          END,
          q.status = com.furuiduo.quote.quote.entity.QuoteStatus.ARCHIVED,
          q.costRiskActive = false,
          q.costRiskReason = null,
          q.costRiskAt = null,
          q.updatedAt = CURRENT_TIMESTAMP
      WHERE (q.deletedAt IS NULL)
        AND (
          q.status IN (
              com.furuiduo.quote.quote.entity.QuoteStatus.EXPIRED,
              com.furuiduo.quote.quote.entity.QuoteStatus.SUPERSEDED
          )
         OR (
              q.validUntil IS NOT NULL
          AND q.validUntil < CURRENT_DATE
          AND q.status NOT IN (
              com.furuiduo.quote.quote.entity.QuoteStatus.VOIDED,
              com.furuiduo.quote.quote.entity.QuoteStatus.REJECTED,
              com.furuiduo.quote.quote.entity.QuoteStatus.WON,
              com.furuiduo.quote.quote.entity.QuoteStatus.SUPERSEDED,
              com.furuiduo.quote.quote.entity.QuoteStatus.REVISING,
              com.furuiduo.quote.quote.entity.QuoteStatus.EXPIRED,
              com.furuiduo.quote.quote.entity.QuoteStatus.ARCHIVED
          )
         )
        )
      """)
  int archiveQuotesPastValidUntil();

  /** @deprecated 使用 {@link #archiveQuotesPastValidUntil()} */
  @Deprecated
  default int voidQuotesPastValidUntil() {
    return archiveQuotesPastValidUntil();
  }
}
