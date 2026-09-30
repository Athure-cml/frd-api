package com.furuiduo.quote.quote.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.support.QuoteCostRiskSupport;
import com.furuiduo.quote.quote.support.QuoteDateTimes;
import com.furuiduo.quote.quote.support.QuoteServiceTypesSupport;
import com.furuiduo.quote.quote.support.QuoteStatusSupport;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价单详情")
public record QuoteDetailResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "报价单号") String quoteNo,
    @Schema(description = "服务类型（多选）") List<String> serviceTypes,
    @Schema(description = "OAK / NON-OAK") String oakType,
    @Schema(description = "客户ID") Long customerId,
    @Schema(description = "客户名称") String customerName,
    @Schema(description = "运输方式") String transportMode,
    @Schema(description = "路线摘要") String routeSummary,
    @Schema(description = "状态") String status,
    @Schema(description = "总金额") BigDecimal totalAmount,
    @Schema(description = "币种") String currency,
    @Schema(description = "基准币种") String baseCurrency,
    @Schema(description = "汇率快照") BigDecimal exchangeRate,
    @Schema(description = "有效期至") LocalDate validUntil,
    @Schema(description = "备注") String remark,
    @Schema(description = "跟进人ID") Long followUpBy,
    @Schema(description = "跟进人") String followUpByName,
    @Schema(description = "是否已过期") boolean expired,
    @Schema(description = "是否已作废") boolean voided,
    @Schema(description = "是否可编辑") boolean editable,
    @Schema(description = "当前用户是否可操作（创建人或超级管理员）") boolean operable,
    @Schema(description = "是否存在底层成本变更风险") boolean costRiskActive,
    @Schema(description = "成本风险说明") String costRiskReason,
    @Schema(description = "成本风险涉及的报价库：road/sea/fumigation") List<String> costRiskModes,
    @Schema(description = "成本风险标记时间") String costRiskAt,
    @Schema(description = "业务表字段") QuoteSheetFieldsDto sheet,
    @Schema(description = "创建人ID") Long createdBy,
    @Schema(description = "创建人") String createdByName,
    @Schema(description = "部门ID") Long deptId,
    @Schema(description = "提交时间") String submittedAt,
    @Schema(description = "创建时间") String createdAt,
    @Schema(description = "更新时间") String updatedAt,
    @Schema(description = "变更来源报价单 ID") Long parentQuoteId,
    @Schema(description = "版本族根单 ID") Long rootQuoteId,
    @Schema(description = "修订号，0=原版") Integer revisionNo,
    @Schema(description = "修订标签，如 R1") String revisionLabel,
    @Schema(description = "变更原因") String changeReason,
    @Schema(description = "是否当前生效版本") boolean currentVersion,
    @Schema(description = "被哪张变更单替代") Long supersededByQuoteId,
    @Schema(description = "明细行") List<QuoteLineResponse> lines,
    @Schema(description = "成本匹配快照") List<QuoteCostMatchItemDto> costSnapshots,
    @Schema(description = "跟进记录") List<QuoteFollowUpResponse> followUps) {

  public static QuoteDetailResponse from(QuoteOrder order) {
    return from(order, List.of(), List.of(), false);
  }

  public static QuoteDetailResponse from(
      QuoteOrder order,
      List<QuoteCostMatchItemDto> costSnapshots,
      List<QuoteFollowUpResponse> followUps,
      boolean operable) {
    return from(
        order,
        costSnapshots,
        followUps,
        operable,
        QuoteCostRiskSupport.parseModes(order.getCostRiskReason()));
  }

  public static QuoteDetailResponse from(
      QuoteOrder order,
      List<QuoteCostMatchItemDto> costSnapshots,
      List<QuoteFollowUpResponse> followUps,
      boolean operable,
      List<String> costRiskModes) {
    String status = QuoteStatusSupport.displayStatus(order.getStatus());
    boolean editable =
        QuoteStatusSupport.isEditable(order.getStatus()) && order.getDeletedAt() == null;
    Integer revisionNo = order.getRevisionNo() == null ? 0 : order.getRevisionNo();
    return new QuoteDetailResponse(
        order.getId(),
        order.getQuoteNo(),
        QuoteServiceTypesSupport.copyOf(order.getServiceTypes()),
        order.getOakType() == null ? null : order.getOakType().name(),
        order.getCustomerId(),
        order.getCustomerName(),
        order.getTransportMode().name(),
        order.getRouteSummary(),
        status,
        order.getTotalAmount(),
        order.getCurrency(),
        order.getBaseCurrency(),
        order.getExchangeRate(),
        order.getValidUntil(),
        order.getRemark(),
        order.getFollowUpBy(),
        order.getFollowUpByName(),
        QuoteStatusSupport.isExpired(order),
        QuoteStatusSupport.isVoided(order),
        editable,
        operable,
        Boolean.TRUE.equals(order.getCostRiskActive()),
        order.getCostRiskReason(),
        costRiskModes != null ? costRiskModes : List.of(),
        QuoteDateTimes.format(order.getCostRiskAt()),
        QuoteSheetFieldsDto.from(order),
        order.getCreatedBy(),
        order.getCreatedByName(),
        order.getDeptId(),
        QuoteDateTimes.format(order.getSubmittedAt()),
        QuoteDateTimes.format(order.getCreatedAt()),
        QuoteDateTimes.format(order.getUpdatedAt()),
        order.getParentQuoteId(),
        QuoteStatusSupport.resolveRootQuoteId(order),
        revisionNo,
        QuoteStatusSupport.revisionLabel(revisionNo),
        order.getChangeReason(),
        order.getCurrentVersion() == null || Boolean.TRUE.equals(order.getCurrentVersion()),
        order.getSupersededByQuoteId(),
        order.getLines().stream().map(QuoteLineResponse::from).toList(),
        costSnapshots,
        followUps);
  }
}
