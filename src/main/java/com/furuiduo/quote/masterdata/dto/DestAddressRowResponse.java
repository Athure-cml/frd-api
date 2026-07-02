package com.furuiduo.quote.masterdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "美国州邮政编码行")
public record DestAddressRowResponse(
    @Schema(description = "邮编 ID") Long id,
    @Schema(description = "州代码") String stateCode,
    @Schema(description = "州 ID") Long stateId,
    @Schema(description = "城市") String city,
    @Schema(description = "城市 ID") Long cityId,
    @Schema(description = "邮编") String zipCode) {}
