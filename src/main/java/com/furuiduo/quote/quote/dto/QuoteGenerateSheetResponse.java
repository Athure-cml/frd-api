package com.furuiduo.quote.quote.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价单生成结果")
public record QuoteGenerateSheetResponse(
    @Schema(description = "报价单日期 yyyy-MM-dd") String quoteDate,
    @Schema(description = "生成的业务表字段") QuoteSheetFieldsDto sheet,
    @Schema(description = "匹配到的成本快照") List<QuoteCostMatchItemDto> costMatches) {}
