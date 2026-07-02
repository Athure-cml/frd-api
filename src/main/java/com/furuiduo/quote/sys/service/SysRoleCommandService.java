package com.furuiduo.quote.sys.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.sys.dto.RoleListItem;
import com.furuiduo.quote.sys.dto.RoleSaveRequest;
import com.furuiduo.quote.sys.entity.DataScope;
import com.furuiduo.quote.sys.entity.SysPermission;
import com.furuiduo.quote.sys.entity.SysRole;
import com.furuiduo.quote.sys.repository.SysPermissionRepository;
import com.furuiduo.quote.sys.repository.SysRoleRepository;
import com.furuiduo.quote.sys.repository.SysUserRepository;

@Service
public class SysRoleCommandService {

  private static final String SUPER_ADMIN = "super_admin";

  private final SysRoleRepository roleRepository;
  private final SysPermissionRepository permissionRepository;
  private final SysUserRepository userRepository;

  public SysRoleCommandService(
      SysRoleRepository roleRepository,
      SysPermissionRepository permissionRepository,
      SysUserRepository userRepository) {
    this.roleRepository = roleRepository;
    this.permissionRepository = permissionRepository;
    this.userRepository = userRepository;
  }

  public RoleListItem getById(Long id) {
    return roleRepository
        .findWithPermissionsById(id)
        .map(RoleListItem::from)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
  }

  @Transactional
  public RoleListItem create(RoleSaveRequest request) {
    if (roleRepository.findByCode(request.code()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Role code already exists");
    }
    SysRole role = new SysRole();
    apply(role, request, true);
    return RoleListItem.from(roleRepository.save(role));
  }

  @Transactional
  public RoleListItem update(Long id, RoleSaveRequest request) {
    SysRole role =
        roleRepository
            .findWithPermissionsById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
    if (SUPER_ADMIN.equals(role.getCode()) && !SUPER_ADMIN.equals(request.code())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot rename built-in super_admin role");
    }
    if (!role.getCode().equals(request.code()) && roleRepository.findByCode(request.code()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Role code already exists");
    }
    apply(role, request, !SUPER_ADMIN.equals(role.getCode()));
    return RoleListItem.from(roleRepository.save(role));
  }

  @Transactional
  public void delete(Long id) {
    SysRole role =
        roleRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
    if (SUPER_ADMIN.equals(role.getCode())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete built-in super_admin role");
    }
    if (userRepository.countByRoles_Code(role.getCode()) > 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cannot delete role assigned to users");
    }
    roleRepository.delete(role);
  }

  private void apply(SysRole role, RoleSaveRequest request, boolean allowCodeChange) {
    if (allowCodeChange) {
      role.setCode(request.code().trim());
    }
    role.setName(request.name().trim());
    role.setDataScope(parseDataScope(request.dataScope()));
    role.setStatus(request.status());
    role.setRemark(request.remark());
    role.setPermissions(resolvePermissions(request.permissionCodes()));
  }

  private DataScope parseDataScope(String value) {
    try {
      return DataScope.valueOf(value);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid data scope");
    }
  }

  private Set<SysPermission> resolvePermissions(List<String> permissionCodes) {
    Set<SysPermission> permissions = new HashSet<>();
    if (permissionCodes == null) {
      return permissions;
    }
    for (String code : permissionCodes) {
      SysPermission permission =
          permissionRepository
              .findByCode(code)
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          HttpStatus.BAD_REQUEST, "Invalid permission: " + code));
      permissions.add(permission);
    }
    return permissions;
  }
}
