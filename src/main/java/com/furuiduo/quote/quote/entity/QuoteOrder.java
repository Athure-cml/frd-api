package com.furuiduo.quote.quote.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "quote_order")
public class QuoteOrder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "quote_no", nullable = false, unique = true, length = 32)
  private String quoteNo;

  @Column(name = "customer_id")
  private Long customerId;

  @Column(name = "customer_name", nullable = false, length = 128)
  private String customerName;

  @Enumerated(EnumType.STRING)
  @Column(name = "transport_mode", nullable = false, length = 16)
  private QuoteTransportMode transportMode;

  @Column(name = "route_summary", length = 256)
  private String routeSummary;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private QuoteStatus status = QuoteStatus.DRAFT;

  @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  @Column(nullable = false, length = 8)
  private String currency = "CNY";

  @Column(name = "base_currency", nullable = false, length = 8)
  private String baseCurrency = "CNY";

  @Column(name = "exchange_rate", precision = 18, scale = 8)
  private BigDecimal exchangeRate;

  @Column(name = "valid_until")
  private LocalDate validUntil;

  @Column(name = "zip_code", length = 16)
  private String zipCode;

  @Column(length = 128)
  private String city;

  @Column(length = 32)
  private String state;

  @Column(length = 64)
  private String por;

  @Column(length = 64)
  private String pol;

  @Column(length = 64)
  private String pod;

  @Column(name = "o_f_usd", length = 128)
  private String ofUsd;

  @Column(length = 128)
  private String ssl;

  @Column(name = "trucking_non_oak_usd", precision = 14, scale = 2)
  private BigDecimal truckingNonOakUsd;

  @Column(name = "trucking_oak_usd", precision = 14, scale = 2)
  private BigDecimal truckingOakUsd;

  @Column(name = "fm_non_oak", precision = 14, scale = 2)
  private BigDecimal fmNonOak;

  @Column(name = "fm_oak", precision = 14, scale = 2)
  private BigDecimal fmOak;

  @Column(name = "doc_usd", length = 64)
  private String docUsd;

  @Column(name = "cargo_max_weight_ton", length = 128)
  private String cargoMaxWeightTon;

  @Column(name = "sheet_remark", length = 1024)
  private String sheetRemark;

  @Column(name = "follow_up_by")
  private Long followUpBy;

  @Column(name = "follow_up_by_name", length = 64)
  private String followUpByName;

  @Column(length = 512)
  private String remark;

  @Column(name = "created_by", nullable = false)
  private Long createdBy;

  @Column(name = "created_by_name", length = 64)
  private String createdByName;

  @Column(name = "dept_id")
  private Long deptId;

  @Column(name = "submitted_at")
  private LocalDateTime submittedAt;

  @Column(name = "approved_by")
  private Long approvedBy;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();

  @OneToMany(mappedBy = "quoteOrder", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sort ASC, id ASC")
  private List<QuoteOrderLine> lines = new ArrayList<>();
}
