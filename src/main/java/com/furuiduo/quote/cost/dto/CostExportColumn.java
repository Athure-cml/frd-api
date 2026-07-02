package com.furuiduo.quote.cost.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "成本导出列")
public record CostExportColumn(
    @Schema(description = "字段编码") String field,
    @Schema(description = "表头标题") String header) {}
