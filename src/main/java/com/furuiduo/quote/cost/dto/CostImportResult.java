package com.furuiduo.quote.cost.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "导入结果（有错时 imported 恒为 0，不会部分落库；dryRun 成功时 imported 为可导入行数）")
public record CostImportResult(
    @Schema(description = "成功导入条数（校验失败时为 0；dryRun 时为可导入行数）") int imported,
    @Schema(description = "失败条数") int failed,
    @Schema(description = "错误信息") List<String> errors,
    @Schema(description = "失败数据行号（从 1 起，不含表头）") List<Integer> failedRowNumbers) {

  public CostImportResult(int imported, int failed, List<String> errors) {
    this(imported, failed, errors, List.of());
  }

  public CostImportResult {
    errors = errors == null ? List.of() : List.copyOf(errors);
    failedRowNumbers =
        failedRowNumbers == null ? List.of() : List.copyOf(failedRowNumbers);
  }
}
