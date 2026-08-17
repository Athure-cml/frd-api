package com.furuiduo.quote.cost.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "卡车成本续期：新建一版运价，并将源行有效期写为新生效期 − 1 天")
public record RoadCostRenewRequest(
    @Schema(description = "源记录 ID（被续期的旧价）", requiredMode = Schema.RequiredMode.REQUIRED)
        Long sourceId,
    @Schema(description = "新价数据", requiredMode = Schema.RequiredMode.REQUIRED)
        RoadCostSaveRequest record) {}
