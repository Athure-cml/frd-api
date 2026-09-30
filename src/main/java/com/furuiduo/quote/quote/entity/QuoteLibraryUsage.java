package com.furuiduo.quote.quote.entity;

import java.time.LocalDateTime;

import com.furuiduo.quote.cost.entity.CostHighlightMode;

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
@Table(name = "quote_library_usage")
public class QuoteLibraryUsage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "quote_id", nullable = false)
  private QuoteOrder quoteOrder;

  @Enumerated(EnumType.STRING)
  @Column(name = "cost_mode", nullable = false, length = 16)
  private CostHighlightMode costMode;

  @Column(name = "cost_id", nullable = false)
  private Long costId;

  @Column(name = "locked_at", nullable = false)
  private LocalDateTime lockedAt = LocalDateTime.now();
}
