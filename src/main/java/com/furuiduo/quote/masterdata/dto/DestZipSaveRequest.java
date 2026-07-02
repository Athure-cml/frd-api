package com.furuiduo.quote.masterdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "目的邮编保存请求")
public record DestZipSaveRequest(
    @Schema(description = "城市 ID") Long cityId, @Schema(description = "邮编") String zipCode) {}
