package com.furuiduo.quote.quote.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "成本入报价库结果")
public record QuoteLibraryPromoteResult(
    @Schema(description = "新入库条数") int promoted,
    @Schema(description = "已在报价库条数") int skipped,
    @Schema(description = "成本不存在条数") int notFound) {}
