package com.furuiduo.quote.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "对话中引用的成本条目摘要")
public record AiCitedCost(
    @Schema(description = "成本类型：road / sea / fumigation") String type,
    @Schema(description = "成本 ID") Long id,
    @Schema(description = "展示标题") String title,
    @Schema(description = "摘要说明") String summary) {}
