package com.furuiduo.quote.ai.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 文档解析预览（未写入业务库）")
public record AiParseResponse(
    @Schema(description = "抽取的结构化字段预览") Map<String, Object> fields,
    @Schema(description = "模型原文说明") String explanation,
    @Schema(description = "用于解析的原文摘录（截断）") String sourceExcerpt) {}
