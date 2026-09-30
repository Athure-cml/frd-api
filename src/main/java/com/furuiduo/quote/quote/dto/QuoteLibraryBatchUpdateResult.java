package com.furuiduo.quote.quote.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价库批量更新结果")
public record QuoteLibraryBatchUpdateResult(@Schema(description = "更新条数") int updated) {}
