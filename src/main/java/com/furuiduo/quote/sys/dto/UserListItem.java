package com.furuiduo.quote.sys.dto;

import java.util.List;

import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户列表项")
public record UserListItem(
    @Schema(description = "用户 ID") Long id,
    @Schema(description = "登录账号") String username,
    @Schema(description = "姓名") String realName,
    @Schema(description = "状态：1 启用，0 停用") Integer status,
    @Schema(description = "所属部门") DepartmentResponse dept,
    @Schema(description = "角色编码") List<String> roles,
    @Schema(description = "角色名称") List<String> roleNames,
    @Schema(description = "数据权限范围") String dataScope) {

  public static UserListItem from(SysUser user, PermissionService permissionService) {
    List<String> roleCodes = permissionService.getRoleCodes(user);
    List<String> roleNames = permissionService.getRoleNames(user);
    return new UserListItem(
        user.getId(),
        user.getUsername(),
        user.getRealName(),
        user.getStatus(),
        DepartmentResponse.from(user.getDepartment()),
        roleCodes,
        roleNames,
        permissionService.getEffectiveDataScope(user).name());
  }
}
