package com.furuiduo.quote.sys.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "系统公告")
public record AnnouncementResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "标题") String title,
    @Schema(description = "正文") String content,
    @Schema(description = "状态") String status,
    @Schema(description = "发布时间") String publishedAt,
    @Schema(description = "过期时间") String expiresAt,
    @Schema(description = "有效天数") Integer validDays,
    @Schema(description = "发布人") String createdByName,
    @Schema(description = "已读人数") Long readCount,
    @Schema(description = "未读人数") Long unreadCount,
    @Schema(description = "是否启用（兼容）") Boolean enabled) {}
