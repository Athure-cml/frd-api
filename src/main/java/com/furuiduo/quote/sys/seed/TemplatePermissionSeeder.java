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

import com.furuiduo.quote.sys.CostTemplatePermissionCodes;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.PermissionType;
import com.furuiduo.quote.sys.entity.SysPermission;
import com.furuiduo.quote.sys.entity.SysRole;
import com.furuiduo.quote.sys.repository.SysPermissionRepository;
import com.furuiduo.quote.sys.repository.SysRoleRepository;

@Component
@Order(100)
public class TemplatePermissionSeeder implements ApplicationRunner {

  private record PermDef(String code, String name, int sort) {}

  private static final List<PermDef> TEMPLATE_PERMISSIONS =
      List.of(
          new PermDef(PermissionCodes.COST_ROAD_TEMPLATE_VIEW, "卡车模板-查看", 36),
          new PermDef(PermissionCodes.COST_ROAD_TEMPLATE_EDIT, "卡车模板-编辑", 37),
          new PermDef(PermissionCodes.COST_ROAD_TEMPLATE_DELETE, "卡车模板-删除", 38),
          new PermDef(PermissionCodes.COST_SEA_TEMPLATE_VIEW, "海运模板-查看", 39),
          new PermDef(PermissionCodes.COST_SEA_TEMPLATE_EDIT, "海运模板-编辑", 40),
          new PermDef(PermissionCodes.COST_SEA_TEMPLATE_DELETE, "海运模板-删除", 41),
          new PermDef(PermissionCodes.COST_FUMIGATION_TEMPLATE_VIEW, "熏蒸模板-查看", 42),
          new PermDef(PermissionCodes.COST_FUMIGATION_TEMPLATE_EDIT, "熏蒸模板-编辑", 43),
          new PermDef(PermissionCodes.COST_FUMIGATION_TEMPLATE_DELETE, "熏蒸模板-删除", 44));

  private final SysPermissionRepository permissionRepository;
  private final SysRoleRepository roleRepository;

  public TemplatePermissionSeeder(
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

    Map<String, SysPermission> templatePermissions = ensureTemplatePermissions();
    for (SysRole role : roleRepository.findAll()) {
      if (grantTemplatePermissions(role, templatePermissions)) {
        roleRepository.save(role);
      }
    }
  }

  private Map<String, SysPermission> ensureTemplatePermissions() {
    Map<String, SysPermission> map = new LinkedHashMap<>();
    for (PermDef def : TEMPLATE_PERMISSIONS) {
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

  private boolean grantTemplatePermissions(
      SysRole role, Map<String, SysPermission> templatePermissions) {
    Set<SysPermission> grants = shouldGrantForRole(role, templatePermissions);
    if (grants.isEmpty()) {
      return false;
    }

    Set<SysPermission> current = role.getPermissions();
    int before = current.size();
    current.addAll(grants);
    return current.size() > before;
  }

  private Set<SysPermission> shouldGrantForRole(
      SysRole role, Map<String, SysPermission> templatePermissions) {
    Set<SysPermission> grants = new HashSet<>();

    if ("super_admin".equals(role.getCode())) {
      templatePermissions.values().forEach(grants::add);
      return grants;
    }

    Set<String> roleCodes =
        role.getPermissions().stream().map(SysPermission::getCode).collect(Collectors.toSet());

    grantByCostPermission(
        roleCodes,
        templatePermissions,
        "road",
        PermissionCodes.COST_ROAD_VIEW,
        PermissionCodes.COST_ROAD_EDIT,
        grants);
    grantByCostPermission(
        roleCodes,
        templatePermissions,
        "sea",
        PermissionCodes.COST_SEA_VIEW,
        PermissionCodes.COST_SEA_EDIT,
        grants);
    grantByCostPermission(
        roleCodes,
        templatePermissions,
        "fumigation",
        PermissionCodes.COST_FUMIGATION_VIEW,
        PermissionCodes.COST_FUMIGATION_EDIT,
        grants);
    grantByCostPermission(
        roleCodes,
        templatePermissions,
        "rail",
        PermissionCodes.COST_RAIL_VIEW,
        PermissionCodes.COST_RAIL_EDIT,
        grants);

    return grants;
  }

  private void grantByCostPermission(
      Set<String> roleCodes,
      Map<String, SysPermission> templatePermissions,
      String mode,
      String viewCode,
      String editCode,
      Set<SysPermission> grants) {
    if (roleCodes.contains(editCode)) {
      addIfPresent(templatePermissions, CostTemplatePermissionCodes.view(mode), grants);
      addIfPresent(templatePermissions, CostTemplatePermissionCodes.edit(mode), grants);
      addIfPresent(templatePermissions, CostTemplatePermissionCodes.delete(mode), grants);
      return;
    }

    if (roleCodes.contains(viewCode)) {
      addIfPresent(templatePermissions, CostTemplatePermissionCodes.view(mode), grants);
    }
  }

  private void addIfPresent(
      Map<String, SysPermission> templatePermissions, String code, Set<SysPermission> grants) {
    SysPermission permission = templatePermissions.get(code);
    if (permission != null) {
      grants.add(permission);
    }
  }
}
