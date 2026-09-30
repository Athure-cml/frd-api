package com.furuiduo.quote.quote.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.cost.entity.CostHighlightMode;
import com.furuiduo.quote.quote.entity.QuoteLibraryEntry;

public interface QuoteLibraryEntryRepository extends JpaRepository<QuoteLibraryEntry, Long> {

  boolean existsByCostModeAndCostId(CostHighlightMode costMode, Long costId);

  List<QuoteLibraryEntry> findByCostModeAndCostIdIn(
      CostHighlightMode costMode, Collection<Long> costIds);

  void deleteByCostModeAndCostId(CostHighlightMode costMode, Long costId);

  void deleteByCostModeAndCostIdIn(CostHighlightMode costMode, Collection<Long> costIds);

  @Query("select e.costId from QuoteLibraryEntry e where e.costMode = :mode")
  Set<Long> findCostIdsByCostMode(@Param("mode") CostHighlightMode mode);
}
