package com.furuiduo.quote.quote.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价库确认结果")
public record QuoteLibraryConfirmResult(@Schema(description = "确认条数") int confirmed) {}
