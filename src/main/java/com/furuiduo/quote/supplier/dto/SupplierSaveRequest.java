package com.furuiduo.quote.supplier.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "供应商保存")
public record SupplierSaveRequest(
    @Schema(description = "分类 TRUCK/FUMIGATION/YARD/OTHER") String category,
    @Schema(description = "供应商名称") String name,
    @Schema(description = "简称") String shortName,
    @Schema(description = "类型多选（仅其他供应商，传类型ID）") List<String> types,
    @Schema(description = "联系人") String contactName,
    @Schema(description = "电话") String phone,
    @Schema(description = "邮箱") String email,
    @Schema(description = "备注") String remark,
    @Schema(description = "非熏蒸打包价公式（仅卡车）") String nonFumigationPackageFormula,
    @Schema(description = "熏蒸打包价（非橡木）公式（仅卡车）")
        String fumigationNonOakPackageFormula,
    @Schema(description = "熏蒸打包价（橡木）公式（仅卡车）") String fumigationOakPackageFormula,
    @Schema(description = "状态 1启用 0停用") Integer status) {}
