package com.furuiduo.quote.approval.dto;

import java.util.List;

import com.furuiduo.quote.approval.entity.ApprovalConfig;
import com.furuiduo.quote.approval.entity.ApprovalConfigStep;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "审批配置")
public record ApprovalConfigResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "配置编号") String configNo,
    @Schema(description = "配置对象") String configObject,
    @Schema(description = "配置流程") List<FlowStep> flowSteps) {

  public static ApprovalConfigResponse from(ApprovalConfig config) {
    List<FlowStep> steps =
        config.getSteps().stream()
            .map(step -> new FlowStep(step.getApproverId(), step.getApproverName()))
            .toList();
    return new ApprovalConfigResponse(
        config.getId(), config.getConfigNo(), config.getConfigObject(), steps);
  }

  @Schema(description = "审批节点")
  public record FlowStep(
      @Schema(description = "审批人ID") Long approverId,
      @Schema(description = "审批人姓名") String approverName) {

    public static FlowStep from(ApprovalConfigStep step) {
      return new FlowStep(step.getApproverId(), step.getApproverName());
    }
  }
}
