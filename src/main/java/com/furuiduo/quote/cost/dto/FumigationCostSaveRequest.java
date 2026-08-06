package com.furuiduo.quote.cost.dto;

import java.math.BigDecimal;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "熏蒸成本保存")
public record FumigationCostSaveRequest(
    @Schema(description = "REGION") String region,
    @Schema(description = "STATION") String station,
    @Schema(description = "FM-OUTDOOR NON OAK") BigDecimal outdoorNonOak,
    @Schema(description = "FM-OUTDOOR OAK") BigDecimal outdoorOak,
    @Schema(description = "FM-OUTDOOR VALIDITY") String outdoorValidity,
    @Schema(description = "FM-INDOOR NON OAK") BigDecimal indoorNonOak,
    @Schema(description = "FM-INDOOR OAK") BigDecimal indoorOak,
    @Schema(description = "FM-INDOOR VALIDITY") String indoorValidity,
    @Schema(description = "ADDRESS") String address,
    @Schema(description = "状态（由有效期自动判定 active/expired，可忽略）") String status,
    @Schema(description = "扩展字段") Map<String, Object> extraFields) {}
