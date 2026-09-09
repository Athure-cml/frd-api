package com.furuiduo.quote.quoterule.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
@Table(name = "md_quote_rule")
public class MdQuoteRule {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 128)
  private String name;

  @Column(name = "target_field", nullable = false, length = 32)
  private String targetField;

  @Column(name = "condition_type", nullable = false, length = 32)
  private String conditionType = "ALWAYS";

  @Column(name = "condition_amount", precision = 18, scale = 4)
  private BigDecimal conditionAmount;

  @Column(name = "calc_type", nullable = false, length = 32)
  private String calcType;

  @Column(name = "add_amount", precision = 18, scale = 4)
  private BigDecimal addAmount;

  @Column(name = "fixed_amount", precision = 18, scale = 4)
  private BigDecimal fixedAmount;

  @Column(name = "cif_factor", precision = 18, scale = 6)
  private BigDecimal cifFactor;

  @Column(name = "cif_rate", precision = 18, scale = 6)
  private BigDecimal cifRate;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder = 0;

  @Column(nullable = false)
  private Integer status = 1;

  @Column(columnDefinition = "TEXT")
  private String remark;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
