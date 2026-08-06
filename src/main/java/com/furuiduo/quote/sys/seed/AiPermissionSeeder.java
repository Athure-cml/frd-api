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
@Order(104)
public class AiPermissionSeeder implements ApplicationRunner {

  private record PermDef(String code, String name, int sort) {}

  private static final List<PermDef> PERMISSIONS =
      List.of(new PermDef(PermissionCodes.AI_USE, "AI助手-使用", 70));

  /**
   * 默认可使用 AI 的角色（最小必要集；其余角色请在「角色管理」中按需勾选 ai:use）。
   * 注意：本 seeder 只新增不回收，已误授的角色需在后台手动取消。
   */
  private static final Set<String> ROLES_WITH_AI =
      Set.of("super_admin", "admin", "dept_manager", "sales");

  private final SysPermissionRepository permissionRepository;
  private final SysRoleRepository roleRepository;

  public AiPermissionSeeder(
      SysPermissionRepository permissionRepository, SysRoleRepository roleRepository) {
    this.permissionRepository = permissionRepository;
    this.roleRepository = roleRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (permissionRepository.count() == 0) {
      return;
    }

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
                    created.setType(PermissionType.API);
                    created.setSort(def.sort());
                    return permissionRepository.save(created);
                  });
      map.put(def.code(), permission);
    }
    return map;
  }

  private boolean grantPermissions(SysRole role, Map<String, SysPermission> permissions) {
    if (!ROLES_WITH_AI.contains(role.getCode())) {
      return false;
    }
    SysPermission permission = permissions.get(PermissionCodes.AI_USE);
    if (permission == null) {
      return false;
    }
    Set<SysPermission> current = role.getPermissions();
    int before = current.size();
    current.add(permission);
    return current.size() > before;
  }
}
