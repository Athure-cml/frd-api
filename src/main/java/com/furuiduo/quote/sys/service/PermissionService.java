package com.furuiduo.quote.sys.service;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;

import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.DataScope;
import com.furuiduo.quote.sys.entity.SysPermission;
import com.furuiduo.quote.sys.entity.SysRole;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.support.DataScopeFilter;

@Service
public class PermissionService {

  public List<String> getRoleCodes(SysUser user) {
    return user.getRoles().stream().map(SysRole::getCode).sorted().toList();
  }

  public List<String> getRoleNames(SysUser user) {
    return user.getRoles().stream()
        .sorted(Comparator.comparing(SysRole::getCode))
        .map(SysRole::getName)
        .toList();
  }

  public List<String> getPermissionCodes(SysUser user) {
    Set<String> codes = new TreeSet<>();
    for (SysRole role : user.getRoles()) {
      for (SysPermission permission : role.getPermissions()) {
        codes.add(permission.getCode());
      }
    }
    return List.copyOf(codes);
  }

  public DataScope getEffectiveDataScope(SysUser user) {
    return user.getRoles().stream()
        .map(SysRole::getDataScope)
        .max(Comparator.comparingInt(DataScope::priority))
        .orElse(DataScope.SELF);
  }

  /**
   * 数据权限为 ASSIGNED 时的可见创建人 ID：角色勾选用户 ∪ 当前登录人（本人始终包含）。
   * 非 ASSIGNED 时返回空集。
   */
  public Set<Long> resolveAssignedDataUserIds(SysUser user) {
    if (getEffectiveDataScope(user) != DataScope.ASSIGNED) {
      return Set.of();
    }
    Set<Long> ids = new LinkedHashSet<>();
    for (SysRole role : user.getRoles()) {
      if (role.getDataScope() != DataScope.ASSIGNED) {
        continue;
      }
      ids.addAll(DataScopeFilter.collectConfiguredUserIds(role));
    }
    if (user.getId() != null) {
      ids.add(user.getId());
    }
    return ids;
  }

  public boolean hasPermission(SysUser user, String permissionCode) {
    if (getRoleCodes(user).contains("super_admin")) {
      return true;
    }
    return getPermissionCodes(user).contains(permissionCode);
  }

  public boolean hasAnyPermission(SysUser user, String... permissionCodes) {
    if (permissionCodes == null || permissionCodes.length == 0) {
      return false;
    }
    if (getRoleCodes(user).contains("super_admin")) {
      return true;
    }
    var owned = getPermissionCodes(user);
    for (String code : permissionCodes) {
      if (code != null && owned.contains(code)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 报价业务页（报价单 / 报价库 / 成本录入）的支撑下拉、联想数据读取权限。
   * 产品原则：能进业务页，就应能读该页依赖的主数据选项；写操作仍走各自模块权限。
   */
  public boolean canUseQuoteLookups(SysUser user) {
    return hasAnyPermission(
        user,
        PermissionCodes.QUOTE_VIEW,
        PermissionCodes.QUOTE_CREATE,
        PermissionCodes.QUOTE_EDIT,
        PermissionCodes.QUOTE_LIBRARY_ROAD_VIEW,
        PermissionCodes.QUOTE_LIBRARY_SEA_VIEW,
        PermissionCodes.QUOTE_LIBRARY_FUMIGATION_VIEW,
        PermissionCodes.COST_ROAD_VIEW,
        PermissionCodes.COST_SEA_VIEW,
        PermissionCodes.COST_FUMIGATION_VIEW);
  }

  /** 报价库列表读取本库视图模板：有对应报价库查看即可。 */
  public boolean canViewQuoteLibrary(SysUser user, String mode) {
    if (mode == null) {
      return false;
    }
    return switch (mode.trim().toLowerCase()) {
      case "road" -> hasPermission(user, PermissionCodes.QUOTE_LIBRARY_ROAD_VIEW);
      case "sea" -> hasPermission(user, PermissionCodes.QUOTE_LIBRARY_SEA_VIEW);
      case "fumigation" -> hasPermission(user, PermissionCodes.QUOTE_LIBRARY_FUMIGATION_VIEW);
      default -> false;
    };
  }
}
