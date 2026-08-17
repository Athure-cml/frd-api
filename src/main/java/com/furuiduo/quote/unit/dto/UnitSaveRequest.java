package com.furuiduo.quote.unit.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "单位保存")
public record UnitSaveRequest(
    @Schema(description = "编码（展示用，如 hours）", example = "hours") String code,
    @Schema(description = "名称") String name,
    @Schema(description = "备注") String remark,
    @Schema(description = "排序") Integer sort,
    @Schema(description = "状态 1启用 0停用") Integer status) {}
