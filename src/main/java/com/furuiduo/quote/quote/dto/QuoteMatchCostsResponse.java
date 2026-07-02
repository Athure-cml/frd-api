package com.furuiduo.quote.quote.dto;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "成本库匹配响应")
public record QuoteMatchCostsResponse(
    @Schema(description = "是否至少匹配到一条成本") boolean matched,
    @Schema(description = "自动填充建议值") QuoteSheetFieldsDto suggestedFields,
    @Schema(description = "匹配明细") List<QuoteCostMatchItemDto> matches) {}
