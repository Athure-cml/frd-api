package com.furuiduo.quote.quote.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.furuiduo.quote.quote.entity.QuoteCostSnapshot;
import com.furuiduo.quote.quote.entity.QuoteCostType;

public interface QuoteCostSnapshotRepository extends JpaRepository<QuoteCostSnapshot, Long> {

  List<QuoteCostSnapshot> findByQuoteOrderIdOrderByCreatedAtDesc(Long quoteId);

  List<QuoteCostSnapshot> findByQuoteOrderIdAndCostTypeOrderByCreatedAtDesc(
      Long quoteId, QuoteCostType costType);
}
