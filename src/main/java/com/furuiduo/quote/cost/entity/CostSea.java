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
@Table(name = "cost_sea")
public class CostSea {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 128)
  private String por;

  @Column(length = 128)
  private String pol;

  @Column(length = 128)
  private String pod;

  @Column(name = "cn_short_name", length = 128)
  private String cnShortName;

  @Column(name = "en_product_name", length = 256)
  private String enProductName;

  @Column(name = "container_type", length = 64)
  private String containerType;

  @Column(precision = 14, scale = 2)
  private BigDecimal freight;

  @Column(name = "freight_valid_date", length = 64)
  private String freightValidDate;

  @Column(precision = 14, scale = 2)
  private BigDecimal buc;

  @Column(name = "buc_valid_date", length = 128)
  private String bucValidDate;

  @Column(precision = 14, scale = 2)
  private BigDecimal ebs;

  @Column(name = "ebs_valid_date", length = 128)
  private String ebsValidDate;

  @Column(precision = 14, scale = 2)
  private BigDecimal gri;

  @Column(name = "gri_valid_date", length = 128)
  private String griValidDate;

  @Column(precision = 14, scale = 2)
  private BigDecimal others;

  @Column(name = "others_valid_date", length = 128)
  private String othersValidDate;

  @Column(name = "all_in", precision = 14, scale = 2)
  private BigDecimal allIn;

  @Column(length = 128)
  private String ssl;

  @Column(length = 128)
  private String agent;

  @Enumerated(EnumType.STRING)
  @Column(length = 16)
  private CostStatus status = CostStatus.active;

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
