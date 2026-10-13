package com.furuiduo.quote.dashboard.support;

import java.util.Collection;
import java.util.List;

import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;
import com.furuiduo.quote.sys.support.DataScopeFilter;

public record DashboardScopeParams(
    boolean scopeAll,
    boolean scopeDept,
    boolean scopeAssigned,
    boolean scopeSelf,
    Long deptId,
    Long userId,
    Collection<Long> assignedUserIds) {

  private static final List<Long> EMPTY_ASSIGNED = List.of(-1L);

  public static DashboardScopeParams from(SysUser user, PermissionService permissionService) {
    DataScopeFilter filter = DataScopeFilter.from(user, permissionService);
    return new DashboardScopeParams(
        filter.scopeAll(),
        filter.scopeDept(),
        filter.scopeAssigned(),
        filter.scopeSelf(),
        filter.deptId(),
        filter.userId(),
        filter.assignedUserIds());
  }

  /** 仅当前用户作为创建人的报价单（待办/提醒推送） */
  public static DashboardScopeParams self(SysUser user) {
    Long deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;
    return new DashboardScopeParams(
        false, false, false, true, deptId, user.getId(), EMPTY_ASSIGNED);
  }
}
