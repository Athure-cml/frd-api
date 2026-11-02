package com.furuiduo.quote.dashboard.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "工作台报价统计（近12个月：询价单数 / 已报价单数）")
public record WorkspaceQuoteStatsDto(
    @Schema(description = "月份，格式 YYYY-MM") List<String> months,
    @Schema(description = "询价单数量") List<Long> inquiries,
    @Schema(description = "已报价单数量") List<Long> quoted) {}
