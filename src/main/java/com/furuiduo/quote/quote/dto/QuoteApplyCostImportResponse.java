package com.furuiduo.quote.quote.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "引入成本规则重算结果")
public record QuoteApplyCostImportResponse(
    @Schema(description = "应写入报价单的业务字段（仅含本次引入相关字段）") QuoteSheetFieldsDto fields) {}
