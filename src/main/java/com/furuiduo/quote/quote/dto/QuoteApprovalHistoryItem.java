package com.furuiduo.quote.quote.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价审批历史记录")
public record QuoteApprovalHistoryItem(
    @Schema(description = "ID") Long id,
    @Schema(description = "操作节点") String nodeTitle,
    @Schema(description = "操作人") String operatorName,
    @Schema(description = "操作时间") String operatedAt,
    @Schema(description = "结果") String result,
    @Schema(description = "意见/原因") String comment,
    @Schema(description = "动作") String action) {}
