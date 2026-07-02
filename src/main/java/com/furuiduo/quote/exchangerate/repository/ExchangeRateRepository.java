package com.furuiduo.quote.exchangerate.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.exchangerate.entity.ExchangeRate;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

  @Query(
      """
      SELECT e FROM ExchangeRate e
      WHERE (:fromCurrency IS NULL OR e.fromCurrency = :fromCurrency)
        AND (:toCurrency IS NULL OR e.toCurrency = :toCurrency)
        AND (:status IS NULL OR e.status = :status)
      ORDER BY e.effectiveDate DESC, e.fromCurrency ASC, e.toCurrency ASC
      """)
  List<ExchangeRate> search(
      @Param("fromCurrency") String fromCurrency,
      @Param("toCurrency") String toCurrency,
      @Param("status") Integer status);

  Optional<ExchangeRate>
      findFirstByFromCurrencyAndToCurrencyAndEffectiveDateLessThanEqualAndStatusOrderByEffectiveDateDesc(
          String fromCurrency, String toCurrency, LocalDate effectiveDate, Integer status);
}
