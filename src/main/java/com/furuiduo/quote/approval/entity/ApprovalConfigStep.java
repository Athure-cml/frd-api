package com.furuiduo.quote.approval.entity;

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
@Table(name = "approval_config_step")
public class ApprovalConfigStep {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "config_id", nullable = false)
  private ApprovalConfig config;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder;

  @Column(name = "approver_id", nullable = false)
  private Long approverId;

  @Column(name = "approver_name", nullable = false, length = 64)
  private String approverName;
}
