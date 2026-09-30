package com.furuiduo.quote.quote.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.support.QuoteDateTimes;
import com.furuiduo.quote.quote.support.QuoteServiceTypesSupport;
import com.furuiduo.quote.quote.support.QuoteSheetAllInSupport;
import com.furuiduo.quote.quote.support.QuoteStatusSupport;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价单列表项")
public record QuoteListItem(
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
    @Schema(description = "ALL IN（业务表费用合计）") BigDecimal allIn,
    @Schema(description = "报价日期") String quoteDate,
    @Schema(description = "币种") String currency,
    @Schema(description = "有效期至") LocalDate validUntil,
    @Schema(description = "跟进人") String followUpByName,
    @Schema(description = "是否已过期") boolean expired,
    @Schema(description = "是否已作废") boolean voided,
    @Schema(description = "业务表字段") QuoteSheetFieldsDto sheet,
    @Schema(description = "创建人ID") Long createdBy,
    @Schema(description = "创建人") String createdByName,
    @Schema(description = "当前用户是否可操作（创建人或超级管理员）") boolean operable,
    @Schema(description = "创建时间") String createdAt,
    @Schema(description = "更新时间") String updatedAt,
    @Schema(description = "是否存在底层成本变更风险") boolean costRiskActive,
    @Schema(description = "成本风险说明") String costRiskReason,
    @Schema(description = "成本风险标记时间") String costRiskAt,
    @Schema(description = "修订号，0=原版") Integer revisionNo,
    @Schema(description = "修订标签，如 R1") String revisionLabel,
    @Schema(description = "是否当前生效版本") boolean currentVersion,
    @Schema(description = "报价库展示行（成本库结构 + 规则处理后费用，libraryMode 时填充）")
        Map<String, Object> libraryRow) {

  public static QuoteListItem from(QuoteOrder order, boolean operable) {
    return from(order, operable, null);
  }

  public static QuoteListItem from(
      QuoteOrder order, boolean operable, Map<String, Object> libraryRow) {
    Integer revisionNo = order.getRevisionNo() == null ? 0 : order.getRevisionNo();
    return new QuoteListItem(
        order.getId(),
        order.getQuoteNo(),
        QuoteServiceTypesSupport.copyOf(order.getServiceTypes()),
        order.getOakType() == null ? null : order.getOakType().name(),
        order.getCustomerId(),
        order.getCustomerName(),
        order.getTransportMode().name(),
        order.getRouteSummary(),
        QuoteStatusSupport.displayStatus(order.getStatus()),
        order.getTotalAmount(),
        QuoteSheetAllInSupport.computeAllIn(order),
        QuoteDateTimes.formatDate(order.getCreatedAt()),
        order.getCurrency(),
        order.getValidUntil(),
        order.getFollowUpByName(),
        QuoteStatusSupport.isExpired(order),
        QuoteStatusSupport.isVoided(order),
        QuoteSheetFieldsDto.from(order),
        order.getCreatedBy(),
        order.getCreatedByName(),
        operable,
        QuoteDateTimes.format(order.getCreatedAt()),
        QuoteDateTimes.format(order.getUpdatedAt()),
        Boolean.TRUE.equals(order.getCostRiskActive()),
        order.getCostRiskReason(),
        QuoteDateTimes.format(order.getCostRiskAt()),
        revisionNo,
        QuoteStatusSupport.revisionLabel(revisionNo),
        order.getCurrentVersion() == null || Boolean.TRUE.equals(order.getCurrentVersion()),
        libraryRow);
  }

  public QuoteListItem withLibraryRow(Map<String, Object> libraryRow) {
    return new QuoteListItem(
        id,
        quoteNo,
        serviceTypes,
        oakType,
        customerId,
        customerName,
        transportMode,
        routeSummary,
        status,
        totalAmount,
        allIn,
        quoteDate,
        currency,
        validUntil,
        followUpByName,
        expired,
        voided,
        sheet,
        createdBy,
        createdByName,
        operable,
        createdAt,
        updatedAt,
        costRiskActive,
        costRiskReason,
        costRiskAt,
        revisionNo,
        revisionLabel,
        currentVersion,
        libraryRow);
  }
}
