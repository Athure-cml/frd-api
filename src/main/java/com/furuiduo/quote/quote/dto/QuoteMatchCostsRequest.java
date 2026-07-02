package com.furuiduo.quote.quote.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "成本库匹配请求（7 字段联合匹配，可部分填写）")
public record QuoteMatchCostsRequest(
    @Schema(description = "Zip code") String zipCode,
    @Schema(description = "City") String city,
    @Schema(description = "State") String state,
    @Schema(description = "POR") String por,
    @Schema(description = "POL") String pol,
    @Schema(description = "POD") String pod,
    @Schema(description = "SSL") String ssl,
    @Schema(description = "成本库类型 ROAD|SEA|FUMIGATION，空则匹配全部") String costType) {}
