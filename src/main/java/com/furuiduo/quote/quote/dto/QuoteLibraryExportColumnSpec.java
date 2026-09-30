package com.furuiduo.quote.quote.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价库导出列")
public record QuoteLibraryExportColumnSpec(
    @Schema(description = "字段编码") String field,
    @Schema(description = "表头标题") String title) {}
