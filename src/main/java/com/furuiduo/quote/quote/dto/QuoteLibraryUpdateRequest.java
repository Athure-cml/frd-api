package com.furuiduo.quote.quote.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价库费用更新")
public record QuoteLibraryUpdateRequest(
    @Schema(description = "要更新的费用字段") Map<String, Object> fields) {}
