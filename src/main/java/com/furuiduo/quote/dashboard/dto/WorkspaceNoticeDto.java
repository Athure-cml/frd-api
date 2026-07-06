package com.furuiduo.quote.dashboard.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "工作台系统通知")
public record WorkspaceNoticeDto(
    @Schema(description = "通知 ID") String id,
    @Schema(description = "类型：QUOTE_EXPIRING/COST_UPDATED") String type,
    @Schema(description = "时间标签") String time,
    @Schema(description = "扩展数据") Map<String, Object> payload) {}
