package com.furuiduo.quote.masterdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "箱型保存请求")
public record ContainerTypeSaveRequest(
    @Schema(description = "箱型代码", example = "40HQ") String code,
    @Schema(description = "名称", example = "40' High Cube") String name,
    @Schema(description = "排序") Integer sort,
    @Schema(description = "状态：1启用 0停用") Integer status,
    @Schema(description = "备注") String remark) {}
