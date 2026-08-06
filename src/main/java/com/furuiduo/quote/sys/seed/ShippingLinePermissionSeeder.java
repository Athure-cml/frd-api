package com.furuiduo.quote.sys.seed;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
@Order(103)
public class ShippingLinePermissionSeeder implements ApplicationRunner {

  private record PermDef(String code, String name, int sort) {}

  private static final List<PermDef> PERMISSIONS =
      List.of(
          new PermDef(PermissionCodes.SHIPPING_LINE_VIEW, "船公司-查看", 67),
          new PermDef(PermissionCodes.SHIPPING_LINE_CREATE, "船公司-新建", 68),
          new PermDef(PermissionCodes.SHIPPING_LINE_EDIT, "船公司-编辑", 69),
          new PermDef(PermissionCodes.SHIPPING_LINE_DELETE, "船公司-删除", 70));

  private static final Set<String> ROLES_WITH_ALL =
      Set.of("super_admin", "admin", "dept_manager", "sales");

  private static final Set<String> ROLES_WITH_VIEW_ONLY =
      Set.of("doc_clerk", "overseas_operator", "booker", "approver", "finance");

  private final SysPermissionRepository permissionRepository;
  private final SysRoleRepository roleRepository;

  public ShippingLinePermissionSeeder(
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
      permission.setName(def.name());
      permission.setSort(def.sort());
      permission = permissionRepository.save(permission);
      map.put(def.code(), permission);
    }
    return map;
  }

  private boolean grantPermissions(SysRole role, Map<String, SysPermission> permissions) {
    Set<SysPermission> grants = shouldGrantForRole(role, permissions);
    if (grants.isEmpty()) {
      return false;
    }
    Set<SysPermission> current = role.getPermissions();
    int before = current.size();
    current.addAll(grants);
    return current.size() > before;
  }

  private Set<SysPermission> shouldGrantForRole(
      SysRole role, Map<String, SysPermission> permissions) {
    Set<SysPermission> grants = new HashSet<>();
    String code = role.getCode();

    if ("super_admin".equals(code) || ROLES_WITH_ALL.contains(code)) {
      permissions.values().forEach(grants::add);
      return grants;
    }

    if (ROLES_WITH_VIEW_ONLY.contains(code)) {
      addIfPresent(permissions, PermissionCodes.SHIPPING_LINE_VIEW, grants);
      return grants;
    }

    Set<String> roleCodes =
        role.getPermissions().stream().map(SysPermission::getCode).collect(Collectors.toSet());
    if (roleCodes.contains(PermissionCodes.QUOTE_CREATE)
        || roleCodes.contains(PermissionCodes.QUOTE_EDIT)
        || roleCodes.contains(PermissionCodes.COST_SEA_EDIT)) {
      permissions.values().forEach(grants::add);
    } else if (roleCodes.contains(PermissionCodes.QUOTE_VIEW)
        || roleCodes.contains(PermissionCodes.COST_SEA_VIEW)) {
      addIfPresent(permissions, PermissionCodes.SHIPPING_LINE_VIEW, grants);
    }

    return grants;
  }

  private void addIfPresent(
      Map<String, SysPermission> permissions, String code, Set<SysPermission> grants) {
    SysPermission permission = permissions.get(code);
    if (permission != null) {
      grants.add(permission);
    }
  }
}
