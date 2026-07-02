package com.furuiduo.quote.masterdata.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "全球港口 UN/LOCODE 同步状态")
public record GlobalPortSyncStatus(
    @Schema(description = "状态：IDLE / RUNNING / COMPLETED / FAILED") String status,
    @Schema(description = "阶段：LOADING / IMPORTING") String phase,
    @Schema(description = "同步结果（完成时）") GlobalPortSyncResult result,
    @Schema(description = "错误信息（失败时）") String errorMessage,
    @Schema(description = "开始时间") LocalDateTime startedAt) {

  public static GlobalPortSyncStatus idle() {
    return new GlobalPortSyncStatus("IDLE", null, null, null, null);
  }

  public static GlobalPortSyncStatus running(String phase, LocalDateTime startedAt) {
    return new GlobalPortSyncStatus("RUNNING", phase, null, null, startedAt);
  }

  public static GlobalPortSyncStatus completed(GlobalPortSyncResult result, LocalDateTime startedAt) {
    return new GlobalPortSyncStatus("COMPLETED", null, result, null, startedAt);
  }

  public static GlobalPortSyncStatus failed(String errorMessage, LocalDateTime startedAt) {
    return new GlobalPortSyncStatus("FAILED", null, null, errorMessage, startedAt);
  }
}
