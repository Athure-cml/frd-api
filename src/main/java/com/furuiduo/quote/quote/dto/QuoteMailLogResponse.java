package com.furuiduo.quote.quote.dto;

import com.furuiduo.quote.quote.entity.QuoteMailLog;
import com.furuiduo.quote.quote.support.QuoteDateTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "已报价发信流水")
public record QuoteMailLogResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "报价单 ID") Long quoteId,
    @Schema(description = "报价单号") String quoteNo,
    @Schema(description = "关联询价单 ID") Long inquiryId,
    @Schema(description = "收件邮箱") String toEmail,
    @Schema(description = "抄送") String cc,
    @Schema(description = "邮件主题") String subject,
    @Schema(description = "附件名") String attachmentName,
    @Schema(description = "状态：SENDING/SENT/FAILED/SKIPPED") String status,
    @Schema(description = "失败阶段：PREPARE/PDF/SMTP") String stage,
    @Schema(description = "跳过原因：MAIL_DISABLED/NO_INQUIRY/NO_OPERATOR_EMAIL/NO_FROM")
        String skipReason,
    @Schema(description = "错误码，如 535/550/PDF_RENDER") String errorCode,
    @Schema(description = "错误说明") String errorMessage,
    @Schema(description = "触发方式：CONFIRM/APPROVE/REVISION/RESEND/MANUAL") String triggerType,
    @Schema(description = "触发人") String operatorName,
    @Schema(description = "开始时间") String startedAt,
    @Schema(description = "结束时间") String finishedAt,
    @Schema(description = "创建时间") String createdAt) {

  public static QuoteMailLogResponse from(QuoteMailLog log) {
    return new QuoteMailLogResponse(
        log.getId(),
        log.getQuoteId(),
        log.getQuoteNo(),
        log.getInquiryId(),
        log.getToEmail(),
        log.getCc(),
        log.getSubject(),
        log.getAttachmentName(),
        log.getStatus() == null ? null : log.getStatus().name(),
        log.getStage() == null ? null : log.getStage().name(),
        log.getSkipReason() == null ? null : log.getSkipReason().name(),
        log.getErrorCode(),
        log.getErrorMessage(),
        log.getTriggerType() == null ? null : log.getTriggerType().name(),
        log.getOperatorName(),
        QuoteDateTimes.format(log.getStartedAt()),
        QuoteDateTimes.format(log.getFinishedAt()),
        QuoteDateTimes.format(log.getCreatedAt()));
  }
}
