package com.furuiduo.quote.cost.dto;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "批量删除")
public record CostBatchDeleteRequest(
    @Schema(description = "ID 列表；与 searchCriteria 二选一") List<Long> ids,
    @Schema(description = "按当前搜索条件全选时使用") Map<String, Object> searchCriteria,
    @Schema(description = "全选搜索结果时排除的 ID") List<Long> excludeIds) {}
