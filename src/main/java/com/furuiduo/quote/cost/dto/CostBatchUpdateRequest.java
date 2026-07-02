package com.furuiduo.quote.cost.dto;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "批量更新")
public record CostBatchUpdateRequest(
    @Schema(description = "ID 列表") List<Long> ids,
    @Schema(description = "要更新的字段") Map<String, Object> fields) {}
