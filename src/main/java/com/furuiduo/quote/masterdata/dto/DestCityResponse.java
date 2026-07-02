package com.furuiduo.quote.masterdata.dto;

import com.furuiduo.quote.masterdata.entity.MdDestCity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "目的城市")
public record DestCityResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "州 ID") Long stateId,
    @Schema(description = "城市名称") String name) {

  public static DestCityResponse from(MdDestCity entity) {
    return new DestCityResponse(entity.getId(), entity.getStateId(), entity.getName());
  }
}
