package com.furuiduo.quote.quote.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

  /** 服务类型多选：SEA / FUMIGATION / TRUCK / INSURANCE / TRADE / OTHER */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "service_types", nullable = false)
  private List<String> serviceTypes = new ArrayList<>();

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

  @Column(name = "fumigation_enabled", nullable = false)
  private Boolean fumigationEnabled = false;

  @Column(name = "fumigation_point", length = 64)
  private String fumigationPoint;

  @Enumerated(EnumType.STRING)
  @Column(name = "oak_type", length = 16)
  private QuoteOakType oakType;

  @Column(name = "doc_usd", length = 64)
  private String docUsd;

  @Column(name = "cargo_max_weight_ton", length = 128)
  private String cargoMaxWeightTon;

  @Column(name = "cif_amount", precision = 14, scale = 2)
  private BigDecimal cifAmount;

  @Column(name = "pick_up_address", length = 512)
  private String pickUpAddress;

  @Column(name = "trucking_fee", precision = 14, scale = 2)
  private BigDecimal truckingFee;

  @Column(name = "ns_lift", precision = 14, scale = 2)
  private BigDecimal nsLift;

  @Column(name = "chassis", precision = 14, scale = 2)
  private BigDecimal chassis;

  @Column(name = "waiting", precision = 14, scale = 2)
  private BigDecimal waiting;

  @Column(name = "redelivery_fee", precision = 14, scale = 2)
  private BigDecimal redeliveryFee;

  @Column(name = "truck_remark", length = 512)
  private String truckRemark;

  @Column(name = "cargo_insurance_premium", length = 128)
  private String cargoInsurancePremium;

  @Column(name = "cargo_agent_fee", length = 128)
  private String cargoAgentFee;

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

  @Column(name = "approved_by_name", length = 64)
  private String approvedByName;

  @Column(name = "approved_at")
  private LocalDateTime approvedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();

  @Column(name = "cost_risk_active", nullable = false)
  private Boolean costRiskActive = false;

  @Column(name = "cost_risk_reason", length = 512)
  private String costRiskReason;

  @Column(name = "cost_risk_at")
  private LocalDateTime costRiskAt;

  @Column(name = "cost_risk_dismissed_at")
  private LocalDateTime costRiskDismissedAt;

  @Column(name = "cost_risk_dismissed_by")
  private Long costRiskDismissedBy;

  /** 变更来源报价单 */
  @Column(name = "parent_quote_id")
  private Long parentQuoteId;

  /** 版本族根单（原版） */
  @Column(name = "root_quote_id")
  private Long rootQuoteId;

  /** 修订号：0=原版 */
  @Column(name = "revision_no", nullable = false)
  private Integer revisionNo = 0;

  /** 发起变更原因 */
  @Column(name = "change_reason", length = 512)
  private String changeReason;

  /** 是否当前对外生效版本 */
  @Column(name = "current_version", nullable = false)
  private Boolean currentVersion = true;

  /** 被哪张变更单替代 */
  @Column(name = "superseded_by_quote_id")
  private Long supersededByQuoteId;

  /** 软删时间：有审批记录的草稿删除后仍保留审批与打印快照 */
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @OneToMany(mappedBy = "quoteOrder", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sort ASC, id ASC")
  private List<QuoteOrderLine> lines = new ArrayList<>();
}
