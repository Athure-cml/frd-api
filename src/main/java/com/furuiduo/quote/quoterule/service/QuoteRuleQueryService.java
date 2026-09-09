package com.furuiduo.quote.quoterule.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.common.SearchText;
import com.furuiduo.quote.quoterule.dto.QuoteRuleResponse;
import com.furuiduo.quote.quoterule.repository.MdQuoteRuleRepository;

@Service
public class QuoteRuleQueryService {

  private final MdQuoteRuleRepository quoteRuleRepository;

  public QuoteRuleQueryService(MdQuoteRuleRepository quoteRuleRepository) {
    this.quoteRuleRepository = quoteRuleRepository;
  }

  @Transactional(readOnly = true)
  public List<QuoteRuleResponse> list(String name, String targetField, Integer status) {
    return quoteRuleRepository
        .search(
            SearchText.orEmpty(name),
            SearchText.orEmpty(targetField),
            status)
        .stream()
        .map(QuoteRuleResponse::from)
        .toList();
  }
}
