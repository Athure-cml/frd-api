package com.furuiduo.quote.ai.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 对话请求")
public record AiChatRequest(
    @Schema(description = "多轮消息（不含 system，由服务端注入）") List<AiChatMessage> messages,
    @Schema(description = "是否启用业务工具（成本查询/报价上下文）", example = "true")
        Boolean enableTools) {}
