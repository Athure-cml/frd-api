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
@Order(101)
public class CustomerPermissionSeeder implements ApplicationRunner {

  private record PermDef(String code, String name, int sort) {}

  private static final List<PermDef> CUSTOMER_PERMISSIONS =
      List.of(
          new PermDef(PermissionCodes.CUSTOMER_VIEW, "客户-查看", 45),
          new PermDef(PermissionCodes.CUSTOMER_CREATE, "客户-新建", 46),
          new PermDef(PermissionCodes.CUSTOMER_EDIT, "客户-编辑", 47),
          new PermDef(PermissionCodes.CUSTOMER_DELETE, "客户-删除", 48));

  private static final Set<String> ROLES_WITH_ALL_CUSTOMER =
      Set.of("super_admin", "admin", "dept_manager", "sales");

  private static final Set<String> ROLES_WITH_VIEW_ONLY =
      Set.of("doc_clerk", "overseas_operator", "booker", "approver", "finance");

  private final SysPermissionRepository permissionRepository;
  private final SysRoleRepository roleRepository;

  public CustomerPermissionSeeder(
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

    Map<String, SysPermission> customerPermissions = ensureCustomerPermissions();
    for (SysRole role : roleRepository.findAll()) {
      if (grantCustomerPermissions(role, customerPermissions)) {
        roleRepository.save(role);
      }
    }
  }

  private Map<String, SysPermission> ensureCustomerPermissions() {
    Map<String, SysPermission> map = new LinkedHashMap<>();
    for (PermDef def : CUSTOMER_PERMISSIONS) {
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

  private boolean grantCustomerPermissions(
      SysRole role, Map<String, SysPermission> customerPermissions) {
    Set<SysPermission> grants = shouldGrantForRole(role, customerPermissions);
    if (grants.isEmpty()) {
      return false;
    }
    Set<SysPermission> current = role.getPermissions();
    int before = current.size();
    current.addAll(grants);
    return current.size() > before;
  }

  private Set<SysPermission> shouldGrantForRole(
      SysRole role, Map<String, SysPermission> customerPermissions) {
    Set<SysPermission> grants = new HashSet<>();
    String code = role.getCode();

    if ("super_admin".equals(code)) {
      customerPermissions.values().forEach(grants::add);
      return grants;
    }

    if (ROLES_WITH_ALL_CUSTOMER.contains(code)) {
      customerPermissions.values().forEach(grants::add);
      return grants;
    }

    if (ROLES_WITH_VIEW_ONLY.contains(code)) {
      addIfPresent(customerPermissions, PermissionCodes.CUSTOMER_VIEW, grants);
      return grants;
    }

    Set<String> roleCodes =
        role.getPermissions().stream().map(SysPermission::getCode).collect(Collectors.toSet());
    if (roleCodes.contains(PermissionCodes.QUOTE_CREATE)
        || roleCodes.contains(PermissionCodes.QUOTE_EDIT)) {
      customerPermissions.values().forEach(grants::add);
    } else if (roleCodes.contains(PermissionCodes.QUOTE_VIEW)) {
      addIfPresent(customerPermissions, PermissionCodes.CUSTOMER_VIEW, grants);
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
