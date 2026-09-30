package com.furuiduo.quote.approval.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "审批配置保存")
public record ApprovalConfigSaveRequest(
    @Schema(description = "配置对象，目前仅 QUOTE") String configObject,
    @Schema(description = "审批流程节点") List<ApprovalConfigFlowStepRequest> flowSteps) {}
