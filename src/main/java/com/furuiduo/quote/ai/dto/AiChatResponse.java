package com.furuiduo.quote.ai.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 对话响应")
public record AiChatResponse(
    @Schema(description = "助手回复") String reply,
    @Schema(description = "本轮调用的工具名") List<String> toolCalls,
    @Schema(description = "引用的成本条目") List<AiCitedCost> citedCosts,
    @Schema(description = "拟新增成本草稿（未入库）") List<AiProposedCost> proposedCosts,
    @Schema(description = "请求前端打开的页面") List<AiOpenPage> openPages,
    @Schema(description = "使用的模型") String model) {}
