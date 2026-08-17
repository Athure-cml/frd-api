package com.furuiduo.quote.supplier.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "其他供应商类型保存")
public record SupplierTypeSaveRequest(
    @Schema(description = "名称") String name,
    @Schema(description = "排序") Integer sortOrder,
    @Schema(description = "状态 1启用 0停用") Integer status) {}
