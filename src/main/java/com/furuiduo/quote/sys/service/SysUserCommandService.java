package com.furuiduo.quote.sys.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.sys.dto.UserCreateRequest;
import com.furuiduo.quote.sys.dto.UserListItem;
import com.furuiduo.quote.sys.dto.UserUpdateRequest;
import com.furuiduo.quote.sys.entity.SysDepartment;
import com.furuiduo.quote.sys.entity.SysRole;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.repository.SysDepartmentRepository;
import com.furuiduo.quote.sys.repository.SysRoleRepository;
import com.furuiduo.quote.sys.repository.SysUserRepository;
import com.furuiduo.quote.user.PasswordStrengthEvaluator;

@Service
public class SysUserCommandService {

  private static final String SUPER_ADMIN = "super_admin";

  private final SysUserRepository userRepository;
  private final SysDepartmentRepository departmentRepository;
  private final SysRoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final PermissionService permissionService;

  public SysUserCommandService(
      SysUserRepository userRepository,
      SysDepartmentRepository departmentRepository,
      SysRoleRepository roleRepository,
      PasswordEncoder passwordEncoder,
      PermissionService permissionService) {
    this.userRepository = userRepository;
    this.departmentRepository = departmentRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
    this.permissionService = permissionService;
  }

  public UserListItem getById(Long id) {
    SysUser user =
        userRepository
            .findWithDetailsById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    return UserListItem.from(user, permissionService);
  }

  @Transactional
  public UserListItem create(UserCreateRequest request) {
    if (userRepository.existsByUsername(request.username())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
    }
    SysUser user = new SysUser();
    user.setUsername(request.username().trim());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    PasswordStrengthEvaluator.apply(user, request.password());
    user.setRealName(request.realName().trim());
    user.setDepartment(requireDepartment(request.deptId()));
    user.setStatus(request.status());
    user.setAvatar(request.avatar());
    user.setHomePath(
        request.homePath() == null || request.homePath().isBlank()
            ? "/workspace"
            : request.homePath());
    user.setRoles(resolveRoles(request.roleCodes()));
    user = userRepository.save(user);
    return getById(user.getId());
  }

  @Transactional
  public UserListItem update(Long id, UserUpdateRequest request, SysUser operator) {
    SysUser user =
        userRepository
            .findWithDetailsById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    boolean wasSuperAdmin = hasRole(user, SUPER_ADMIN);
    Set<SysRole> newRoles = resolveRoles(request.roleCodes());
    boolean willBeSuperAdmin =
        newRoles.stream().anyMatch(role -> SUPER_ADMIN.equals(role.getCode()));

    if (wasSuperAdmin && !willBeSuperAdmin && userRepository.countByRoles_Code(SUPER_ADMIN) <= 1) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cannot remove the last super administrator");
    }
    if (operator.getId().equals(id) && request.status() != null && request.status() == 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot disable your own account");
    }

    user.setRealName(request.realName().trim());
    user.setDepartment(requireDepartment(request.deptId()));
    user.setStatus(request.status());
    if (request.avatar() != null) {
      user.setAvatar(request.avatar());
    }
    if (request.homePath() != null && !request.homePath().isBlank()) {
      user.setHomePath(request.homePath());
    }
    user.setRoles(newRoles);
    if (request.password() != null && !request.password().isBlank()) {
      user.setPasswordHash(passwordEncoder.encode(request.password()));
      PasswordStrengthEvaluator.apply(user, request.password());
    }
    userRepository.save(user);
    return getById(id);
  }

  @Transactional
  public void delete(Long id, SysUser operator) {
    if (operator.getId().equals(id)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete your own account");
    }
    SysUser user =
        userRepository
            .findWithDetailsById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    if (hasRole(user, SUPER_ADMIN) && userRepository.countByRoles_Code(SUPER_ADMIN) <= 1) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cannot delete the last super administrator");
    }
    userRepository.delete(user);
  }

  private SysDepartment requireDepartment(Long deptId) {
    return departmentRepository
        .findById(deptId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid department"));
  }

  private Set<SysRole> resolveRoles(List<String> roleCodes) {
    if (roleCodes == null || roleCodes.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one role is required");
    }
    Set<SysRole> roles = new HashSet<>();
    for (String code : roleCodes) {
      SysRole role =
          roleRepository
              .findByCode(code)
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + code));
      roles.add(role);
    }
    return roles;
  }

  private boolean hasRole(SysUser user, String roleCode) {
    return user.getRoles().stream().anyMatch(role -> roleCode.equals(role.getCode()));
  }
}
