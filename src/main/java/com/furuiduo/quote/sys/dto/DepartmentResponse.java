package com.furuiduo.quote.sys.dto;

import com.furuiduo.quote.sys.entity.SysDepartment;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "部门信息")
public record DepartmentResponse(
    @Schema(description = "部门 ID") Long id,
    @Schema(description = "部门编码", example = "CS") String code,
    @Schema(description = "部门名称", example = "客服部") String name,
    @Schema(description = "上级部门 ID，0 表示根") Long parentId,
    @Schema(description = "排序") Integer sort,
    @Schema(description = "状态：1 启用，0 停用") Integer status) {

  public static DepartmentResponse from(SysDepartment department) {
    return new DepartmentResponse(
        department.getId(),
        department.getCode(),
        department.getName(),
        department.getParentId(),
        department.getSort(),
        department.getStatus());
  }
}
