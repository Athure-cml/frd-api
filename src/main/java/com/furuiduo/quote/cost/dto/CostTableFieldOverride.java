package com.furuiduo.quote.cost.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "表格列覆盖配置")
public record CostTableFieldOverride(
    @Schema(description = "是否可见") Boolean visible,
    @Schema(description = "列宽") Integer width,
    @Schema(description = "最小列宽") Integer minWidth,
    @Schema(description = "固定列 left/right") String fixed,
    @Schema(description = "自定义列标题") String title,
    @Schema(description = "是否必填") Boolean required,
    @Schema(description = "对齐 left/center/right") String align) {}
