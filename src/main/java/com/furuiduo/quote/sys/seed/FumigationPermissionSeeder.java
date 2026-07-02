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
public class FumigationPermissionSeeder implements ApplicationRunner {

  private record PermDef(String code, String name, int sort) {}

  private static final List<PermDef> FUMIGATION_PERMISSIONS =
      List.of(
          new PermDef(PermissionCodes.COST_FUMIGATION_VIEW, "熏蒸成本-查看", 34),
          new PermDef(PermissionCodes.COST_FUMIGATION_EDIT, "熏蒸成本-编辑", 35),
          new PermDef(PermissionCodes.COST_FUMIGATION_TEMPLATE_VIEW, "熏蒸模板-查看", 42),
          new PermDef(PermissionCodes.COST_FUMIGATION_TEMPLATE_EDIT, "熏蒸模板-编辑", 43),
          new PermDef(PermissionCodes.COST_FUMIGATION_TEMPLATE_DELETE, "熏蒸模板-删除", 44));

  private static final List<String> LEGACY_RAIL_CODES =
      List.of(
          "cost:rail:view",
          "cost:rail:edit",
          "cost:rail:template:view",
          "cost:rail:template:edit",
          "cost:rail:template:delete");

  private final SysPermissionRepository permissionRepository;
  private final SysRoleRepository roleRepository;

  public FumigationPermissionSeeder(
      SysPermissionRepository permissionRepository, SysRoleRepository roleRepository) {
    this.permissionRepository = permissionRepository;
    this.roleRepository = roleRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    Map<String, SysPermission> fumigationPermissions = ensureFumigationPermissions();
    for (SysRole role : roleRepository.findAll()) {
      if (migrateRolePermissions(role, fumigationPermissions)) {
        roleRepository.save(role);
      }
    }
  }

  private Map<String, SysPermission> ensureFumigationPermissions() {
    Map<String, SysPermission> map = new LinkedHashMap<>();
    for (PermDef def : FUMIGATION_PERMISSIONS) {
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

  private boolean migrateRolePermissions(
      SysRole role, Map<String, SysPermission> fumigationPermissions) {
    Set<String> roleCodes =
        role.getPermissions().stream().map(SysPermission::getCode).collect(Collectors.toSet());

    boolean hadRail =
        roleCodes.stream().anyMatch(LEGACY_RAIL_CODES::contains)
            || roleCodes.contains(PermissionCodes.COST_RAIL_VIEW)
            || roleCodes.contains(PermissionCodes.COST_RAIL_EDIT);

    if (!hadRail && !"super_admin".equals(role.getCode())) {
      return false;
    }

    Set<SysPermission> grants = new HashSet<>(role.getPermissions());
    int before = grants.size();

    if ("super_admin".equals(role.getCode())) {
      fumigationPermissions.values().forEach(grants::add);
    } else if (roleCodes.contains("cost:rail:edit")
        || roleCodes.contains(PermissionCodes.COST_RAIL_EDIT)) {
      grants.add(fumigationPermissions.get(PermissionCodes.COST_FUMIGATION_VIEW));
      grants.add(fumigationPermissions.get(PermissionCodes.COST_FUMIGATION_EDIT));
      grants.add(
          fumigationPermissions.get(PermissionCodes.COST_FUMIGATION_TEMPLATE_VIEW));
      grants.add(
          fumigationPermissions.get(PermissionCodes.COST_FUMIGATION_TEMPLATE_EDIT));
      grants.add(
          fumigationPermissions.get(PermissionCodes.COST_FUMIGATION_TEMPLATE_DELETE));
    } else if (roleCodes.contains("cost:rail:view")
        || roleCodes.contains(PermissionCodes.COST_RAIL_VIEW)) {
      grants.add(fumigationPermissions.get(PermissionCodes.COST_FUMIGATION_VIEW));
      grants.add(
          fumigationPermissions.get(PermissionCodes.COST_FUMIGATION_TEMPLATE_VIEW));
    }

    role.setPermissions(grants);
    return grants.size() > before;
  }
}
