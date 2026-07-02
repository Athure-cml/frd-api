package com.furuiduo.quote.quote.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.support.QuoteDateTimes;
import com.furuiduo.quote.quote.support.QuoteStatusSupport;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价单详情")
public record QuoteDetailResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "报价单号") String quoteNo,
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
    @Schema(description = "业务表字段") QuoteSheetFieldsDto sheet,
    @Schema(description = "创建人ID") Long createdBy,
    @Schema(description = "创建人") String createdByName,
    @Schema(description = "部门ID") Long deptId,
    @Schema(description = "提交时间") String submittedAt,
    @Schema(description = "创建时间") String createdAt,
    @Schema(description = "更新时间") String updatedAt,
    @Schema(description = "明细行") List<QuoteLineResponse> lines,
    @Schema(description = "成本匹配快照") List<QuoteCostMatchItemDto> costSnapshots,
    @Schema(description = "跟进记录") List<QuoteFollowUpResponse> followUps) {

  public static QuoteDetailResponse from(QuoteOrder order) {
    return from(order, List.of(), List.of());
  }

  public static QuoteDetailResponse from(
      QuoteOrder order,
      List<QuoteCostMatchItemDto> costSnapshots,
      List<QuoteFollowUpResponse> followUps) {
    String status = QuoteStatusSupport.displayStatus(order.getStatus());
    boolean editable =
        order.getStatus() == com.furuiduo.quote.quote.entity.QuoteStatus.DRAFT
            || order.getStatus() == com.furuiduo.quote.quote.entity.QuoteStatus.EFFECTIVE
            || order.getStatus() == com.furuiduo.quote.quote.entity.QuoteStatus.FOLLOWING
            || order.getStatus() == com.furuiduo.quote.quote.entity.QuoteStatus.SENT
            || order.getStatus() == com.furuiduo.quote.quote.entity.QuoteStatus.PENDING;
    return new QuoteDetailResponse(
        order.getId(),
        order.getQuoteNo(),
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
        QuoteSheetFieldsDto.from(order),
        order.getCreatedBy(),
        order.getCreatedByName(),
        order.getDeptId(),
        QuoteDateTimes.format(order.getSubmittedAt()),
        QuoteDateTimes.format(order.getCreatedAt()),
        QuoteDateTimes.format(order.getUpdatedAt()),
        order.getLines().stream().map(QuoteLineResponse::from).toList(),
        costSnapshots,
        followUps);
  }
}
