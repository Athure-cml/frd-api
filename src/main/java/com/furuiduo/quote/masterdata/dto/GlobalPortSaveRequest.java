package com.furuiduo.quote.masterdata.dto;

import com.furuiduo.quote.masterdata.entity.PortType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "全球港口保存请求")
public record GlobalPortSaveRequest(
    @Schema(description = "港口代码") String code,
    @Schema(description = "英文名称") String nameEn,
    @Schema(description = "中文名称") String nameZh,
    @Schema(description = "航线") String route,
    @Schema(description = "国家/地区") String countryRegion,
    @Schema(description = "港口类型") PortType portType) {}
