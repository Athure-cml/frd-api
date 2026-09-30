package com.furuiduo.quote.sys.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "活动滚动条条目")
public record AnnouncementTickerResponse(
    @Schema(description = "公告 ID") Long id,
    @Schema(description = "滚动展示文案") String text,
    @Schema(description = "标题") String title,
    @Schema(description = "正文") String content) {}
