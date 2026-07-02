package com.furuiduo.quote.masterdata.dto;

import com.furuiduo.quote.masterdata.entity.MdUsState;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "美国州")
public record UsStateResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "州代码") String code,
    @Schema(description = "中文名称") String nameZh) {

  public static UsStateResponse from(MdUsState entity) {
    return new UsStateResponse(entity.getId(), entity.getCode(), entity.getNameZh());
  }
}
