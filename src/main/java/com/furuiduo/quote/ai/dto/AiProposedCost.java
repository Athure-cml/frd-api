package com.furuiduo.quote.ai.dto;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 拟新增的成本草稿（未入库，需用户确认）")
public record AiProposedCost(
    @Schema(description = "成本类型：road / sea / fumigation") String type,
    @Schema(description = "展示标题") String title,
    @Schema(description = "摘要说明") String summary,
    @Schema(description = "可提交的保存载荷（与对应 SaveRequest 字段对齐）")
        Map<String, Object> payload,
    @Schema(description = "缺失或需核对的提示") List<String> warnings) {}
