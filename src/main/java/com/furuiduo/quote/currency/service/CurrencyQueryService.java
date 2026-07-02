package com.furuiduo.quote.currency.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.furuiduo.quote.common.SearchText;
import com.furuiduo.quote.currency.dto.CurrencyResponse;
import com.furuiduo.quote.currency.repository.CurrencyRepository;

@Service
public class CurrencyQueryService {

  private final CurrencyRepository currencyRepository;

  public CurrencyQueryService(CurrencyRepository currencyRepository) {
    this.currencyRepository = currencyRepository;
  }

  public List<CurrencyResponse> list(String code, String name, Integer status) {
    String normalizedCode = SearchText.orEmpty(code);
    String normalizedName = SearchText.orEmpty(name);
    if (normalizedCode.isEmpty() && normalizedName.isEmpty()) {
      if (status == null) {
        return currencyRepository.findAll(Sort.by("sort").ascending().and(Sort.by("code"))).stream()
            .map(CurrencyResponse::from)
            .toList();
      }
      return currencyRepository.findByStatusOrderBySortAscCodeAsc(status).stream()
          .map(CurrencyResponse::from)
          .toList();
    }
    return currencyRepository.search(normalizedCode, normalizedName, status).stream()
        .map(CurrencyResponse::from)
        .toList();
  }
}
