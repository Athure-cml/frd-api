package com.furuiduo.quote.masterdata.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "全球港口 UN/LOCODE 同步结果")
public record GlobalPortSyncResult(
    @Schema(description = "数据版本") String dataVersion,
    @Schema(description = "解析记录数") int totalParsed,
    @Schema(description = "新增条数") int inserted,
    @Schema(description = "更新条数") int updated,
    @Schema(description = "跳过条数") int skipped,
    @Schema(description = "同步时间") LocalDateTime syncedAt) {}
