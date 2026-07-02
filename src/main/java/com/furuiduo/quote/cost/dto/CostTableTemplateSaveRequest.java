package com.furuiduo.quote.cost.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "保存表格视图模板")
public record CostTableTemplateSaveRequest(
    @Schema(description = "适用模式 road/sea/rail", requiredMode = Schema.RequiredMode.REQUIRED)
        String mode,
    @Schema(description = "模板编码（新建时由系统生成，可省略）") String code,
    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED) String name,
    @Schema(description = "是否默认模板") Boolean isDefault,
    @Schema(description = "列布局", requiredMode = Schema.RequiredMode.REQUIRED)
        CostTableTemplateLayout layout) {}
