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

/**
 * 询价单权限：先入库（含写入码），再按报价能力补齐角色。
 *
 * <p>此前只在权限已存在时回填，且排在 {@code SuperAdminPermissionSyncSeeder}
 * 之前，导致首启业务角色拿不到 create/edit/submit/delete。
 */
@Component
@Order(107)
public class InquiryPermissionSeeder implements ApplicationRunner {

  private record PermDef(String code, String name, PermissionType type, int sort) {}

  private static final List<PermDef> INQUIRY_PERMISSIONS =
      List.of(
          new PermDef(PermissionCodes.INQUIRY_VIEW, "询价单-查看", PermissionType.MENU, 52),
          new PermDef(PermissionCodes.INQUIRY_CREATE, "询价单-新建", PermissionType.API, 53),
          new PermDef(PermissionCodes.INQUIRY_EDIT, "询价单-编辑", PermissionType.API, 54),
          new PermDef(PermissionCodes.INQUIRY_SUBMIT, "询价单-递交", PermissionType.API, 55),
          new PermDef(PermissionCodes.INQUIRY_DELETE, "询价单-删除", PermissionType.API, 56));

  /** 与报价单写权限对齐：可建/改报价的角色拿到询价全套写入 */
  private static final Set<String> ROLES_WITH_WRITE =
      Set.of("super_admin", "admin", "dept_manager", "sales");

  private final SysPermissionRepository permissionRepository;
  private final SysRoleRepository roleRepository;

  public InquiryPermissionSeeder(
      SysPermissionRepository permissionRepository, SysRoleRepository roleRepository) {
    this.permissionRepository = permissionRepository;
    this.roleRepository = roleRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    Map<String, SysPermission> inquiryPermissions = ensureInquiryPermissions();
    for (SysRole role : roleRepository.findAll()) {
      if (grantInquiryPermissions(role, inquiryPermissions)) {
        roleRepository.save(role);
      }
    }
  }

  private Map<String, SysPermission> ensureInquiryPermissions() {
    Map<String, SysPermission> map = new LinkedHashMap<>();
    for (PermDef def : INQUIRY_PERMISSIONS) {
      SysPermission permission =
          permissionRepository
              .findByCode(def.code())
              .orElseGet(
                  () -> {
                    SysPermission created = new SysPermission();
                    created.setCode(def.code());
                    created.setType(def.type());
                    return created;
                  });
      permission.setName(def.name());
      permission.setType(def.type());
      permission.setSort(def.sort());
      permission = permissionRepository.save(permission);
      map.put(def.code(), permission);
    }
    return map;
  }

  private boolean grantInquiryPermissions(
      SysRole role, Map<String, SysPermission> inquiryPermissions) {
    Set<SysPermission> grants = shouldGrantForRole(role, inquiryPermissions);
    if (grants.isEmpty()) {
      return false;
    }
    Set<SysPermission> current =
        role.getPermissions() == null ? new HashSet<>() : role.getPermissions();
    int before = current.size();
    current.addAll(grants);
    role.setPermissions(current);
    return current.size() > before;
  }

  private Set<SysPermission> shouldGrantForRole(
      SysRole role, Map<String, SysPermission> inquiryPermissions) {
    Set<SysPermission> grants = new HashSet<>();
    String code = role.getCode();

    if ("super_admin".equals(code) || ROLES_WITH_WRITE.contains(code)) {
      inquiryPermissions.values().forEach(grants::add);
      return grants;
    }

    Set<String> roleCodes =
        role.getPermissions() == null
            ? Set.of()
            : role.getPermissions().stream()
                .map(SysPermission::getCode)
                .collect(Collectors.toSet());

    // 有报价新建/编辑 → 询价写入全套；仅有报价查看 → 只给询价查看
    if (roleCodes.contains(PermissionCodes.QUOTE_CREATE)
        || roleCodes.contains(PermissionCodes.QUOTE_EDIT)) {
      inquiryPermissions.values().forEach(grants::add);
    } else if (roleCodes.contains(PermissionCodes.QUOTE_VIEW)) {
      addIfPresent(inquiryPermissions, PermissionCodes.INQUIRY_VIEW, grants);
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
