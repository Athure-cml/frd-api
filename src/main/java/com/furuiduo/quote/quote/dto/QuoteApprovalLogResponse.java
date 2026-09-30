package com.furuiduo.quote.quote.dto;

import com.furuiduo.quote.quote.entity.QuoteApprovalAction;
import com.furuiduo.quote.quote.entity.QuoteApprovalLog;
import com.furuiduo.quote.quote.support.QuoteDateTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价审批日志")
public record QuoteApprovalLogResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "动作") String action,
    @Schema(description = "原状态") String fromStatus,
    @Schema(description = "新状态") String toStatus,
    @Schema(description = "意见") String comment,
    @Schema(description = "操作人ID") Long operatorId,
    @Schema(description = "操作人") String operatorName,
    @Schema(description = "操作时间") String createdAt) {

  public static QuoteApprovalLogResponse from(QuoteApprovalLog log) {
    return new QuoteApprovalLogResponse(
        log.getId(),
        log.getAction().name(),
        log.getFromStatus(),
        log.getToStatus(),
        log.getComment(),
        log.getOperatorId(),
        log.getOperatorName(),
        QuoteDateTimes.format(log.getCreatedAt()));
  }
}
