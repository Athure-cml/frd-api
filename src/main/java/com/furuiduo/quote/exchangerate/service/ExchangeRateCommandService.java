package com.furuiduo.quote.exchangerate.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.currency.service.CurrencyCommandService;
import com.furuiduo.quote.exchangerate.dto.ExchangeRateResponse;
import com.furuiduo.quote.exchangerate.dto.ExchangeRateSaveRequest;
import com.furuiduo.quote.exchangerate.entity.ExchangeRate;
import com.furuiduo.quote.exchangerate.repository.ExchangeRateRepository;

@Service
public class ExchangeRateCommandService {

  private final ExchangeRateRepository exchangeRateRepository;
  private final CurrencyCommandService currencyCommandService;

  public ExchangeRateCommandService(
      ExchangeRateRepository exchangeRateRepository,
      CurrencyCommandService currencyCommandService) {
    this.exchangeRateRepository = exchangeRateRepository;
    this.currencyCommandService = currencyCommandService;
  }

  @Transactional(readOnly = true)
  public ExchangeRateResponse getById(Long id) {
    return ExchangeRateResponse.from(requireEntity(id));
  }

  @Transactional
  public ExchangeRateResponse create(ExchangeRateSaveRequest request) {
    validateSaveRequest(request);
    ExchangeRate rate = new ExchangeRate();
    apply(rate, request);
    return ExchangeRateResponse.from(exchangeRateRepository.save(rate));
  }

  @Transactional
  public ExchangeRateResponse update(Long id, ExchangeRateSaveRequest request) {
    ExchangeRate rate = requireEntity(id);
    validateSaveRequest(request);
    apply(rate, request);
    rate.setUpdatedAt(LocalDateTime.now());
    return ExchangeRateResponse.from(exchangeRateRepository.save(rate));
  }

  @Transactional
  public void delete(Long id) {
    exchangeRateRepository.delete(requireEntity(id));
  }

  private ExchangeRate requireEntity(Long id) {
    return exchangeRateRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "汇率不存在"));
  }

  private void validateSaveRequest(ExchangeRateSaveRequest request) {
    if (request.fromCurrency() == null || request.fromCurrency().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "源币种不能为空");
    }
    if (request.toCurrency() == null || request.toCurrency().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "目标币种不能为空");
    }
    if (request.effectiveDate() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "生效日期不能为空");
    }
    if (request.rate() == null || request.rate().compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "汇率必须大于 0");
    }
    String from = request.fromCurrency().trim().toUpperCase();
    String to = request.toCurrency().trim().toUpperCase();
    if (from.equals(to)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "源币种与目标币种不能相同");
    }
    currencyCommandService.requireEnabled(from);
    currencyCommandService.requireEnabled(to);
  }

  private void apply(ExchangeRate rate, ExchangeRateSaveRequest request) {
    rate.setFromCurrency(request.fromCurrency().trim().toUpperCase());
    rate.setToCurrency(request.toCurrency().trim().toUpperCase());
    rate.setRate(request.rate());
    rate.setEffectiveDate(request.effectiveDate());
    rate.setStatus(request.status() == null ? 1 : request.status());
  }
}
