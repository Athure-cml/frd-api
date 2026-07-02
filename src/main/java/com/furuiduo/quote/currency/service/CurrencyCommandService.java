package com.furuiduo.quote.currency.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.currency.dto.CurrencyResponse;
import com.furuiduo.quote.currency.dto.CurrencySaveRequest;
import com.furuiduo.quote.currency.entity.Currency;
import com.furuiduo.quote.currency.repository.CurrencyRepository;
import com.furuiduo.quote.exchangerate.repository.ExchangeRateRepository;
import com.furuiduo.quote.quote.repository.QuoteOrderRepository;

@Service
public class CurrencyCommandService {

  private final CurrencyRepository currencyRepository;
  private final ExchangeRateRepository exchangeRateRepository;
  private final QuoteOrderRepository quoteOrderRepository;

  public CurrencyCommandService(
      CurrencyRepository currencyRepository,
      ExchangeRateRepository exchangeRateRepository,
      QuoteOrderRepository quoteOrderRepository) {
    this.currencyRepository = currencyRepository;
    this.exchangeRateRepository = exchangeRateRepository;
    this.quoteOrderRepository = quoteOrderRepository;
  }

  @Transactional(readOnly = true)
  public CurrencyResponse getById(Long id) {
    return CurrencyResponse.from(requireEntity(id));
  }

  @Transactional(readOnly = true)
  public Currency requireEnabled(String code) {
    Currency currency =
        currencyRepository
            .findByCode(normalizeCode(code))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "币种不存在：" + code));
    if (currency.getStatus() != 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "币种已停用：" + code);
    }
    return currency;
  }

  @Transactional(readOnly = true)
  public String getBaseCurrencyCode() {
    return currencyRepository
        .findByBaseTrue()
        .map(Currency::getCode)
        .orElse("CNY");
  }

  @Transactional
  public CurrencyResponse create(CurrencySaveRequest request) {
    validateSaveRequest(request, null);
    Currency currency = new Currency();
    apply(currency, request);
    return CurrencyResponse.from(currencyRepository.save(currency));
  }

  @Transactional
  public CurrencyResponse update(Long id, CurrencySaveRequest request) {
    Currency currency = requireEntity(id);
    validateSaveRequest(request, currency);
    apply(currency, request);
    currency.setUpdatedAt(LocalDateTime.now());
    return CurrencyResponse.from(currencyRepository.save(currency));
  }

  @Transactional
  public void delete(Long id) {
    Currency currency = requireEntity(id);
    if (Boolean.TRUE.equals(currency.getBase())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "基准币不可删除");
    }
    if (quoteOrderRepository.existsByCurrency(currency.getCode())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "币种已被报价单引用，无法删除");
    }
    long rateCount =
        exchangeRateRepository.search(currency.getCode(), null, null).size()
            + exchangeRateRepository.search(null, currency.getCode(), null).size();
    if (rateCount > 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "币种已配置汇率，无法删除");
    }
    currencyRepository.delete(currency);
  }

  private Currency requireEntity(Long id) {
    return currencyRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "币种不存在"));
  }

  private void validateSaveRequest(CurrencySaveRequest request, Currency existing) {
    if (request.code() == null || request.code().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "币种编码不能为空");
    }
    if (request.name() == null || request.name().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "币种名称不能为空");
    }
    String code = normalizeCode(request.code());
    if (existing == null && currencyRepository.existsByCode(code)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "币种编码已存在");
    }
    if (existing != null
        && !existing.getCode().equals(code)
        && currencyRepository.existsByCode(code)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "币种编码已存在");
    }
  }

  private void apply(Currency currency, CurrencySaveRequest request) {
    currency.setCode(normalizeCode(request.code()));
    currency.setName(request.name().trim());
    currency.setSymbol(trimToNull(request.symbol()));
    currency.setDecimalPlaces(request.decimalPlaces() == null ? 2 : request.decimalPlaces());
    currency.setSort(request.sort() == null ? 0 : request.sort());
    currency.setStatus(request.status() == null ? 1 : request.status());
    boolean base = Boolean.TRUE.equals(request.base());
    if (base) {
      clearOtherBaseCurrencies(currency.getId());
    }
    currency.setBase(base);
  }

  private void clearOtherBaseCurrencies(Long keepId) {
    List<Currency> bases = currencyRepository.findAll().stream().filter(Currency::getBase).toList();
    for (Currency item : bases) {
      if (keepId == null || !keepId.equals(item.getId())) {
        item.setBase(false);
        item.setUpdatedAt(LocalDateTime.now());
        currencyRepository.save(item);
      }
    }
  }

  private String normalizeCode(String code) {
    return code.trim().toUpperCase();
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
