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
@Order(105)
public class QuoteLibraryPermissionSeeder implements ApplicationRunner {

  private record PermDef(String code, String name, PermissionType type, int sort) {}

  private static final List<PermDef> PERMISSIONS =
      List.of(
          new PermDef(
              PermissionCodes.QUOTE_LIBRARY_ROAD_VIEW, "卡车报价库-查看", PermissionType.MENU, 57),
          new PermDef(
              PermissionCodes.QUOTE_LIBRARY_ROAD_EDIT, "卡车报价库-编辑", PermissionType.API, 58),
          new PermDef(
              PermissionCodes.QUOTE_LIBRARY_ROAD_DELETE, "卡车报价库-删除", PermissionType.API, 59),
          new PermDef(
              PermissionCodes.QUOTE_LIBRARY_SEA_VIEW, "海运报价库-查看", PermissionType.MENU, 60),
          new PermDef(
              PermissionCodes.QUOTE_LIBRARY_SEA_EDIT, "海运报价库-编辑", PermissionType.API, 61),
          new PermDef(
              PermissionCodes.QUOTE_LIBRARY_SEA_DELETE, "海运报价库-删除", PermissionType.API, 62),
          new PermDef(
              PermissionCodes.QUOTE_LIBRARY_FUMIGATION_VIEW,
              "熏蒸报价库-查看",
              PermissionType.MENU,
              63),
          new PermDef(
              PermissionCodes.QUOTE_LIBRARY_FUMIGATION_EDIT,
              "熏蒸报价库-编辑",
              PermissionType.API,
              64),
          new PermDef(
              PermissionCodes.QUOTE_LIBRARY_FUMIGATION_DELETE,
              "熏蒸报价库-删除",
              PermissionType.API,
              65));

  private static final Set<String> ROLES_WITH_ALL =
      Set.of("super_admin", "admin", "dept_manager", "sales");

  private static final Set<String> ROLES_WITH_VIEW_ONLY =
      Set.of("doc_clerk", "overseas_operator", "booker", "approver", "finance");

  private final SysPermissionRepository permissionRepository;
  private final SysRoleRepository roleRepository;

  public QuoteLibraryPermissionSeeder(
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
      permission.setName(def.name());
      permission.setType(def.type());
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
      grants.add(permissions.get(PermissionCodes.QUOTE_LIBRARY_ROAD_VIEW));
      grants.add(permissions.get(PermissionCodes.QUOTE_LIBRARY_SEA_VIEW));
      grants.add(permissions.get(PermissionCodes.QUOTE_LIBRARY_FUMIGATION_VIEW));
      return grants.stream().filter(p -> p != null).collect(Collectors.toSet());
    }

    Set<String> roleCodes =
        role.getPermissions().stream().map(SysPermission::getCode).collect(Collectors.toSet());

    if (roleCodes.contains(PermissionCodes.QUOTE_LIBRARY_ROAD_VIEW)
        || roleCodes.contains(PermissionCodes.QUOTE_VIEW)
        || roleCodes.contains(PermissionCodes.QUOTE_CREATE)
        || roleCodes.contains(PermissionCodes.QUOTE_EDIT)) {
      grants.add(permissions.get(PermissionCodes.QUOTE_LIBRARY_ROAD_VIEW));
      grants.add(permissions.get(PermissionCodes.QUOTE_LIBRARY_SEA_VIEW));
      grants.add(permissions.get(PermissionCodes.QUOTE_LIBRARY_FUMIGATION_VIEW));
    }

    if (roleCodes.contains(PermissionCodes.QUOTE_LIBRARY_ROAD_EDIT)
        || (roleCodes.contains(PermissionCodes.QUOTE_EDIT)
            && roleCodes.contains(PermissionCodes.COST_ROAD_EDIT))) {
      grants.add(permissions.get(PermissionCodes.QUOTE_LIBRARY_ROAD_EDIT));
    }
    if (roleCodes.contains(PermissionCodes.QUOTE_LIBRARY_SEA_EDIT)
        || (roleCodes.contains(PermissionCodes.QUOTE_EDIT)
            && roleCodes.contains(PermissionCodes.COST_SEA_EDIT))) {
      grants.add(permissions.get(PermissionCodes.QUOTE_LIBRARY_SEA_EDIT));
    }
    if (roleCodes.contains(PermissionCodes.QUOTE_LIBRARY_FUMIGATION_EDIT)
        || (roleCodes.contains(PermissionCodes.QUOTE_EDIT)
            && roleCodes.contains(PermissionCodes.COST_FUMIGATION_EDIT))) {
      grants.add(permissions.get(PermissionCodes.QUOTE_LIBRARY_FUMIGATION_EDIT));
    }

    if (roleCodes.contains(PermissionCodes.QUOTE_LIBRARY_ROAD_DELETE)
        || (roleCodes.contains(PermissionCodes.QUOTE_DELETE)
            && roleCodes.contains(PermissionCodes.COST_ROAD_EDIT))) {
      grants.add(permissions.get(PermissionCodes.QUOTE_LIBRARY_ROAD_DELETE));
    }
    if (roleCodes.contains(PermissionCodes.QUOTE_LIBRARY_SEA_DELETE)
        || (roleCodes.contains(PermissionCodes.QUOTE_DELETE)
            && roleCodes.contains(PermissionCodes.COST_SEA_EDIT))) {
      grants.add(permissions.get(PermissionCodes.QUOTE_LIBRARY_SEA_DELETE));
    }
    if (roleCodes.contains(PermissionCodes.QUOTE_LIBRARY_FUMIGATION_DELETE)
        || (roleCodes.contains(PermissionCodes.QUOTE_DELETE)
            && roleCodes.contains(PermissionCodes.COST_FUMIGATION_EDIT))) {
      grants.add(permissions.get(PermissionCodes.QUOTE_LIBRARY_FUMIGATION_DELETE));
    }

    return grants.stream().filter(p -> p != null).collect(Collectors.toSet());
  }
}
