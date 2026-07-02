package com.furuiduo.quote.quote.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "跟进记录保存")
public record QuoteFollowUpSaveRequest(
    @Schema(description = "跟进状态") String followStatus,
    @Schema(description = "跟进内容") String content) {}
