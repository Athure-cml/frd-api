package com.furuiduo.quote.quote.entity;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "quote_order_line")
public class QuoteOrderLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "quote_id", nullable = false)
  private QuoteOrder quoteOrder;

  @Column(nullable = false)
  private Integer sort = 0;

  @Column(name = "item_name", nullable = false, length = 128)
  private String itemName;

  @Column(length = 256)
  private String spec;

  @Enumerated(EnumType.STRING)
  @Column(name = "cost_mode", nullable = false, length = 16)
  private QuoteCostMode costMode = QuoteCostMode.MANUAL;

  @Column(name = "cost_ref_id")
  private Long costRefId;

  @Column(nullable = false, precision = 18, scale = 4)
  private BigDecimal quantity = BigDecimal.ONE;

  @Column(length = 16)
  private String unit;

  @Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
  private BigDecimal unitPrice = BigDecimal.ZERO;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal amount = BigDecimal.ZERO;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "extra_json")
  private Map<String, Object> extraJson = new HashMap<>();
}
