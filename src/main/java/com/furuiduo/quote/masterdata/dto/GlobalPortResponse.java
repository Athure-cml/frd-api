package com.furuiduo.quote.masterdata.dto;

import com.furuiduo.quote.masterdata.entity.MdGlobalPort;
import com.furuiduo.quote.masterdata.entity.PortType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "全球港口")
public record GlobalPortResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "港口代码") String code,
    @Schema(description = "英文名称") String nameEn,
    @Schema(description = "中文名称") String nameZh,
    @Schema(description = "航线") String route,
    @Schema(description = "国家/地区") String countryRegion,
    @Schema(description = "港口类型") PortType portType,
    @Schema(description = "数据版本") String dataVersion) {

  public static GlobalPortResponse from(MdGlobalPort entity) {
    return new GlobalPortResponse(
        entity.getId(),
        entity.getCode(),
        entity.getNameEn(),
        entity.getNameZh(),
        entity.getRoute(),
        entity.getCountryRegion(),
        entity.getPortType(),
        entity.getDataVersion());
  }
}
