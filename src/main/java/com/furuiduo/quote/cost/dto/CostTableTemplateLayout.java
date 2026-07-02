package com.furuiduo.quote.cost.dto;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "表格模板布局")
public record CostTableTemplateLayout(
    @Schema(description = "分组列（卡车）") List<CostTableTemplateGroup> groups,
    @Schema(description = "扁平列（海运/铁路）") List<String> fields,
    @Schema(description = "字段级覆盖") Map<String, CostTableFieldOverride> fieldOverrides,
    @Schema(description = "列顺序（优先于 groups/fields）") List<String> fieldOrder,
    @Schema(description = "自定义业务字段定义") List<CostTableCustomFieldDef> customFields) {}
