package com.furuiduo.quote.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 文档解析请求（纯文本）")
public record AiParseRequest(
    @Schema(description = "邮件或粘贴文本") String text,
    @Schema(description = "可选提示，例如「提取报价关键字段」") String hint) {}
