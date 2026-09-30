package com.furuiduo.quote.quote.dto;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价库批量费用更新")
public record QuoteLibraryBatchUpdateRequest(
    @Schema(description = "成本记录 ID 列表") List<Long> ids,
    @Schema(description = "要更新的费用字段") Map<String, Object> fields) {}
