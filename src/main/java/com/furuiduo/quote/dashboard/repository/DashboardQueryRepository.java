package com.furuiduo.quote.dashboard.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.furuiduo.quote.dashboard.support.DashboardScopeParams;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.entity.QuoteStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

@Repository
public class DashboardQueryRepository {

  private static final String SCOPE_FILTER =
      """
      AND (
        :scopeAll = TRUE OR
        (:scopeDept = TRUE AND q.dept_id = :deptId) OR
        (:scopeAssigned = TRUE AND q.created_by IN (:assignedUserIds)) OR
        (:scopeSelf = TRUE AND q.created_by = :userId)
      )
      """;

  private static final String INQUIRY_SCOPE_FILTER =
      """
      AND (
        :scopeAll = TRUE OR
        (:scopeDept = TRUE AND i.dept_id = :deptId) OR
        (:scopeAssigned = TRUE AND i.created_by IN (:assignedUserIds)) OR
        (:scopeSelf = TRUE AND i.created_by = :userId)
      )
      """;

  private static final String JPQL_SCOPE_FILTER =
      """
      AND (
        :scopeAll = TRUE OR
        (:scopeDept = TRUE AND q.deptId = :deptId) OR
        (:scopeAssigned = TRUE AND q.createdBy IN :assignedUserIds) OR
        (:scopeSelf = TRUE AND q.createdBy = :userId)
      )
      """;

  @PersistenceContext private EntityManager entityManager;

  public long countCreatedBetween(
      DashboardScopeParams scope, LocalDateTime from, LocalDateTime to) {
    String sql =
        """
        SELECT COUNT(*) FROM quote_order q
        WHERE q.deleted_at IS NULL
          AND q.created_at >= :from AND q.created_at < :to
        """
            + SCOPE_FILTER;
    return scalarLong(sql, scope, from, to);
  }

  /** 本月询价单：询价单创建时间落在区间内 */
  public long countInquiriesCreatedBetween(
      DashboardScopeParams scope, LocalDateTime from, LocalDateTime to) {
    String sql =
        """
        SELECT COUNT(*) FROM inquiry_order i
        WHERE i.deleted_at IS NULL
          AND i.created_at >= :from AND i.created_at < :to
        """
            + INQUIRY_SCOPE_FILTER;
    return scalarLongInquiry(sql, scope, from, to);
  }

  /**
   * 本月已报价：确认报价时间（无则创建时间）落在区间内，且状态为已报价族。
   */
  public long countQuotedBetween(
      DashboardScopeParams scope, LocalDateTime from, LocalDateTime to) {
    String sql =
        """
        SELECT COUNT(*) FROM quote_order q
        WHERE q.deleted_at IS NULL
          AND q.status IN ('QUOTED', 'SENT', 'WON', 'EFFECTIVE', 'FOLLOWING')
          AND COALESCE(q.approved_at, q.created_at) >= :from
          AND COALESCE(q.approved_at, q.created_at) < :to
        """
            + SCOPE_FILTER;
    return scalarLong(sql, scope, from, to);
  }

  /**
   * 已报价且关联询价单：用于转换率分子（排除历史无询价关联的已报价单）。
   */
  public long countQuotedLinkedInquiryBetween(
      DashboardScopeParams scope, LocalDateTime from, LocalDateTime to) {
    String sql =
        """
        SELECT COUNT(*) FROM quote_order q
        WHERE q.deleted_at IS NULL
          AND q.inquiry_id IS NOT NULL
          AND q.status IN ('QUOTED', 'SENT', 'WON', 'EFFECTIVE', 'FOLLOWING')
          AND COALESCE(q.approved_at, q.created_at) >= :from
          AND COALESCE(q.approved_at, q.created_at) < :to
        """
            + SCOPE_FILTER;
    return scalarLong(sql, scope, from, to);
  }

  /** 待报价：待报价/草稿报价单 */
  public long countPendingQuote(DashboardScopeParams scope) {
    String sql =
        """
        SELECT COUNT(*) FROM quote_order q
        WHERE q.deleted_at IS NULL
          AND q.status IN ('PENDING_QUOTE', 'DRAFT')
        """
            + SCOPE_FILTER;
    Number value =
        (Number) bindScope(entityManager.createNativeQuery(sql), scope).getSingleResult();
    return value.longValue();
  }

  /** 成本异常：有效单据上仍激活的成本风险 */
  public long countCostRiskActive(DashboardScopeParams scope) {
    String sql =
        """
        SELECT COUNT(*) FROM quote_order q
        WHERE q.deleted_at IS NULL
          AND q.cost_risk_active = TRUE
          AND q.status NOT IN (
            'VOIDED', 'REJECTED', 'LOST', 'ARCHIVED', 'EXPIRED', 'SUPERSEDED', 'REVISING')
        """
            + SCOPE_FILTER;
    Number value =
        (Number) bindScope(entityManager.createNativeQuery(sql), scope).getSingleResult();
    return value.longValue();
  }

  public long countCostRiskActiveUpdatedBetween(
      DashboardScopeParams scope, LocalDateTime from, LocalDateTime to) {
    String sql =
        """
        SELECT COUNT(*) FROM quote_order q
        WHERE q.deleted_at IS NULL
          AND q.cost_risk_active = TRUE
          AND q.status NOT IN (
            'VOIDED', 'REJECTED', 'LOST', 'ARCHIVED', 'EXPIRED', 'SUPERSEDED', 'REVISING')
          AND COALESCE(q.cost_risk_at, q.updated_at) >= :from
          AND COALESCE(q.cost_risk_at, q.updated_at) < :to
        """
            + SCOPE_FILTER;
    return scalarLong(sql, scope, from, to);
  }

  public BigDecimal sumAmountCreatedBetween(
      DashboardScopeParams scope, LocalDateTime from, LocalDateTime to) {
    String sql =
        """
        SELECT COALESCE(SUM(q.total_amount), 0) FROM quote_order q
        WHERE q.created_at >= :from AND q.created_at < :to
        """
            + SCOPE_FILTER;
    Object raw =
        bindScope(entityManager.createNativeQuery(sql), scope)
            .setParameter("from", from)
            .setParameter("to", to)
            .getSingleResult();
    if (raw instanceof BigDecimal decimal) {
      return decimal;
    }
    if (raw instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    return BigDecimal.ZERO;
  }

  public long countByStatuses(DashboardScopeParams scope, List<String> statuses) {
    if (statuses.isEmpty()) {
      return 0;
    }
    String sql =
        """
        SELECT COUNT(*) FROM quote_order q
        WHERE q.status IN (:statuses)
        """
            + SCOPE_FILTER;
    Number value =
        (Number)
            bindScope(entityManager.createNativeQuery(sql), scope)
                .setParameter("statuses", statuses)
                .getSingleResult();
    return value.longValue();
  }

  public long countByStatusesUpdatedBetween(
      DashboardScopeParams scope, List<String> statuses, LocalDateTime from, LocalDateTime to) {
    if (statuses.isEmpty()) {
      return 0;
    }
    String sql =
        """
        SELECT COUNT(*) FROM quote_order q
        WHERE q.status IN (:statuses)
          AND q.updated_at >= :from AND q.updated_at < :to
        """
            + SCOPE_FILTER;
    Number value =
        (Number)
            bindScope(entityManager.createNativeQuery(sql), scope)
                .setParameter("statuses", statuses)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    return value.longValue();
  }

  public long countExpiringSoon(DashboardScopeParams scope, LocalDate today, LocalDate deadline) {
    String sql =
        """
        SELECT COUNT(*) FROM quote_order q
        WHERE q.valid_until IS NOT NULL
          AND q.valid_until >= :today
          AND q.valid_until <= :deadline
          AND q.status NOT IN ('VOIDED', 'LOST', 'WON', 'EXPIRED')
        """
            + SCOPE_FILTER;
    Number value =
        (Number)
            bindScope(entityManager.createNativeQuery(sql), scope)
                .setParameter("today", today)
                .setParameter("deadline", deadline)
                .getSingleResult();
    return value.longValue();
  }

  public long countWonBetween(
      DashboardScopeParams scope, LocalDateTime from, LocalDateTime to) {
    String sql =
        """
        SELECT COUNT(*) FROM quote_order q
        WHERE q.status = 'WON'
          AND q.updated_at >= :from AND q.updated_at < :to
        """
            + SCOPE_FILTER;
    return scalarLong(sql, scope, from, to);
  }

  public long countClosedBetween(
      DashboardScopeParams scope, LocalDateTime from, LocalDateTime to) {
    String sql =
        """
        SELECT COUNT(*) FROM quote_order q
        WHERE q.status IN ('WON', 'LOST', 'EXPIRED')
          AND q.updated_at >= :from AND q.updated_at < :to
        """
            + SCOPE_FILTER;
    return scalarLong(sql, scope, from, to);
  }

  public List<QuoteOrder> findRecentActionable(DashboardScopeParams scope, int limit) {
    String jpql =
        """
        SELECT q FROM QuoteOrder q
        WHERE q.status IN :statuses
        """
            + JPQL_SCOPE_FILTER
            + """
        ORDER BY q.updatedAt DESC
        """;
    return bindScope(entityManager.createQuery(jpql, QuoteOrder.class), scope)
        .setParameter(
            "statuses",
            List.of(
                QuoteStatus.DRAFT,
                QuoteStatus.PENDING_QUOTE,
                QuoteStatus.PENDING_APPROVAL,
                QuoteStatus.QUOTED,
                QuoteStatus.SENT,
                QuoteStatus.WON,
                QuoteStatus.PENDING,
                QuoteStatus.FOLLOWING,
                QuoteStatus.EFFECTIVE))
        .setMaxResults(limit)
        .getResultList();
  }

  /** 待办候选：仅待报价 / 待审批（不含已报价） */
  public List<QuoteOrder> findTodoCandidates(DashboardScopeParams scope, int limit) {
    String jpql =
        """
        SELECT q FROM QuoteOrder q
        WHERE q.status IN :statuses
        """
            + JPQL_SCOPE_FILTER
            + """
        ORDER BY q.updatedAt DESC
        """;
    return bindScope(entityManager.createQuery(jpql, QuoteOrder.class), scope)
        .setParameter(
            "statuses",
            List.of(
                QuoteStatus.DRAFT,
                QuoteStatus.PENDING_QUOTE,
                QuoteStatus.PENDING_APPROVAL,
                QuoteStatus.PENDING))
        .setMaxResults(limit)
        .getResultList();
  }

  public List<QuoteOrder> findCostRiskQuotes(DashboardScopeParams scope, int limit) {
    String jpql =
        """
        SELECT q FROM QuoteOrder q
        WHERE q.costRiskActive = TRUE
          AND q.status NOT IN :excluded
        """
            + JPQL_SCOPE_FILTER
            + """
        ORDER BY q.costRiskAt DESC, q.updatedAt DESC
        """;
    return bindScope(entityManager.createQuery(jpql, QuoteOrder.class), scope)
        .setParameter(
            "excluded",
            List.of(
                QuoteStatus.VOIDED,
                QuoteStatus.REJECTED,
                QuoteStatus.LOST,
                QuoteStatus.ARCHIVED,
                QuoteStatus.EXPIRED,
                QuoteStatus.SUPERSEDED,
                QuoteStatus.REVISING))
        .setMaxResults(limit)
        .getResultList();
  }

  public List<QuoteOrder> findDraftsWithSnapshots(DashboardScopeParams scope, int limit) {
    String jpql =
        """
        SELECT DISTINCT q FROM QuoteOrder q
        WHERE q.status = :draft
        AND EXISTS (
          SELECT 1 FROM QuoteCostSnapshot cs WHERE cs.quoteOrder.id = q.id
        )
        """
            + JPQL_SCOPE_FILTER
            + """
        ORDER BY q.updatedAt DESC
        """;
    return bindScope(entityManager.createQuery(jpql, QuoteOrder.class), scope)
        .setParameter("draft", QuoteStatus.DRAFT)
        .setMaxResults(limit)
        .getResultList();
  }

  @SuppressWarnings("unchecked")
  public List<Object[]> findTopRoutes(DashboardScopeParams scope, LocalDateTime since, int limit) {
    String sql =
        """
        SELECT CONCAT_WS(
                 ' → ',
                 NULLIF(TRIM(q.por), ''),
                 NULLIF(TRIM(q.pol), ''),
                 NULLIF(TRIM(q.pod), '')
               ) AS route_name,
               COUNT(*) AS cnt
        FROM quote_order q
        WHERE q.deleted_at IS NULL
          AND q.created_at >= :since
          AND (
            NULLIF(TRIM(q.por), '') IS NOT NULL
            OR NULLIF(TRIM(q.pol), '') IS NOT NULL
            OR NULLIF(TRIM(q.pod), '') IS NOT NULL
          )
        """
            + SCOPE_FILTER
            + """
        GROUP BY 1
        ORDER BY cnt DESC
        """;
    List<Object[]> rows =
        bindScope(entityManager.createNativeQuery(sql), scope)
            .setParameter("since", since)
            .setMaxResults(limit)
            .getResultList();
    return rows.stream()
        .map(this::toObjectArray)
        .filter(row -> row.length > 0 && row[0] != null && !row[0].toString().isBlank())
        .toList();
  }

  @SuppressWarnings("unchecked")
  public List<Object[]> sumAmountByCreatedMonth(
      DashboardScopeParams scope, LocalDateTime from, LocalDateTime to) {
    String sql =
        """
        SELECT EXTRACT(YEAR FROM q.created_at), EXTRACT(MONTH FROM q.created_at),
               COALESCE(SUM(q.total_amount), 0)
        FROM quote_order q
        WHERE q.deleted_at IS NULL
          AND q.created_at >= :from AND q.created_at < :to
        """
            + SCOPE_FILTER
            + """
        GROUP BY EXTRACT(YEAR FROM q.created_at), EXTRACT(MONTH FROM q.created_at)
        """;
    return bindScope(entityManager.createNativeQuery(sql), scope)
        .setParameter("from", from)
        .setParameter("to", to)
        .getResultList()
        .stream()
        .map(this::toObjectArray)
        .toList();
  }

  @SuppressWarnings("unchecked")
  public List<Object[]> sumWonAmountByUpdatedMonth(
      DashboardScopeParams scope, LocalDateTime from, LocalDateTime to) {
    String sql =
        """
        SELECT EXTRACT(YEAR FROM q.updated_at), EXTRACT(MONTH FROM q.updated_at),
               COALESCE(SUM(q.total_amount), 0)
        FROM quote_order q
        WHERE q.deleted_at IS NULL
          AND q.status = 'WON'
          AND q.updated_at >= :from AND q.updated_at < :to
        """
            + SCOPE_FILTER
            + """
        GROUP BY EXTRACT(YEAR FROM q.updated_at), EXTRACT(MONTH FROM q.updated_at)
        """;
    return bindScope(entityManager.createNativeQuery(sql), scope)
        .setParameter("from", from)
        .setParameter("to", to)
        .getResultList()
        .stream()
        .map(this::toObjectArray)
        .toList();
  }

  /** 近 N 月询价单数量（按创建月） */
  @SuppressWarnings("unchecked")
  public List<Object[]> countInquiriesByCreatedMonth(
      DashboardScopeParams scope, LocalDateTime from, LocalDateTime to) {
    String sql =
        """
        SELECT EXTRACT(YEAR FROM i.created_at), EXTRACT(MONTH FROM i.created_at),
               COUNT(*)
        FROM inquiry_order i
        WHERE i.deleted_at IS NULL
          AND i.created_at >= :from AND i.created_at < :to
        """
            + INQUIRY_SCOPE_FILTER
            + """
        GROUP BY EXTRACT(YEAR FROM i.created_at), EXTRACT(MONTH FROM i.created_at)
        """;
    return bindScope(entityManager.createNativeQuery(sql), scope)
        .setParameter("from", from)
        .setParameter("to", to)
        .getResultList()
        .stream()
        .map(this::toObjectArray)
        .toList();
  }

  /** 近 N 月已报价数量（按确认/创建月） */
  @SuppressWarnings("unchecked")
  public List<Object[]> countQuotedByMonth(
      DashboardScopeParams scope, LocalDateTime from, LocalDateTime to) {
    String sql =
        """
        SELECT EXTRACT(YEAR FROM COALESCE(q.approved_at, q.created_at)),
               EXTRACT(MONTH FROM COALESCE(q.approved_at, q.created_at)),
               COUNT(*)
        FROM quote_order q
        WHERE q.deleted_at IS NULL
          AND q.status IN ('QUOTED', 'SENT', 'WON', 'EFFECTIVE', 'FOLLOWING')
          AND COALESCE(q.approved_at, q.created_at) >= :from
          AND COALESCE(q.approved_at, q.created_at) < :to
        """
            + SCOPE_FILTER
            + """
        GROUP BY EXTRACT(YEAR FROM COALESCE(q.approved_at, q.created_at)),
                 EXTRACT(MONTH FROM COALESCE(q.approved_at, q.created_at))
        """;
    return bindScope(entityManager.createNativeQuery(sql), scope)
        .setParameter("from", from)
        .setParameter("to", to)
        .getResultList()
        .stream()
        .map(this::toObjectArray)
        .toList();
  }

  private Object[] toObjectArray(Object row) {
    if (row instanceof Object[] array) {
      return array;
    }
    return new Object[] {row};
  }

  private long scalarLong(
      String sql, DashboardScopeParams scope, LocalDateTime from, LocalDateTime to) {
    Number value =
        (Number)
            bindScope(entityManager.createNativeQuery(sql), scope)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    return value.longValue();
  }

  private long scalarLongInquiry(
      String sql, DashboardScopeParams scope, LocalDateTime from, LocalDateTime to) {
    Number value =
        (Number)
            bindScope(entityManager.createNativeQuery(sql), scope)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    return value.longValue();
  }

  private Query bindScope(Query query, DashboardScopeParams scope) {
    query.setParameter("scopeAll", scope.scopeAll());
    query.setParameter("scopeDept", scope.scopeDept());
    query.setParameter("scopeAssigned", scope.scopeAssigned());
    query.setParameter("scopeSelf", scope.scopeSelf());
    query.setParameter("deptId", scope.deptId());
    query.setParameter("userId", scope.userId());
    query.setParameter("assignedUserIds", scope.assignedUserIds());
    return query;
  }

  private TypedQuery<QuoteOrder> bindScope(
      TypedQuery<QuoteOrder> query, DashboardScopeParams scope) {
    bindScope((Query) query, scope);
    return query;
  }
}
