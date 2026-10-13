package com.furuiduo.quote.sys.dto;

import java.util.Comparator;
import java.util.List;

import com.furuiduo.quote.sys.entity.SysPermission;
import com.furuiduo.quote.sys.entity.SysRole;
import com.furuiduo.quote.sys.support.DataScopeFilter;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "角色列表项")
public record RoleListItem(
    @Schema(description = "角色 ID") Long id,
    @Schema(description = "角色编码") String code,
    @Schema(description = "角色名称") String name,
    @Schema(description = "数据权限范围") String dataScope,
    @Schema(description = "状态：1 启用，0 停用") Integer status,
    @Schema(description = "备注") String remark,
    @Schema(description = "权限码列表") List<String> permissions,
    @Schema(description = "指定数据可见用户 ID") List<Long> assignedUserIds) {

  public static RoleListItem from(SysRole role) {
    List<String> permissionCodes =
        role.getPermissions().stream()
            .sorted(Comparator.comparingInt(SysPermission::getSort))
            .map(SysPermission::getCode)
            .toList();
    List<Long> assignedUserIds =
        DataScopeFilter.collectConfiguredUserIds(role).stream().sorted().toList();
    return new RoleListItem(
        role.getId(),
        role.getCode(),
        role.getName(),
        role.getDataScope().name(),
        role.getStatus(),
        role.getRemark(),
        permissionCodes,
        assignedUserIds);
  }
}
