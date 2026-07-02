package com.furuiduo.quote.quote.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.support.QuoteDateTimes;
import com.furuiduo.quote.quote.support.QuoteStatusSupport;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价单列表项")
public record QuoteListItem(
    @Schema(description = "ID") Long id,
    @Schema(description = "报价单号") String quoteNo,
    @Schema(description = "客户ID") Long customerId,
    @Schema(description = "客户名称") String customerName,
    @Schema(description = "运输方式") String transportMode,
    @Schema(description = "路线摘要") String routeSummary,
    @Schema(description = "状态") String status,
    @Schema(description = "总金额") BigDecimal totalAmount,
    @Schema(description = "币种") String currency,
    @Schema(description = "有效期至") LocalDate validUntil,
    @Schema(description = "跟进人") String followUpByName,
    @Schema(description = "是否已过期") boolean expired,
    @Schema(description = "是否已作废") boolean voided,
    @Schema(description = "业务表字段") QuoteSheetFieldsDto sheet,
    @Schema(description = "创建人") String createdByName,
    @Schema(description = "创建时间") String createdAt,
    @Schema(description = "更新时间") String updatedAt) {

  public static QuoteListItem from(QuoteOrder order) {
    return new QuoteListItem(
        order.getId(),
        order.getQuoteNo(),
        order.getCustomerId(),
        order.getCustomerName(),
        order.getTransportMode().name(),
        order.getRouteSummary(),
        QuoteStatusSupport.displayStatus(order.getStatus()),
        order.getTotalAmount(),
        order.getCurrency(),
        order.getValidUntil(),
        order.getFollowUpByName(),
        QuoteStatusSupport.isExpired(order),
        QuoteStatusSupport.isVoided(order),
        QuoteSheetFieldsDto.from(order),
        order.getCreatedByName(),
        QuoteDateTimes.format(order.getCreatedAt()),
        QuoteDateTimes.format(order.getUpdatedAt()));
  }
}
