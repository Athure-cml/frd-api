package com.furuiduo.quote.quote.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.quote.entity.QuoteCostSnapshot;
import com.furuiduo.quote.quote.entity.QuoteCostType;

public interface QuoteCostSnapshotRepository extends JpaRepository<QuoteCostSnapshot, Long> {

  List<QuoteCostSnapshot> findByQuoteOrderIdOrderByCreatedAtDesc(Long quoteId);

  List<QuoteCostSnapshot> findByQuoteOrderIdAndCostTypeOrderByCreatedAtDesc(
      Long quoteId, QuoteCostType costType);

  List<QuoteCostSnapshot> findByCostTypeAndCostRefId(QuoteCostType costType, Long costRefId);

  void deleteByQuoteOrderId(Long quoteOrderId);

  @Query(
      """
      select s from QuoteCostSnapshot s
      join fetch s.quoteOrder
      where s.quoteOrder.id in :quoteIds and s.costType = :costType
      order by s.createdAt desc
      """)
  List<QuoteCostSnapshot> findByQuoteOrderIdInAndCostTypeOrderByCreatedAtDesc(
      @Param("quoteIds") Collection<Long> quoteIds, @Param("costType") QuoteCostType costType);

  /** 被已成交报价单引用的成本 ID */
  @Query(
      """
      SELECT DISTINCT s.costRefId FROM QuoteCostSnapshot s
      WHERE s.costType = :costType
        AND s.costRefId IN :costIds
        AND s.quoteOrder.status = com.furuiduo.quote.quote.entity.QuoteStatus.WON
      """)
  List<Long> findCostRefIdsLockedByWonQuotes(
      @Param("costType") QuoteCostType costType, @Param("costIds") Collection<Long> costIds);

  @Query(
      """
      SELECT COUNT(s) > 0 FROM QuoteCostSnapshot s
      WHERE s.costType = :costType
        AND s.costRefId = :costId
        AND s.quoteOrder.status = com.furuiduo.quote.quote.entity.QuoteStatus.WON
      """)
  boolean existsWonLockByCostTypeAndCostRefId(
      @Param("costType") QuoteCostType costType, @Param("costId") Long costId);
}
