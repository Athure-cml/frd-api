package com.furuiduo.quote.sys.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "部门保存请求")
public record DepartmentSaveRequest(
    @NotBlank @Size(max = 32) @Schema(description = "部门编码") String code,
    @NotBlank @Size(max = 64) @Schema(description = "部门名称") String name,
    @Schema(description = "上级部门 ID，0 表示根") Long parentId,
    @NotNull @Schema(description = "排序") Integer sort,
    @NotNull @Schema(description = "状态：1 启用，0 停用") Integer status) {}
