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

@Repository
public class DashboardQueryRepository {

  private static final String SCOPE_FILTER =
      """
      AND (
        :scopeAll = TRUE OR
        (:scopeDept = TRUE AND q.dept_id = :deptId) OR
        (:scopeSelf = TRUE AND q.created_by = :userId)
      )
      """;

  @PersistenceContext private EntityManager entityManager;

  public long countCreatedBetween(
      DashboardScopeParams scope, LocalDateTime from, LocalDateTime to) {
    String sql =
        """
        SELECT COUNT(*) FROM quote_order q
        WHERE q.created_at >= :from AND q.created_at < :to
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
        entityManager
            .createNativeQuery(sql)
            .setParameter("from", from)
            .setParameter("to", to)
            .setParameter("scopeAll", scope.scopeAll())
            .setParameter("scopeDept", scope.scopeDept())
            .setParameter("scopeSelf", scope.scopeSelf())
            .setParameter("deptId", scope.deptId())
            .setParameter("userId", scope.userId())
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
            entityManager
                .createNativeQuery(sql)
                .setParameter("statuses", statuses)
                .setParameter("scopeAll", scope.scopeAll())
                .setParameter("scopeDept", scope.scopeDept())
                .setParameter("scopeSelf", scope.scopeSelf())
                .setParameter("deptId", scope.deptId())
                .setParameter("userId", scope.userId())
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
            entityManager
                .createNativeQuery(sql)
                .setParameter("statuses", statuses)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("scopeAll", scope.scopeAll())
                .setParameter("scopeDept", scope.scopeDept())
                .setParameter("scopeSelf", scope.scopeSelf())
                .setParameter("deptId", scope.deptId())
                .setParameter("userId", scope.userId())
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
            entityManager
                .createNativeQuery(sql)
                .setParameter("today", today)
                .setParameter("deadline", deadline)
                .setParameter("scopeAll", scope.scopeAll())
                .setParameter("scopeDept", scope.scopeDept())
                .setParameter("scopeSelf", scope.scopeSelf())
                .setParameter("deptId", scope.deptId())
                .setParameter("userId", scope.userId())
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

  @SuppressWarnings("unchecked")
  public List<QuoteOrder> findRecentActionable(DashboardScopeParams scope, int limit) {
    String jpql =
        """
        SELECT q FROM QuoteOrder q
        WHERE q.status IN :statuses
        AND (
          :scopeAll = TRUE OR
          (:scopeDept = TRUE AND q.deptId = :deptId) OR
          (:scopeSelf = TRUE AND q.createdBy = :userId)
        )
        ORDER BY q.updatedAt DESC
        """;
    return entityManager
        .createQuery(jpql, QuoteOrder.class)
        .setParameter(
            "statuses",
            List.of(
                QuoteStatus.DRAFT,
                QuoteStatus.FOLLOWING,
                QuoteStatus.EFFECTIVE,
                QuoteStatus.PENDING,
                QuoteStatus.SENT,
                QuoteStatus.WON))
        .setParameter("scopeAll", scope.scopeAll())
        .setParameter("scopeDept", scope.scopeDept())
        .setParameter("scopeSelf", scope.scopeSelf())
        .setParameter("deptId", scope.deptId())
        .setParameter("userId", scope.userId())
        .setMaxResults(limit)
        .getResultList();
  }

  @SuppressWarnings("unchecked")
  public List<QuoteOrder> findDraftsWithSnapshots(DashboardScopeParams scope, int limit) {
    String jpql =
        """
        SELECT DISTINCT q FROM QuoteOrder q
        WHERE q.status = :draft
        AND EXISTS (
          SELECT 1 FROM QuoteCostSnapshot cs WHERE cs.quoteOrder.id = q.id
        )
        AND (
          :scopeAll = TRUE OR
          (:scopeDept = TRUE AND q.deptId = :deptId) OR
          (:scopeSelf = TRUE AND q.createdBy = :userId)
        )
        ORDER BY q.updatedAt DESC
        """;
    return entityManager
        .createQuery(jpql, QuoteOrder.class)
        .setParameter("draft", QuoteStatus.DRAFT)
        .setParameter("scopeAll", scope.scopeAll())
        .setParameter("scopeDept", scope.scopeDept())
        .setParameter("scopeSelf", scope.scopeSelf())
        .setParameter("deptId", scope.deptId())
        .setParameter("userId", scope.userId())
        .setMaxResults(limit)
        .getResultList();
  }

  @SuppressWarnings("unchecked")
  public List<Object[]> findTopRoutes(DashboardScopeParams scope, LocalDateTime since, int limit) {
    String sql =
        """
        SELECT q.route_summary, COUNT(*)
        FROM quote_order q
        WHERE q.route_summary IS NOT NULL
          AND TRIM(q.route_summary) <> ''
          AND q.created_at >= :since
        """
            + SCOPE_FILTER
            + """
        GROUP BY q.route_summary
        ORDER BY COUNT(*) DESC
        """;
    List<Object[]> rows =
        entityManager
            .createNativeQuery(sql)
            .setParameter("since", since)
            .setParameter("scopeAll", scope.scopeAll())
            .setParameter("scopeDept", scope.scopeDept())
            .setParameter("scopeSelf", scope.scopeSelf())
            .setParameter("deptId", scope.deptId())
            .setParameter("userId", scope.userId())
            .setMaxResults(limit)
            .getResultList();
    return rows.stream().map(this::toObjectArray).toList();
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
            entityManager
                .createNativeQuery(sql)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("scopeAll", scope.scopeAll())
                .setParameter("scopeDept", scope.scopeDept())
                .setParameter("scopeSelf", scope.scopeSelf())
                .setParameter("deptId", scope.deptId())
                .setParameter("userId", scope.userId())
                .getSingleResult();
    return value.longValue();
  }
}
