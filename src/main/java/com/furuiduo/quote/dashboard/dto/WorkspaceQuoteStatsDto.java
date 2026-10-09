package com.furuiduo.quote.dashboard.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "工作台报价统计（近12个月，金额单位：万）")
public record WorkspaceQuoteStatsDto(
    @Schema(description = "月份，格式 YYYY-MM") List<String> months,
    @Schema(description = "报价金额（万）") List<Double> quoted,
    @Schema(description = "成交金额（万）") List<Double> won) {}
