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
import com.furuiduo.quote.unit.entity.Unit;
import com.furuiduo.quote.unit.repository.UnitRepository;

@Component
@Order(103)
public class UnitSeeder implements ApplicationRunner {

  private record PermDef(String code, String name, int sort) {}

  private record UnitDef(String code, String name, int sort) {}

  private static final List<PermDef> PERMISSIONS =
      List.of(
          new PermDef(PermissionCodes.UNIT_VIEW, "单位-查看", 53),
          new PermDef(PermissionCodes.UNIT_MANAGE, "单位-管理", 54));

  private static final List<UnitDef> DEFAULT_UNITS =
      List.of(
          new UnitDef("day", "天", 1),
          new UnitDef("days", "天(复数)", 2),
          new UnitDef("hour", "小时", 3),
          new UnitDef("hours", "小时(复数)", 4),
          new UnitDef("trip", "趟", 5),
          new UnitDef("container", "箱", 6));

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
  private final UnitRepository unitRepository;

  public UnitSeeder(
      SysPermissionRepository permissionRepository,
      SysRoleRepository roleRepository,
      UnitRepository unitRepository) {
    this.permissionRepository = permissionRepository;
    this.roleRepository = roleRepository;
    this.unitRepository = unitRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    seedUnits();
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

  private void seedUnits() {
    for (UnitDef def : DEFAULT_UNITS) {
      if (unitRepository.existsByCodeNormalized(def.code(), null)) {
        continue;
      }
      Unit unit = new Unit();
      unit.setCode(def.code());
      unit.setName(def.name());
      unit.setSort(def.sort());
      unit.setStatus(1);
      unitRepository.save(unit);
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
    Set<SysPermission> grants = new HashSet<>();
    String code = role.getCode();

    if ("super_admin".equals(code) || ROLES_WITH_MANAGE.contains(code)) {
      permissions.values().forEach(grants::add);
    } else if (ROLES_WITH_VIEW.contains(code)) {
      SysPermission view = permissions.get(PermissionCodes.UNIT_VIEW);
      if (view != null) {
        grants.add(view);
      }
    }

    if (grants.isEmpty()) {
      return false;
    }
    Set<SysPermission> current = role.getPermissions();
    int before = current.size();
    current.addAll(grants);
    return current.size() > before;
  }
}
