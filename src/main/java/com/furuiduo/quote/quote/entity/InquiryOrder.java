package com.furuiduo.quote.quote.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
@Table(name = "inquiry_order")
public class InquiryOrder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "inquiry_no", nullable = false, unique = true, length = 32)
  private String inquiryNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private InquiryStatus status = InquiryStatus.PENDING_SUBMIT;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "service_types", nullable = false)
  private List<String> serviceTypes = new ArrayList<>();

  @Column(name = "customer_id")
  private Long customerId;

  @Column(name = "customer_name", nullable = false, length = 128)
  private String customerName;

  @Enumerated(EnumType.STRING)
  @Column(name = "transport_mode", nullable = false, length = 16)
  private QuoteTransportMode transportMode = QuoteTransportMode.SEA;

  @Column(name = "route_summary", length = 256)
  private String routeSummary;

  @Column(name = "quote_date")
  private LocalDate quoteDate;

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

  @Column(name = "fumigation_enabled", nullable = false)
  private Boolean fumigationEnabled = false;

  @Column(name = "fumigation_point", length = 64)
  private String fumigationPoint;

  @Enumerated(EnumType.STRING)
  @Column(name = "oak_type", length = 16)
  private QuoteOakType oakType;

  @Column(name = "pick_up_address", length = 512)
  private String pickUpAddress;

  @Column(length = 512)
  private String remark;

  /** 递交后生成的报价单 */
  @Column(name = "quote_id")
  private Long quoteId;

  @Column(name = "submitted_at")
  private LocalDateTime submittedAt;

  @Column(name = "created_by", nullable = false)
  private Long createdBy;

  @Column(name = "created_by_name", length = 64)
  private String createdByName;

  @Column(name = "dept_id")
  private Long deptId;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public void touch() {
    this.updatedAt = LocalDateTime.now();
  }
}
