package com.furuiduo.quote.exchangerate.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.furuiduo.quote.exchangerate.dto.ExchangeRateResponse;
import com.furuiduo.quote.exchangerate.repository.ExchangeRateRepository;

@Service
public class ExchangeRateQueryService {

  private final ExchangeRateRepository exchangeRateRepository;

  public ExchangeRateQueryService(ExchangeRateRepository exchangeRateRepository) {
    this.exchangeRateRepository = exchangeRateRepository;
  }

  public List<ExchangeRateResponse> list(
      String fromCurrency, String toCurrency, Integer status) {
    return exchangeRateRepository
        .search(normalize(fromCurrency), normalize(toCurrency), status)
        .stream()
        .map(ExchangeRateResponse::from)
        .toList();
  }

  private String normalize(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return value.trim().toUpperCase();
  }
}
