package com.furuiduo.quote.supplier.dto;

import com.furuiduo.quote.quote.support.QuoteDateTimes;
import com.furuiduo.quote.supplier.entity.SupplierType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "其他供应商类型")
public record SupplierTypeResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "名称") String name,
    @Schema(description = "排序") Integer sortOrder,
    @Schema(description = "状态") Integer status,
    @Schema(description = "是否已被使用") boolean inUse,
    @Schema(description = "更新时间") String updatedAt) {

  public static SupplierTypeResponse from(SupplierType type, boolean inUse) {
    return new SupplierTypeResponse(
        type.getId(),
        type.getName(),
        type.getSortOrder(),
        type.getStatus(),
        inUse,
        QuoteDateTimes.format(type.getUpdatedAt()));
  }
}
