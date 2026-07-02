package com.furuiduo.quote.quote.entity;

import java.time.LocalDateTime;
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
@Table(name = "quote_cost_snapshot")
public class QuoteCostSnapshot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "quote_id", nullable = false)
  private QuoteOrder quoteOrder;

  @Enumerated(EnumType.STRING)
  @Column(name = "cost_type", nullable = false, length = 16)
  private QuoteCostType costType;

  @Column(name = "cost_ref_id", nullable = false)
  private Long costRefId;

  @Column(name = "cost_version", length = 64)
  private String costVersion;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "match_keys_json")
  private Map<String, Object> matchKeysJson = new HashMap<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "snapshot_json", nullable = false)
  private Map<String, Object> snapshotJson = new HashMap<>();

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();
}
