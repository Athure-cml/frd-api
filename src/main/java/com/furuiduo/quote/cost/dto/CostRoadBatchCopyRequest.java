package com.furuiduo.quote.cost.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "卡车成本批量复制")
public record CostRoadBatchCopyRequest(
    @Schema(description = "源记录 ID 列表；与 searchCriteria 二选一") List<Long> ids,
    @Schema(description = "按当前搜索条件全选时使用") Map<String, Object> searchCriteria,
    @Schema(description = "全选搜索结果时排除的 ID") List<Long> excludeIds,
    @Schema(description = "是否统一修改可覆盖字段；false 表示原样复制") Boolean applyOverrides,
    @Schema(description = "统一覆盖字段（费用/时间/备注等），仅 applyOverrides=true 时生效")
        Map<String, Object> fields,
    @Schema(description = "仅预览复制结果，不写入数据库") Boolean previewOnly) {}
