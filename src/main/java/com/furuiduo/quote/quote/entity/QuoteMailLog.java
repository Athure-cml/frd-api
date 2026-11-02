package com.furuiduo.quote.quote.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 已报价自动发信流水。一次报价单可有确认、审批、变更、重发多次记录。
 *
 * <p>status: SENDING / SENT / FAILED / SKIPPED
 */
@Getter
@Setter
@Entity
@Table(
    name = "quote_mail_log",
    indexes = {
      @Index(name = "idx_quote_mail_log_quote", columnList = "quote_id"),
      @Index(name = "idx_quote_mail_log_status", columnList = "status"),
    })
public class QuoteMailLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "quote_id", nullable = false)
  private Long quoteId;

  @Column(name = "quote_no", length = 64)
  private String quoteNo;

  @Column(name = "inquiry_id")
  private Long inquiryId;

  @Column(name = "to_email", length = 255)
  private String toEmail;

  @Column(length = 500)
  private String cc;

  @Column(length = 500)
  private String subject;

  @Column(name = "attachment_name", length = 255)
  private String attachmentName;

  /** SENDING / SENT / FAILED / SKIPPED */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private QuoteMailStatus status;

  /** 失败阶段：PREPARE / PDF / SMTP */
  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private QuoteMailStage stage;

  /** 跳过原因：MAIL_DISABLED / NO_INQUIRY / NO_OPERATOR_EMAIL / NO_FROM */
  @Enumerated(EnumType.STRING)
  @Column(name = "skip_reason", length = 40)
  private QuoteMailSkipReason skipReason;

  /** SMTP/HTTP 返回码，如 535、550 */
  @Column(name = "error_code", length = 20)
  private String errorCode;

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  /** CONFIRM / APPROVE / REVISION / RESEND */
  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_type", nullable = false, length = 20)
  private QuoteMailTrigger triggerType;

  @Column(name = "operator_id")
  private Long operatorId;

  @Column(name = "operator_name", length = 64)
  private String operatorName;

  @Column(name = "started_at")
  private LocalDateTime startedAt;

  @Column(name = "finished_at")
  private LocalDateTime finishedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    if (startedAt == null) {
      startedAt = createdAt;
    }
  }
}
