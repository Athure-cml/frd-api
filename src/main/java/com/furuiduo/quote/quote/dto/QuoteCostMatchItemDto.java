package com.furuiduo.quote.quote.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "单条成本匹配结果")
public record QuoteCostMatchItemDto(
    @Schema(description = "成本类型 ROAD|SEA|FUMIGATION") String costType,
    @Schema(description = "成本记录 ID") Long costRefId,
    @Schema(description = "版本编码") String costVersion,
    @Schema(description = "匹配键") Map<String, Object> matchKeys,
    @Schema(description = "快照") Map<String, Object> snapshot) {}
