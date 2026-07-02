package com.furuiduo.quote.cost.dto;

import java.math.BigDecimal;
import java.util.Map;

import com.furuiduo.quote.cost.entity.CostStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "海运/铁路成本保存")
public record FreightCostSaveRequest(
    @Schema(description = "起运地/发站") String origin,
    @Schema(description = "目的地/到站") String destination,
    @Schema(description = "承运商") String carrier,
    @Schema(description = "箱型") String spec,
    @Schema(description = "单位") String unit,
    @Schema(description = "单价") BigDecimal unitPrice,
    @Schema(description = "BUC 附加费") BigDecimal buc,
    @Schema(description = "附加费有效期") String surchargeValidDate,
    @Schema(description = "ALL IN") BigDecimal allIn,
    @Schema(description = "有效期") String validDate,
    @Schema(description = "币种") String currency,
    @Schema(description = "有效期起") String validFrom,
    @Schema(description = "有效期止") String validTo,
    @Schema(description = "状态") CostStatus status,
    @Schema(description = "备注") String remark,
    @Schema(description = "自定义字段值") Map<String, Object> extraFields) {}
