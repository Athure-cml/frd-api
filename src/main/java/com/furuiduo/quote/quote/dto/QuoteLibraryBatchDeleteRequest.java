package com.furuiduo.quote.quote.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价库批量删除")
public record QuoteLibraryBatchDeleteRequest(
    @Schema(description = "成本记录 ID 列表") List<Long> ids) {}
