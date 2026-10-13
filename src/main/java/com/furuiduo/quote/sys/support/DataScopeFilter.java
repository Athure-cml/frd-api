package com.furuiduo.quote.sys.support;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.furuiduo.quote.sys.entity.DataScope;
import com.furuiduo.quote.sys.entity.SysRole;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

/** 报价等业务列表/详情的数据范围过滤参数。 */
public record DataScopeFilter(
    boolean scopeAll,
    boolean scopeDept,
    boolean scopeAssigned,
    boolean scopeSelf,
    Long deptId,
    Long userId,
    Collection<Long> assignedUserIds) {

  private static final List<Long> EMPTY_ASSIGNED = List.of(-1L);

  public static DataScopeFilter from(SysUser user, PermissionService permissionService) {
    DataScope scope = permissionService.getEffectiveDataScope(user);
    Long deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;
    Set<Long> assigned = permissionService.resolveAssignedDataUserIds(user);
    return new DataScopeFilter(
        scope == DataScope.ALL,
        scope == DataScope.DEPT,
        scope == DataScope.ASSIGNED,
        scope == DataScope.SELF,
        deptId,
        user.getId(),
        assigned.isEmpty() ? EMPTY_ASSIGNED : List.copyOf(assigned));
  }

  public static DataScopeFilter selfOnly(SysUser user) {
    Long deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;
    return new DataScopeFilter(
        false, false, false, true, deptId, user.getId(), EMPTY_ASSIGNED);
  }

  /** 从角色配置收集指定用户（不含登录人；登录人由 resolve 时并入）。 */
  public static Set<Long> collectConfiguredUserIds(SysRole role) {
    Set<Long> ids = new LinkedHashSet<>();
    if (role == null || role.getDataUsers() == null) {
      return ids;
    }
    for (SysUser dataUser : role.getDataUsers()) {
      if (dataUser != null && dataUser.getId() != null) {
        ids.add(dataUser.getId());
      }
    }
    return ids;
  }
}
