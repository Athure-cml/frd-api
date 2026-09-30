package com.furuiduo.quote.dashboard.support;

import com.furuiduo.quote.sys.entity.DataScope;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

public record DashboardScopeParams(
    boolean scopeAll, boolean scopeDept, boolean scopeSelf, Long deptId, Long userId) {

  public static DashboardScopeParams from(SysUser user, PermissionService permissionService) {
    DataScope scope = permissionService.getEffectiveDataScope(user);
    Long deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;
    return new DashboardScopeParams(
        scope == DataScope.ALL,
        scope == DataScope.DEPT,
        scope == DataScope.SELF,
        deptId,
        user.getId());
  }

  /** 仅当前用户作为创建人的报价单（待办/提醒推送） */
  public static DashboardScopeParams self(SysUser user) {
    Long deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;
    return new DashboardScopeParams(false, false, true, deptId, user.getId());
  }
}
