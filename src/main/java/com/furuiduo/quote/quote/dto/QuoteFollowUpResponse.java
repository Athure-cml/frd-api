package com.furuiduo.quote.quote.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "跟进记录")
public record QuoteFollowUpResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "跟进状态") String followStatus,
    @Schema(description = "跟进内容") String content,
    @Schema(description = "跟进人 ID") Long followUpBy,
    @Schema(description = "跟进人") String followUpByName,
    @Schema(description = "跟进时间") String followUpAt,
    @Schema(description = "更新时间") String updatedAt) {}
