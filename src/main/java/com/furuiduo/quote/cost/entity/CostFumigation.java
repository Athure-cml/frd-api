package com.furuiduo.quote.cost.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
  private String port;

  @Column(length = 64)
  private String station;

  @Column(name = "non_oak_outdoor", precision = 14, scale = 2)
  private BigDecimal nonOakOutdoor;

  @Column(name = "non_oak_indoor", precision = 14, scale = 2)
  private BigDecimal nonOakIndoor;

  @Column(name = "non_oak_quote_summer", length = 128)
  private String nonOakQuoteSummer;

  @Column(name = "non_oak_quote_winter", length = 128)
  private String nonOakQuoteWinter;

  @Column(name = "oak_outdoor", precision = 14, scale = 2)
  private BigDecimal oakOutdoor;

  @Column(name = "oak_indoor", precision = 14, scale = 2)
  private BigDecimal oakIndoor;

  @Column(name = "oak_quote_summer", length = 128)
  private String oakQuoteSummer;

  @Column(name = "oak_quote_winter", length = 128)
  private String oakQuoteWinter;

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
