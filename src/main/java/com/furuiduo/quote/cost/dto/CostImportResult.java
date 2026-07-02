package com.furuiduo.quote.cost.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "导入结果")
public record CostImportResult(
    @Schema(description = "成功导入条数") int imported,
    @Schema(description = "失败条数") int failed,
    @Schema(description = "错误信息") List<String> errors) {}
