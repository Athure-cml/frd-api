package com.furuiduo.quote.cost.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "模板自定义业务字段")
public record CostTableCustomFieldDef(
    @Schema(description = "字段编码，以 cf_ 开头") String field,
    @Schema(description = "列标题") String title,
    @Schema(description = "是否必填") Boolean required,
    @Schema(description = "数据类型 text/number") String dataType) {}
