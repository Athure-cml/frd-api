package com.furuiduo.quote.cost.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

@Schema(description = "成本库批量取消常用")
public record CostHighlightIdsRequest(
    @NotEmpty @Schema(description = "成本行 ID 列表") List<Long> ids) {}
