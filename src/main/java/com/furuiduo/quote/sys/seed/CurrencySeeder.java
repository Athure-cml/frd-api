package com.furuiduo.quote.sys.seed;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.currency.entity.Currency;
import com.furuiduo.quote.currency.repository.CurrencyRepository;
import com.furuiduo.quote.exchangerate.entity.ExchangeRate;
import com.furuiduo.quote.exchangerate.repository.ExchangeRateRepository;

@Component
@Order(99)
public class CurrencySeeder implements ApplicationRunner {

  private record CurrencyDef(
      String code, String name, String symbol, int decimalPlaces, boolean base, int sort) {}

  private static final List<CurrencyDef> CURRENCIES =
      List.of(
          new CurrencyDef("CNY", "人民币", "¥", 2, true, 1),
          new CurrencyDef("USD", "美元", "$", 2, false, 2),
          new CurrencyDef("EUR", "欧元", "€", 2, false, 3),
          new CurrencyDef("HKD", "港币", "HK$", 2, false, 4));

  private final CurrencyRepository currencyRepository;
  private final ExchangeRateRepository exchangeRateRepository;

  public CurrencySeeder(
      CurrencyRepository currencyRepository, ExchangeRateRepository exchangeRateRepository) {
    this.currencyRepository = currencyRepository;
    this.exchangeRateRepository = exchangeRateRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    for (CurrencyDef def : CURRENCIES) {
      currencyRepository
          .findByCode(def.code())
          .ifPresentOrElse(
              existing -> {
                existing.setName(def.name());
                existing.setSymbol(def.symbol());
                existing.setDecimalPlaces(def.decimalPlaces());
                existing.setSort(def.sort());
                if (def.base()) {
                  existing.setBase(true);
                }
                currencyRepository.save(existing);
              },
              () -> {
                Currency currency = new Currency();
                currency.setCode(def.code());
                currency.setName(def.name());
                currency.setSymbol(def.symbol());
                currency.setDecimalPlaces(def.decimalPlaces());
                currency.setBase(def.base());
                currency.setSort(def.sort());
                currencyRepository.save(currency);
              });
    }

    ensureBaseCurrencyUnique();
    seedDefaultRates();
  }

  private void ensureBaseCurrencyUnique() {
    currencyRepository.findAll().forEach(item -> {
      if (!"CNY".equals(item.getCode()) && Boolean.TRUE.equals(item.getBase())) {
        item.setBase(false);
        currencyRepository.save(item);
      }
    });
    currencyRepository.findByCode("CNY").ifPresent(cny -> {
      cny.setBase(true);
      currencyRepository.save(cny);
    });
  }

  private void seedDefaultRates() {
    LocalDate today = LocalDate.now();
    seedRateIfMissing("USD", "CNY", new BigDecimal("7.25000000"), today);
    seedRateIfMissing("EUR", "CNY", new BigDecimal("7.85000000"), today);
    seedRateIfMissing("HKD", "CNY", new BigDecimal("0.93000000"), today);
  }

  private void seedRateIfMissing(
      String from, String to, BigDecimal rate, LocalDate effectiveDate) {
    boolean exists =
        exchangeRateRepository
            .findFirstByFromCurrencyAndToCurrencyAndEffectiveDateLessThanEqualAndStatusOrderByEffectiveDateDesc(
                from, to, effectiveDate, 1)
            .isPresent();
    if (exists) {
      return;
    }
    ExchangeRate exchangeRate = new ExchangeRate();
    exchangeRate.setFromCurrency(from);
    exchangeRate.setToCurrency(to);
    exchangeRate.setRate(rate);
    exchangeRate.setEffectiveDate(effectiveDate);
    exchangeRateRepository.save(exchangeRate);
  }
}
