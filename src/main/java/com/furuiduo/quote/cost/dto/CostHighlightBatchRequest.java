package com.furuiduo.quote.cost.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@Schema(description = "成本库批量标记常用")
public record CostHighlightBatchRequest(
    @NotEmpty @Schema(description = "成本行 ID 列表") List<Long> ids,
    @NotBlank @Schema(description = "行背景色 #RRGGBB") String color,
    @Size(max = 128) @Schema(description = "备注") String remark) {}
