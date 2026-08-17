package com.furuiduo.quote.common;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "拖拽排序请求")
public record ReorderRequest(
    @Schema(description = "按新顺序排列的 ID 列表（通常为当前页可视行）") List<Long> ids) {}
