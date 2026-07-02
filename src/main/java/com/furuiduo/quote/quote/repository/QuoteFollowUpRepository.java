package com.furuiduo.quote.quote.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.furuiduo.quote.quote.entity.QuoteFollowUp;

public interface QuoteFollowUpRepository extends JpaRepository<QuoteFollowUp, Long> {

  List<QuoteFollowUp> findByQuoteOrderIdOrderByFollowUpAtDesc(Long quoteId);
}
