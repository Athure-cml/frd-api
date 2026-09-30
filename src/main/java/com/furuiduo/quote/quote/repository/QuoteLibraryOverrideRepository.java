package com.furuiduo.quote.quote.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.furuiduo.quote.cost.entity.CostHighlightMode;
import com.furuiduo.quote.quote.entity.QuoteLibraryOverride;

public interface QuoteLibraryOverrideRepository
    extends JpaRepository<QuoteLibraryOverride, Long> {

  List<QuoteLibraryOverride> findByCostModeAndCostIdIn(
      CostHighlightMode costMode, Collection<Long> costIds);

  void deleteByCostModeAndCostIdIn(CostHighlightMode costMode, Collection<Long> costIds);

  void deleteByCostModeAndCostId(CostHighlightMode costMode, Long costId);
}
