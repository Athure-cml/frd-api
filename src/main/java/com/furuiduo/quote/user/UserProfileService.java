package com.furuiduo.quote.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.repository.SysUserRepository;
import com.furuiduo.quote.sys.service.PermissionService;

@Service
public class UserProfileService {

  private final SysUserRepository sysUserRepository;
  private final PermissionService permissionService;
  private final PasswordEncoder passwordEncoder;
  private final AvatarStorageService avatarStorageService;

  public UserProfileService(
      SysUserRepository sysUserRepository,
      PermissionService permissionService,
      PasswordEncoder passwordEncoder,
      AvatarStorageService avatarStorageService) {
    this.sysUserRepository = sysUserRepository;
    this.permissionService = permissionService;
    this.passwordEncoder = passwordEncoder;
    this.avatarStorageService = avatarStorageService;
  }

  @Transactional
  public UserInfoResponse updateProfile(SysUser user, ProfileUpdateRequest request, String token) {
    SysUser current =
        sysUserRepository
            .findWithDetailsById(user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    current.setRealName(request.realName().trim());
    if (request.avatar() != null) {
      current.setAvatar(request.avatar().isBlank() ? null : request.avatar().trim());
    }
    if (request.phone() != null) {
      String phone = request.phone().trim();
      current.setPhone(phone.isBlank() ? null : phone);
    }
    if (request.email() != null) {
      String email = request.email().trim();
      if (!email.isBlank() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱格式不正确");
      }
      current.setEmail(email.isBlank() ? null : email);
    }
    sysUserRepository.save(current);
    return toUserInfo(current, token);
  }

  @Transactional
  public UserInfoResponse uploadAvatar(SysUser user, MultipartFile file, String token) {
    SysUser current =
        sysUserRepository
            .findWithDetailsById(user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    try {
      current.setAvatar(avatarStorageService.store(current, file));
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "头像上传失败");
    }
    sysUserRepository.save(current);
    return toUserInfo(current, token);
  }

  @Transactional
  public void changePassword(SysUser user, ChangePasswordRequest request) {
    SysUser current =
        sysUserRepository
            .findWithDetailsById(user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    if (!passwordEncoder.matches(request.oldPassword(), current.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前密码不正确");
    }
    if (passwordEncoder.matches(request.newPassword(), current.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新密码不能与当前密码相同");
    }
    current.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    PasswordStrengthEvaluator.apply(current, request.newPassword());
    sysUserRepository.save(current);
  }

  private UserInfoResponse toUserInfo(SysUser user, String token) {
    return new UserInfoResponse(
        String.valueOf(user.getId()),
        user.getUsername(),
        user.getRealName(),
        user.getAvatar(),
        user.getPhone(),
        user.getEmail(),
        permissionService.getRoleCodes(user),
        permissionService.getRoleNames(user),
        com.furuiduo.quote.sys.dto.DepartmentResponse.from(user.getDepartment()),
        permissionService.getEffectiveDataScope(user).name(),
        "",
        user.getHomePath(),
        PasswordSecurityInfo.from(user),
        token);
  }
}
