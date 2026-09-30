package com.furuiduo.quote.quote.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.cost.entity.CostHighlightMode;
import com.furuiduo.quote.quote.entity.QuoteLibraryUsage;

public interface QuoteLibraryUsageRepository extends JpaRepository<QuoteLibraryUsage, Long> {

  List<QuoteLibraryUsage> findByQuoteOrderId(Long quoteOrderId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("delete from QuoteLibraryUsage u where u.quoteOrder.id = :quoteId")
  void deleteByQuoteOrderId(@Param("quoteId") Long quoteId);

  boolean existsByCostModeAndCostId(CostHighlightMode costMode, Long costId);

  long countByCostModeAndCostIdIn(CostHighlightMode costMode, Collection<Long> costIds);

  List<QuoteLibraryUsage> findByCostModeAndCostIdIn(
      CostHighlightMode costMode, Collection<Long> costIds);
}
