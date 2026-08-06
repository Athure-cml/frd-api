package com.furuiduo.quote.supplier.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "供应商保存")
public record SupplierSaveRequest(
    @Schema(description = "供应商名称") String name,
    @Schema(
            description =
                "类型多选：BOOKING_AGENT/FLEET/CUSTOMS_BROKER/WAREHOUSE/DEDICATED_LINE/CONTAINER_LEASING/OTHER")
        List<String> types,
    @Schema(description = "邮箱") String email,
    @Schema(description = "备注") String remark,
    @Schema(description = "非熏蒸打包价公式") String nonFumigationPackageFormula,
    @Schema(description = "熏蒸打包价（非橡木）公式") String fumigationNonOakPackageFormula,
    @Schema(description = "熏蒸打包价（橡木）公式") String fumigationOakPackageFormula,
    @Schema(description = "状态 1启用 0停用") Integer status) {}
