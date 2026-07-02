package com.furuiduo.quote.sys.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.furuiduo.quote.sys.dto.DepartmentResponse;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.repository.SysUserRepository;
import com.furuiduo.quote.user.PasswordSecurityInfo;
import com.furuiduo.quote.user.UserInfoResponse;

@Service
public class UserAccountService {

  private final SysUserRepository sysUserRepository;
  private final PermissionService permissionService;
  private final PasswordEncoder passwordEncoder;

  public UserAccountService(
      SysUserRepository sysUserRepository,
      PermissionService permissionService,
      PasswordEncoder passwordEncoder) {
    this.sysUserRepository = sysUserRepository;
    this.permissionService = permissionService;
    this.passwordEncoder = passwordEncoder;
  }

  public boolean matchesPassword(SysUser user, String rawPassword) {
    return passwordEncoder.matches(rawPassword, user.getPasswordHash());
  }

  public SysUser requireByUsername(String username) {
    return sysUserRepository
        .findWithDetailsByUsername(username)
        .orElseThrow(() -> new IllegalStateException("User not found: " + username));
  }

  public UserInfoResponse toUserInfo(SysUser user, String token) {
    return new UserInfoResponse(
        String.valueOf(user.getId()),
        user.getUsername(),
        user.getRealName(),
        user.getAvatar(),
        user.getPhone(),
        user.getEmail(),
        permissionService.getRoleCodes(user),
        permissionService.getRoleNames(user),
        DepartmentResponse.from(user.getDepartment()),
        permissionService.getEffectiveDataScope(user).name(),
        "",
        user.getHomePath(),
        PasswordSecurityInfo.from(user),
        token);
  }
}
