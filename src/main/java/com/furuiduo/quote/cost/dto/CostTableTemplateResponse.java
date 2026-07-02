package com.furuiduo.quote.cost.dto;

import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.furuiduo.quote.cost.entity.CostGridTemplate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "成本库表格视图模板")
public record CostTableTemplateResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "模板编码") String code,
    @Schema(description = "模板名称或 i18n key") String name,
    @Schema(description = "适用模式 road/sea/rail") String mode,
    @Schema(description = "是否默认模板") @JsonProperty("isDefault") boolean isDefault,
    @Schema(description = "列布局") CostTableTemplateLayout layout,
    @Schema(description = "创建时间") String createdAt) {

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public static CostTableTemplateResponse from(CostGridTemplate entity) {
    return new CostTableTemplateResponse(
        entity.getId(),
        entity.getCode(),
        entity.getName(),
        entity.getMode(),
        entity.isDefaultTemplate(),
        entity.getLayout(),
        entity.getCreatedAt() == null ? null : FORMATTER.format(entity.getCreatedAt()));
  }
}
