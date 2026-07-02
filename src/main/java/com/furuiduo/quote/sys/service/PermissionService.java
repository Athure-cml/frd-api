package com.furuiduo.quote.sys.service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;

import com.furuiduo.quote.sys.entity.DataScope;
import com.furuiduo.quote.sys.entity.SysPermission;
import com.furuiduo.quote.sys.entity.SysRole;
import com.furuiduo.quote.sys.entity.SysUser;

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

  public boolean hasPermission(SysUser user, String permissionCode) {
    if (getRoleCodes(user).contains("super_admin")) {
      return true;
    }
    return getPermissionCodes(user).contains(permissionCode);
  }
}
