package com.furuiduo.quote.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "审批配置流程节点")
public record ApprovalConfigFlowStepRequest(
    @Schema(description = "审批人用户ID") Long approverId) {}
