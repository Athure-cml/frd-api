package com.furuiduo.quote.cost.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "海运成本批量复制结果")
public record CostSeaBatchCopyResult(
    @Schema(description = "将创建/已创建条数") int created,
    @Schema(description = "复制后的记录预览") List<FreightCostResponse> items) {}
