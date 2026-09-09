package com.furuiduo.quote.cost.support;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.furuiduo.quote.sys.entity.DataScope;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

@Service
public class CostHighlightAccessService {

  private static final Set<String> GLOBAL_DEPT_CODES = Set.of("GMO", "TECH");

  private final PermissionService permissionService;

  public CostHighlightAccessService(PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  /** 总经办、技术部、管理员等：可见并操作全部部门的标记 */
  public boolean isGlobalViewer(SysUser user) {
    List<String> roles = permissionService.getRoleCodes(user);
    if (roles.contains("super_admin") || roles.contains("admin")) {
      return true;
    }
    if (permissionService.getEffectiveDataScope(user) == DataScope.ALL) {
      return true;
    }
    if (user.getDepartment() == null) {
      return false;
    }
    return GLOBAL_DEPT_CODES.contains(user.getDepartment().getCode());
  }

  /** 超级管理员、系统管理员角色（标记对业务员可见） */
  public boolean isAdminRole(SysUser user) {
    List<String> roles = permissionService.getRoleCodes(user);
    return roles.contains("super_admin") || roles.contains("admin");
  }

  public Long requireDeptId(SysUser user) {
    if (user.getDepartment() == null) {
      throw new IllegalStateException("用户未分配部门，无法标记常用");
    }
    return user.getDepartment().getId();
  }
}
