package com.furuiduo.quote.cost.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "成本库批量修改结果")
public record CostBatchUpdateResult<T>(
    @Schema(description = "已更新条数") int updated,
    @Schema(description = "修改后记录预览（预览模式可能截断）") List<T> items,
    @Schema(description = "选中/处理总条数") int total) {}
