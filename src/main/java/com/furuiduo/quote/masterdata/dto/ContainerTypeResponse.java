package com.furuiduo.quote.masterdata.dto;

import com.furuiduo.quote.masterdata.entity.MdContainerType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "箱型")
public record ContainerTypeResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "箱型代码") String code,
    @Schema(description = "名称") String name,
    @Schema(description = "排序") Integer sort,
    @Schema(description = "状态：1启用 0停用") Integer status,
    @Schema(description = "备注") String remark) {

  public static ContainerTypeResponse from(MdContainerType entity) {
    return new ContainerTypeResponse(
        entity.getId(),
        entity.getCode(),
        entity.getName(),
        entity.getSort(),
        entity.getStatus(),
        entity.getRemark());
  }
}
