package com.furuiduo.quote.quote.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "quote_follow_up")
public class QuoteFollowUp {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "quote_id", nullable = false)
  private QuoteOrder quoteOrder;

  @Column(name = "follow_status", nullable = false, length = 32)
  private String followStatus;

  @Column(nullable = false, length = 2000)
  private String content;

  @Column(name = "follow_up_by", nullable = false)
  private Long followUpBy;

  @Column(name = "follow_up_by_name", nullable = false, length = 64)
  private String followUpByName;

  @Column(name = "follow_up_at", nullable = false)
  private LocalDateTime followUpAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
