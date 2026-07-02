package com.furuiduo.quote.currency.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.furuiduo.quote.currency.dto.CurrencyResponse;
import com.furuiduo.quote.currency.repository.CurrencyRepository;

@Service
public class CurrencyQueryService {

  private final CurrencyRepository currencyRepository;

  public CurrencyQueryService(CurrencyRepository currencyRepository) {
    this.currencyRepository = currencyRepository;
  }

  public List<CurrencyResponse> list(String code, String name, Integer status) {
    return currencyRepository.search(trim(code), trim(name), status).stream()
        .map(CurrencyResponse::from)
        .toList();
  }

  private String trim(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
