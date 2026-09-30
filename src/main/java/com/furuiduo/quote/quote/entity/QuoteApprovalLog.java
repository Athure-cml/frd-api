package com.furuiduo.quote.quote.entity;

import java.time.LocalDateTime;

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
@Table(name = "quote_approval_log")
public class QuoteApprovalLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "quote_order_id", nullable = false)
  private Long quoteOrderId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private QuoteApprovalAction action;

  @Column(name = "from_status", length = 32)
  private String fromStatus;

  @Column(name = "to_status", length = 32)
  private String toStatus;

  @Column(columnDefinition = "TEXT")
  private String comment;

  @Column(name = "operator_id", nullable = false)
  private Long operatorId;

  @Column(name = "operator_name", length = 64)
  private String operatorName;

  @Column(name = "node_title", length = 64)
  private String nodeTitle;

  @Column(name = "flow_snapshot", columnDefinition = "TEXT")
  private String flowSnapshot;

  /** 提交审批时的报价单打印模板快照 JSON */
  @Column(name = "print_snapshot", columnDefinition = "TEXT")
  private String printSnapshot;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();
}
