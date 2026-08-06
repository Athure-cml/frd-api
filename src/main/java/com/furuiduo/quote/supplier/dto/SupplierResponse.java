package com.furuiduo.quote.supplier.dto;

import java.util.List;

import com.furuiduo.quote.quote.support.QuoteDateTimes;
import com.furuiduo.quote.supplier.entity.Supplier;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "供应商")
public record SupplierResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "供应商编码") String code,
    @Schema(description = "供应商名称") String name,
    @Schema(description = "类型") List<String> types,
    @Schema(description = "邮箱") String email,
    @Schema(description = "备注") String remark,
    @Schema(description = "非熏蒸打包价公式") String nonFumigationPackageFormula,
    @Schema(description = "熏蒸打包价（非橡木）公式") String fumigationNonOakPackageFormula,
    @Schema(description = "熏蒸打包价（橡木）公式") String fumigationOakPackageFormula,
    @Schema(description = "状态") Integer status,
    @Schema(description = "创建人") String createdByName,
    @Schema(description = "创建时间") String createdAt,
    @Schema(description = "更新时间") String updatedAt) {

  public static SupplierResponse from(Supplier supplier) {
    return new SupplierResponse(
        supplier.getId(),
        supplier.getCode(),
        supplier.getName(),
        supplier.getTypes() == null ? List.of() : List.copyOf(supplier.getTypes()),
        supplier.getEmail(),
        supplier.getRemark(),
        supplier.getNonFumigationPackageFormula(),
        supplier.getFumigationNonOakPackageFormula(),
        supplier.getFumigationOakPackageFormula(),
        supplier.getStatus(),
        supplier.getCreatedByName(),
        QuoteDateTimes.format(supplier.getCreatedAt()),
        QuoteDateTimes.format(supplier.getUpdatedAt()));
  }
}
