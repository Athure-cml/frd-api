package com.furuiduo.quote.exchangerate.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "exchange_rate",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_exchange_rate_pair_date",
            columnNames = {"from_currency", "to_currency", "effective_date"}))
public class ExchangeRate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "from_currency", nullable = false, length = 8)
  private String fromCurrency;

  @Column(name = "to_currency", nullable = false, length = 8)
  private String toCurrency;

  /** 1 单位 from_currency 折合多少 to_currency */
  @Column(nullable = false, precision = 18, scale = 8)
  private BigDecimal rate;

  @Column(name = "effective_date", nullable = false)
  private LocalDate effectiveDate;

  @Column(nullable = false)
  private Integer status = 1;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
