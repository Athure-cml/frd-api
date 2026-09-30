package com.furuiduo.quote.quote.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价审批详情")
public record QuoteApprovalDetailResponse(
    @Schema(description = "报价单详情") QuoteDetailResponse quote,
    @Schema(description = "是否待审批") boolean pendingApproval,
    @Schema(description = "审批日志") List<QuoteApprovalLogResponse> logs,
    @Schema(description = "审批流程节点") List<QuoteApprovalWorkflowStepResponse> workflowSteps) {}
