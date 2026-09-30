package com.furuiduo.quote.quote.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价库编辑上下文")
public record QuoteLibraryEditContext(
    @Schema(description = "成本记录 ID") Long id,
    @Schema(description = "当前展示费用") Map<String, BigDecimal> values,
    @Schema(description = "成本+规则默认费用（无人工覆盖）") Map<String, BigDecimal> defaultValues,
    @Schema(description = "成本原价下限") Map<String, BigDecimal> costFloors,
    @Schema(description = "可编辑字段") List<String> editableFields) {}
