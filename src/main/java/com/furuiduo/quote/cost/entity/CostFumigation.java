package com.furuiduo.quote.cost.entity;

import java.math.BigDecimal;
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
@Table(name = "cost_fumigation")
public class CostFumigation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 64)
  private String region;

  @Column(length = 64)
  private String station;

  @Column(name = "outdoor_non_oak", precision = 14, scale = 2)
  private BigDecimal outdoorNonOak;

  @Column(name = "outdoor_oak", precision = 14, scale = 2)
  private BigDecimal outdoorOak;

  @Column(name = "outdoor_validity", length = 128)
  private String outdoorValidity;

  @Column(name = "indoor_non_oak", precision = 14, scale = 2)
  private BigDecimal indoorNonOak;

  @Column(name = "indoor_oak", precision = 14, scale = 2)
  private BigDecimal indoorOak;

  @Column(name = "indoor_validity", length = 128)
  private String indoorValidity;

  @Column(length = 512)
  private String address;

  @Enumerated(EnumType.STRING)
  @Column(length = 16)
  private CostStatus status = CostStatus.active;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "extra_fields")
  private Map<String, Object> extraFields = new HashMap<>();

  @Column(name = "updated_at")
  private LocalDateTime updatedAt = LocalDateTime.now();

  public void touch() {
    updatedAt = LocalDateTime.now();
  }
}
