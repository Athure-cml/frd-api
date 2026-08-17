package com.furuiduo.quote.unit.dto;

import com.furuiduo.quote.unit.entity.Unit;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "单位")
public record UnitResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "编码") String code,
    @Schema(description = "名称") String name,
    @Schema(description = "备注") String remark,
    @Schema(description = "排序") Integer sort,
    @Schema(description = "状态") Integer status) {

  public static UnitResponse from(Unit unit) {
    return new UnitResponse(
        unit.getId(),
        unit.getCode(),
        unit.getName(),
        unit.getRemark(),
        unit.getSort(),
        unit.getStatus());
  }
}
