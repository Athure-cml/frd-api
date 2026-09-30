package com.furuiduo.quote.quote.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价审批流程节点")
public record QuoteApprovalWorkflowStepResponse(
    @Schema(description = "节点标识") String key,
    @Schema(description = "节点标题") String title,
    @Schema(description = "状态 wait/process/finish/error") String status,
    @Schema(description = "操作人") String operatorName,
    @Schema(description = "操作时间") String operatedAt,
    @Schema(description = "意见") String comment) {}
