package com.furuiduo.quote.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "工作台 KPI 指标")
public record WorkspaceMetricDto(
    @Schema(description = "指标键：monthQuotes/monthAmount/winRate/followUp/expiringSoon")
        String key,
    @Schema(description = "当前值") long value,
    @Schema(description = "较昨日变化") long trend) {}
