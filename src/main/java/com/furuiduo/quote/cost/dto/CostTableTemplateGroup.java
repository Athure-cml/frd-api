package com.furuiduo.quote.cost.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "表格模板分组")
public record CostTableTemplateGroup(
    @Schema(description = "分组键") String key,
    @Schema(description = "分组标题 i18n key") String labelKey,
    @Schema(description = "表头样式类名") String headerClassName,
    @Schema(description = "字段列表") List<String> fields) {}
