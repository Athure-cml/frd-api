package com.furuiduo.quote.ai.dto;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 拟新建报价草稿（未入库，需用户在创建页确认）")
public record AiProposedQuote(
    @Schema(description = "展示标题") String title,
    @Schema(description = "摘要说明") String summary,
    @Schema(description = "新建报价预填载荷（serviceTypes/customer/sheet/costMatches 等）")
        Map<String, Object> payload,
    @Schema(description = "缺失或需核对的提示") List<String> warnings,
    @Schema(description = "是否已匹配到报价库成本") boolean matched) {}
