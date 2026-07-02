package com.furuiduo.quote.exchangerate.support;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.exchangerate.entity.ExchangeRate;
import com.furuiduo.quote.exchangerate.repository.ExchangeRateRepository;

@Component
public class ExchangeRateResolver {

  private final ExchangeRateRepository exchangeRateRepository;

  public ExchangeRateResolver(ExchangeRateRepository exchangeRateRepository) {
    this.exchangeRateRepository = exchangeRateRepository;
  }

  public BigDecimal resolve(String fromCurrency, String toCurrency, LocalDate asOf) {
    if (fromCurrency.equalsIgnoreCase(toCurrency)) {
      return BigDecimal.ONE;
    }
    LocalDate date = asOf == null ? LocalDate.now() : asOf;
    ExchangeRate rate =
        exchangeRateRepository
            .findFirstByFromCurrencyAndToCurrencyAndEffectiveDateLessThanEqualAndStatusOrderByEffectiveDateDesc(
                fromCurrency.toUpperCase(), toCurrency.toUpperCase(), date, 1)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "未找到有效汇率：" + fromCurrency + " → " + toCurrency));
    return rate.getRate();
  }
}
