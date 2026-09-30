package com.furuiduo.quote.quote.dto;

import com.furuiduo.quote.quote.entity.QuoteApprovalLog;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.support.QuoteApprovalSupport;
import com.furuiduo.quote.quote.support.QuoteDateTimes;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "审批单列表项")
public record QuoteApprovalListItem(
    @Schema(description = "报价单ID") Long id,
    @Schema(description = "列表行键，一轮审批一条") String rowKey,
    @Schema(description = "审批轮次，从1开始") Integer cycleIndex,
    @Schema(description = "审批单号") String approvalNo,
    @Schema(description = "审批类型") String approvalType,
    @Schema(description = "关联单据号") String relatedDocNo,
    @Schema(description = "当前节点") String currentNode,
    @Schema(description = "发起人") String initiatorName,
    @Schema(description = "发起部门") String initiatorDept,
    @Schema(description = "发起时间") String initiatedAt,
    @Schema(description = "完成时间") String completedAt,
    @Schema(description = "审批用时") String duration,
    @Schema(description = "状态 PENDING/APPROVED/REJECTED/WITHDRAWN") String status) {

  public static QuoteApprovalListItem from(QuoteOrder order, List<QuoteApprovalLog> logs) {
    return from(order, logs, List.of());
  }

  public static QuoteApprovalListItem from(
      QuoteOrder order,
      List<QuoteApprovalLog> logs,
      List<QuoteApprovalSupport.FlowNode> flow) {
    return from(order, logs, flow, null, null);
  }

  public static QuoteApprovalListItem from(
      QuoteOrder order,
      List<QuoteApprovalLog> logs,
      List<QuoteApprovalSupport.FlowNode> flow,
      Long viewerId) {
    return from(order, logs, flow, viewerId, null);
  }

  public static QuoteApprovalListItem from(
      QuoteOrder order,
      List<QuoteApprovalLog> logs,
      List<QuoteApprovalSupport.FlowNode> flow,
      Long viewerId,
      String initiatorDept) {
    List<QuoteApprovalLog> cycle = QuoteApprovalSupport.currentCycle(logs);
    int cycleIndex = Math.max(QuoteApprovalSupport.splitCycles(logs).size(), 1);
    return fromCycle(order, cycle, cycleIndex, true, flow, initiatorDept);
  }

  public static QuoteApprovalListItem fromCycle(
      QuoteOrder order,
      List<QuoteApprovalLog> cycle,
      int cycleIndex,
      boolean latest,
      List<QuoteApprovalSupport.FlowNode> flow,
      String initiatorDept) {
    QuoteApprovalSupport.ResolvedApproval resolved =
        QuoteApprovalSupport.resolveCycle(order, cycle, flow, latest);
    return new QuoteApprovalListItem(
        order.getId(),
        order.getId() + "-" + cycleIndex,
        cycleIndex,
        QuoteApprovalSupport.buildApprovalNo(order, cycleIndex),
        QuoteApprovalSupport.resolveApprovalType(order),
        order.getQuoteNo(),
        resolved.currentNode(),
        QuoteApprovalSupport.resolveInitiatorName(order, cycle),
        initiatorDept == null ? "" : initiatorDept,
        QuoteDateTimes.format(QuoteApprovalSupport.cycleSubmitAt(cycle)),
        QuoteDateTimes.format(QuoteApprovalSupport.cycleCompletedAt(cycle, order, latest)),
        QuoteApprovalSupport.formatDuration(
            QuoteApprovalSupport.cycleSubmitAt(cycle),
            QuoteApprovalSupport.cycleCompletedAt(cycle, order, latest)),
        resolved.status());
  }
}
