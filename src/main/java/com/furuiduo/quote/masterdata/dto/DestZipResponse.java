package com.furuiduo.quote.masterdata.dto;

import com.furuiduo.quote.masterdata.entity.MdDestZip;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "目的邮编")
public record DestZipResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "城市 ID") Long cityId,
    @Schema(description = "邮编") String zipCode) {

  public static DestZipResponse from(MdDestZip entity) {
    return new DestZipResponse(entity.getId(), entity.getCityId(), entity.getZipCode());
  }
}
