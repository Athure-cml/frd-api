package com.furuiduo.quote.dashboard.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "顶栏通知项")
public record NotificationItemDto(
    @Schema(description = "通知 ID") String id,
    @Schema(description = "类型：QUOTE_EXPIRING/COST_UPDATED") String type,
    @Schema(description = "标题") String title,
    @Schema(description = "描述") String message,
    @Schema(description = "时间标签") String date,
    @Schema(description = "是否已读") boolean isRead,
    @Schema(description = "跳转链接") String link,
    @Schema(description = "扩展数据") Map<String, Object> payload) {}
