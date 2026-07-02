package com.furuiduo.quote.masterdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "目的城市保存请求")
public record DestCitySaveRequest(
    @Schema(description = "州 ID") Long stateId, @Schema(description = "城市名称") String name) {}
