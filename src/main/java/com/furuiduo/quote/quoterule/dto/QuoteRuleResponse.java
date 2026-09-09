package com.furuiduo.quote.quoterule.dto;

import java.math.BigDecimal;

import com.furuiduo.quote.quoterule.entity.MdQuoteRule;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价单规则")
public record QuoteRuleResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "规则名称") String name,
    @Schema(description = "目标费用字段") String targetField,
    @Schema(description = "条件类型") String conditionType,
    @Schema(description = "条件金额（分档阈值）") BigDecimal conditionAmount,
    @Schema(description = "计算类型") String calcType,
    @Schema(description = "加价金额") BigDecimal addAmount,
    @Schema(description = "固定金额") BigDecimal fixedAmount,
    @Schema(description = "CIF 系数") BigDecimal cifFactor,
    @Schema(description = "CIF 费率") BigDecimal cifRate,
    @Schema(description = "排序") Integer sortOrder,
    @Schema(description = "状态 1启用 0停用") Integer status,
    @Schema(description = "备注") String remark) {

  public static QuoteRuleResponse from(MdQuoteRule rule) {
    return new QuoteRuleResponse(
        rule.getId(),
        rule.getName(),
        rule.getTargetField(),
        rule.getConditionType(),
        rule.getConditionAmount(),
        rule.getCalcType(),
        rule.getAddAmount(),
        rule.getFixedAmount(),
        rule.getCifFactor(),
        rule.getCifRate(),
        rule.getSortOrder(),
        rule.getStatus(),
        rule.getRemark());
  }
}
