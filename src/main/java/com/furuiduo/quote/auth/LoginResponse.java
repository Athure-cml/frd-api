package com.furuiduo.quote.auth;

import java.util.List;

import com.furuiduo.quote.sys.dto.DepartmentResponse;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录成功响应")
public record LoginResponse(
    @Schema(description = "访问令牌") String accessToken,
    @Schema(description = "用户 ID") Long id,
    @Schema(description = "工号") String username,
    @Schema(description = "显示名称") String realName,
    @Schema(description = "角色编码列表") List<String> roles,
    @Schema(description = "所属部门") DepartmentResponse dept,
    @Schema(description = "数据权限范围") String dataScope) {

  public static LoginResponse of(
      String accessToken, SysUser user, PermissionService permissionService) {
    return new LoginResponse(
        accessToken,
        user.getId(),
        user.getUsername(),
        user.getRealName(),
        permissionService.getRoleCodes(user),
        DepartmentResponse.from(user.getDepartment()),
        permissionService.getEffectiveDataScope(user).name());
  }
}
