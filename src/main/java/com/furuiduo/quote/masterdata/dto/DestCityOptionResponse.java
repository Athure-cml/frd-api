package com.furuiduo.quote.masterdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "城市下拉选项（含州代码）")
public record DestCityOptionResponse(
    @Schema(description = "城市名称") String city,
    @Schema(description = "州代码") String stateCode) {}
