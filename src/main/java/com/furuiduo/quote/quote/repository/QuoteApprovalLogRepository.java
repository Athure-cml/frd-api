package com.furuiduo.quote.quote.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.furuiduo.quote.quote.entity.QuoteApprovalAction;
import com.furuiduo.quote.quote.entity.QuoteApprovalLog;

public interface QuoteApprovalLogRepository extends JpaRepository<QuoteApprovalLog, Long> {

  List<QuoteApprovalLog> findByQuoteOrderIdOrderByCreatedAtAscIdAsc(Long quoteOrderId);

  List<QuoteApprovalLog> findByQuoteOrderIdInOrderByCreatedAtAscIdAsc(
      Collection<Long> quoteOrderIds);

  boolean existsByQuoteOrderId(Long quoteOrderId);
}
