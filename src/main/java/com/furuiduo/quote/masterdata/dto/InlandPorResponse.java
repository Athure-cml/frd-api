package com.furuiduo.quote.masterdata.dto;

import com.furuiduo.quote.masterdata.entity.MdGlobalPort;
import com.furuiduo.quote.masterdata.entity.MdInlandPor;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "内陆 POR")
public record InlandPorResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "POR 名称") String name,
    @Schema(description = "POL ID") Long polId,
    @Schema(description = "POL 代码") String polCode,
    @Schema(description = "POL 英文名称") String polNameEn,
    @Schema(description = "区域") String region) {

  public static InlandPorResponse from(MdInlandPor entity, MdGlobalPort pol) {
    return new InlandPorResponse(
        entity.getId(),
        entity.getName(),
        entity.getPolId(),
        pol != null ? pol.getCode() : null,
        pol != null ? pol.getNameEn() : null,
        entity.getRegion());
  }
}
