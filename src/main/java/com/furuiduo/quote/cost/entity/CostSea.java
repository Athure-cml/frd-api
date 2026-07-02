package com.furuiduo.quote.cost.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cost_sea")
public class CostSea {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 128)
  private String origin;

  @Column(length = 128)
  private String destination;

  @Column(length = 128)
  private String carrier;

  @Column(length = 32)
  private String spec;

  @Column(length = 16)
  private String unit;

  @Column(name = "unit_price", precision = 14, scale = 2)
  private BigDecimal unitPrice;

  @Column(precision = 14, scale = 2)
  private BigDecimal buc;

  @Column(name = "surcharge_valid_date")
  private LocalDate surchargeValidDate;

  @Column(name = "all_in", precision = 14, scale = 2)
  private BigDecimal allIn;

  @Column(name = "valid_date", length = 64)
  private String validDate;

  @Column(length = 8)
  private String currency;

  @Column(name = "valid_from")
  private LocalDate validFrom;

  @Column(name = "valid_to")
  private LocalDate validTo;

  @Enumerated(EnumType.STRING)
  @Column(length = 16)
  private CostStatus status = CostStatus.draft;

  @Column(length = 512)
  private String remark;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "extra_fields")
  private Map<String, Object> extraFields = new HashMap<>();

  @Column(name = "updated_at")
  private LocalDateTime updatedAt = LocalDateTime.now();

  public void touch() {
    updatedAt = LocalDateTime.now();
  }
}
