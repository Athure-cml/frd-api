package com.furuiduo.quote.currency.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.currency.entity.Currency;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {

  boolean existsByCode(String code);

  Optional<Currency> findByCode(String code);

  Optional<Currency> findByBaseTrue();

  @Query(
      """
      SELECT c FROM Currency c
      WHERE (:code IS NULL OR UPPER(c.code) LIKE UPPER(CONCAT('%', :code, '%')))
        AND (:name IS NULL OR UPPER(c.name) LIKE UPPER(CONCAT('%', :name, '%')))
        AND (:status IS NULL OR c.status = :status)
      ORDER BY c.sort ASC, c.code ASC
      """)
  List<Currency> search(
      @Param("code") String code, @Param("name") String name, @Param("status") Integer status);
}
