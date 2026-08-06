package com.furuiduo.quote.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 对话消息")
public record AiChatMessage(
    @Schema(description = "角色：user / assistant / system", example = "user") String role,
    @Schema(description = "内容") String content) {}
