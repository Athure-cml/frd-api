package com.furuiduo.quote.sys.seed;

import java.util.HashSet;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.sys.entity.SysPermission;
import com.furuiduo.quote.sys.entity.SysRole;
import com.furuiduo.quote.sys.repository.SysPermissionRepository;
import com.furuiduo.quote.sys.repository.SysRoleRepository;

/** 启动末尾同步 super_admin 拥有库内全部权限（修复首次部署与增量权限遗漏）。 */
@Component
@Order(999)
public class SuperAdminPermissionSyncSeeder implements ApplicationRunner {

  private final SysPermissionRepository permissionRepository;
  private final SysRoleRepository roleRepository;

  public SuperAdminPermissionSyncSeeder(
      SysPermissionRepository permissionRepository, SysRoleRepository roleRepository) {
    this.permissionRepository = permissionRepository;
    this.roleRepository = roleRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    PermissionCatalog.ensureAll(permissionRepository);

    roleRepository
        .findByCode("super_admin")
        .ifPresent(
            role -> {
              if (syncAllPermissions(role)) {
                roleRepository.save(role);
              }
            });
  }

  private boolean syncAllPermissions(SysRole role) {
    var allPermissions = new HashSet<>(permissionRepository.findAll());
    var current = role.getPermissions();
    if (current.size() == allPermissions.size() && current.containsAll(allPermissions)) {
      return false;
    }
    role.setPermissions(allPermissions);
    return true;
  }
}
