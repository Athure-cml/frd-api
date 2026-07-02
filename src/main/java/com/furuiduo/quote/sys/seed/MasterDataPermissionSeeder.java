package com.furuiduo.quote.sys.seed;

import java.util.HashSet;
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
@Order(950)
public class MasterDataPermissionSeeder implements ApplicationRunner {

  private record PermDef(String code, String name, int sort) {}

  private static final List<PermDef> PERMISSIONS =
      List.of(
          new PermDef(PermissionCodes.MD_US_STATE_VIEW, "美国州-查看", 53),
          new PermDef(PermissionCodes.MD_US_STATE_MANAGE, "美国州-管理", 54),
          new PermDef(PermissionCodes.MD_DEST_ADDRESS_VIEW, "美国州邮政编码-查看", 55),
          new PermDef(PermissionCodes.MD_DEST_ADDRESS_MANAGE, "美国州邮政编码-管理", 56),
          new PermDef(PermissionCodes.MD_GLOBAL_PORT_VIEW, "全球港口-查看", 57),
          new PermDef(PermissionCodes.MD_GLOBAL_PORT_MANAGE, "全球港口-管理", 58),
          new PermDef(PermissionCodes.MD_INLAND_POR_VIEW, "内陆POR-查看", 59),
          new PermDef(PermissionCodes.MD_INLAND_POR_MANAGE, "内陆POR-管理", 60));

  private static final Set<String> ROLES_WITH_VIEW =
      Set.of(
          "super_admin",
          "admin",
          "dept_manager",
          "sales",
          "doc_clerk",
          "overseas_operator",
          "booker",
          "finance",
          "viewer");

  private static final Set<String> ROLES_WITH_MANAGE =
      Set.of("super_admin", "admin", "finance");

  private final SysPermissionRepository permissionRepository;
  private final SysRoleRepository roleRepository;

  public MasterDataPermissionSeeder(
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
                    created.setType(PermissionType.API);
                    created.setSort(def.sort());
                    return created;
                  });
      permission.setName(def.name());
      permission.setSort(def.sort());
      permission = permissionRepository.save(permission);
      map.put(def.code(), permission);
    }
    return map;
  }

  private boolean grantPermissions(SysRole role, Map<String, SysPermission> permissions) {
    Set<SysPermission> grants = new HashSet<>();
    String code = role.getCode();

    if ("super_admin".equals(code)) {
      permissions.values().forEach(grants::add);
    } else if (ROLES_WITH_MANAGE.contains(code)) {
      permissions.values().forEach(grants::add);
    } else if (ROLES_WITH_VIEW.contains(code)) {
      addIfPresent(permissions, PermissionCodes.MD_US_STATE_VIEW, grants);
      addIfPresent(permissions, PermissionCodes.MD_DEST_ADDRESS_VIEW, grants);
      addIfPresent(permissions, PermissionCodes.MD_GLOBAL_PORT_VIEW, grants);
      addIfPresent(permissions, PermissionCodes.MD_INLAND_POR_VIEW, grants);
    }

    if (grants.isEmpty()) {
      return false;
    }
    Set<SysPermission> current = role.getPermissions();
    int before = current.size();
    current.addAll(grants);
    return current.size() > before;
  }

  private void addIfPresent(
      Map<String, SysPermission> permissions, String code, Set<SysPermission> grants) {
    SysPermission permission = permissions.get(code);
    if (permission != null) {
      grants.add(permission);
    }
  }
}
