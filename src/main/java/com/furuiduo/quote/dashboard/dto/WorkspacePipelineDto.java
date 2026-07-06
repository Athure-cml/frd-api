package com.furuiduo.quote.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "工作台报价进度")
public record WorkspacePipelineDto(
    @Schema(description = "报价单 ID") Long id,
    @Schema(description = "报价单号") String quoteNo,
    @Schema(description = "线路/标题") String title,
    @Schema(description = "进度 0-100") int progress,
    @Schema(description = "状态：progress/done") String status) {}
