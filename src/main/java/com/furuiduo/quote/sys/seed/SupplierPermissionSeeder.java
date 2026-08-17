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
import com.furuiduo.quote.sys.SupplierPermissionCodes;
import com.furuiduo.quote.sys.entity.PermissionType;
import com.furuiduo.quote.sys.entity.SysPermission;
import com.furuiduo.quote.sys.entity.SysRole;
import com.furuiduo.quote.sys.repository.SysPermissionRepository;
import com.furuiduo.quote.sys.repository.SysRoleRepository;

@Component
@Order(102)
public class SupplierPermissionSeeder implements ApplicationRunner {

  private record PermDef(String code, String name, int sort) {}

  private static final List<PermDef> SUPPLIER_PERMISSIONS =
      List.of(
          new PermDef(PermissionCodes.SUPPLIER_TRUCK_VIEW, "卡车供应商-查看", 63),
          new PermDef(PermissionCodes.SUPPLIER_TRUCK_CREATE, "卡车供应商-新建", 64),
          new PermDef(PermissionCodes.SUPPLIER_TRUCK_EDIT, "卡车供应商-编辑", 65),
          new PermDef(PermissionCodes.SUPPLIER_TRUCK_DELETE, "卡车供应商-删除", 66),
          new PermDef(PermissionCodes.SUPPLIER_FUMIGATION_VIEW, "熏蒸供应商-查看", 67),
          new PermDef(PermissionCodes.SUPPLIER_FUMIGATION_CREATE, "熏蒸供应商-新建", 68),
          new PermDef(PermissionCodes.SUPPLIER_FUMIGATION_EDIT, "熏蒸供应商-编辑", 69),
          new PermDef(PermissionCodes.SUPPLIER_FUMIGATION_DELETE, "熏蒸供应商-删除", 70),
          new PermDef(PermissionCodes.SUPPLIER_YARD_VIEW, "仓库堆场-查看", 71),
          new PermDef(PermissionCodes.SUPPLIER_YARD_CREATE, "仓库堆场-新建", 72),
          new PermDef(PermissionCodes.SUPPLIER_YARD_EDIT, "仓库堆场-编辑", 73),
          new PermDef(PermissionCodes.SUPPLIER_YARD_DELETE, "仓库堆场-删除", 74),
          new PermDef(PermissionCodes.SUPPLIER_OTHER_VIEW, "其他供应商-查看", 75),
          new PermDef(PermissionCodes.SUPPLIER_OTHER_CREATE, "其他供应商-新建", 76),
          new PermDef(PermissionCodes.SUPPLIER_OTHER_EDIT, "其他供应商-编辑", 77),
          new PermDef(PermissionCodes.SUPPLIER_OTHER_DELETE, "其他供应商-删除", 78));

  private static final Set<String> ROLES_WITH_ALL =
      Set.of("super_admin", "admin", "dept_manager", "sales");

  private static final Set<String> ROLES_WITH_VIEW_ONLY =
      Set.of("doc_clerk", "overseas_operator", "booker", "approver", "finance");

  private final SysPermissionRepository permissionRepository;
  private final SysRoleRepository roleRepository;

  public SupplierPermissionSeeder(
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
    for (PermDef def : SUPPLIER_PERMISSIONS) {
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
      for (String category : SupplierPermissionCodes.CATEGORIES) {
        addIfPresent(permissions, SupplierPermissionCodes.view(category), grants);
      }
      return grants;
    }

    Set<String> roleCodes =
        role.getPermissions().stream().map(SysPermission::getCode).collect(Collectors.toSet());

    // 旧版扁平权限 → 四类同动作
    for (String legacy :
        List.of(
            PermissionCodes.SUPPLIER_VIEW,
            PermissionCodes.SUPPLIER_CREATE,
            PermissionCodes.SUPPLIER_EDIT,
            PermissionCodes.SUPPLIER_DELETE)) {
      if (!roleCodes.contains(legacy)) {
        continue;
      }
      String action = SupplierPermissionCodes.legacyAction(legacy);
      if (action == null) {
        continue;
      }
      for (String category : SupplierPermissionCodes.CATEGORIES) {
        addIfPresent(permissions, SupplierPermissionCodes.of(category, action), grants);
      }
    }

    if (roleCodes.contains(PermissionCodes.QUOTE_CREATE)
        || roleCodes.contains(PermissionCodes.QUOTE_EDIT)
        || roleCodes.contains(PermissionCodes.COST_ROAD_EDIT)
        || roleCodes.contains(PermissionCodes.COST_FUMIGATION_EDIT)) {
      permissions.values().forEach(grants::add);
    } else if (roleCodes.contains(PermissionCodes.QUOTE_VIEW)
        || roleCodes.contains(PermissionCodes.COST_ROAD_VIEW)
        || roleCodes.contains(PermissionCodes.COST_FUMIGATION_VIEW)) {
      for (String category : SupplierPermissionCodes.CATEGORIES) {
        addIfPresent(permissions, SupplierPermissionCodes.view(category), grants);
      }
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
