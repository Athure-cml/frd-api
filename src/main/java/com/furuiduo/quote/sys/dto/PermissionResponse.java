package com.furuiduo.quote.sys.dto;

import com.furuiduo.quote.sys.entity.SysPermission;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "权限项")
public record PermissionResponse(
    @Schema(description = "权限 ID") Long id,
    @Schema(description = "权限码") String code,
    @Schema(description = "权限名称") String name,
    @Schema(description = "类型") String type,
    @Schema(description = "排序") Integer sort) {

  public static PermissionResponse from(SysPermission permission) {
    return new PermissionResponse(
        permission.getId(),
        permission.getCode(),
        permission.getName(),
        permission.getType().name(),
        permission.getSort());
  }
}
