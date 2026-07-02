package com.furuiduo.quote.masterdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "内陆 POR 保存请求")
public record InlandPorSaveRequest(
    @Schema(description = "POR 名称") String name,
    @Schema(description = "POL ID") Long polId,
    @Schema(description = "区域") String region) {}
