package com.furuiduo.quote.sys.seed;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.PermissionType;
import com.furuiduo.quote.sys.entity.SysPermission;
import com.furuiduo.quote.sys.entity.SysRole;
import com.furuiduo.quote.sys.repository.SysPermissionRepository;
import com.furuiduo.quote.sys.repository.SysRoleRepository;

@Component
@Order(107)
public class ApprovalConfigPermissionSeeder implements ApplicationRunner {

  private record PermDef(String code, String name, PermissionType type, int sort) {}

  private static final List<PermDef> PERMISSIONS =
      List.of(
          new PermDef(
              PermissionCodes.APPROVAL_CONFIG_VIEW, "审批配置-查看", PermissionType.MENU, 92),
          new PermDef(
              PermissionCodes.APPROVAL_CONFIG_MANAGE, "审批配置-管理", PermissionType.API, 93));

  private static final Set<String> ROLES_WITH_ACCESS =
      Set.of("super_admin", "admin", "dept_manager", "finance");

  private final SysPermissionRepository permissionRepository;
  private final SysRoleRepository roleRepository;

  public ApprovalConfigPermissionSeeder(
      SysPermissionRepository permissionRepository, SysRoleRepository roleRepository) {
    this.permissionRepository = permissionRepository;
    this.roleRepository = roleRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    Map<String, SysPermission> permissions = ensurePermissions();
    for (SysRole role : roleRepository.findAll()) {
      if (grantPermissions(role, permissions)) {
        roleRepository.save(role);
      }
    }
  }

  private Map<String, SysPermission> ensurePermissions() {
    Map<String, SysPermission> map = new LinkedHashMap<>();
    for (PermDef def : PERMISSIONS) {
      SysPermission permission =
          permissionRepository
              .findByCode(def.code())
              .orElseGet(
                  () -> {
                    SysPermission created = new SysPermission();
                    created.setCode(def.code());
                    created.setName(def.name());
                    created.setType(def.type());
                    created.setSort(def.sort());
                    return permissionRepository.save(created);
                  });
      map.put(def.code(), permission);
    }
    return map;
  }

  private boolean grantPermissions(SysRole role, Map<String, SysPermission> permissions) {
    if (!ROLES_WITH_ACCESS.contains(role.getCode())) {
      return false;
    }
    var current = role.getPermissions();
    int before = current.size();
    SysPermission view = permissions.get(PermissionCodes.APPROVAL_CONFIG_VIEW);
    SysPermission manage = permissions.get(PermissionCodes.APPROVAL_CONFIG_MANAGE);
    if (view != null) {
      current.add(view);
    }
    if (manage != null) {
      current.add(manage);
    }
    return current.size() > before;
  }
}
