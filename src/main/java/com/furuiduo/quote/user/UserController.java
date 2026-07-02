package com.furuiduo.quote.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.multipart.MultipartFile;

import com.furuiduo.quote.auth.AuthService;
import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.UserAccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "用户", description = "当前登录用户信息")
@RestController
@RequestMapping("/user")
public class UserController {

  private final AuthService authService;
  private final UserAccountService userAccountService;
  private final UserProfileService userProfileService;

  public UserController(
      AuthService authService,
      UserAccountService userAccountService,
      UserProfileService userProfileService) {
    this.authService = authService;
    this.userAccountService = userAccountService;
    this.userProfileService = userProfileService;
  }

  @Operation(
      summary = "获取当前用户信息",
      description = "返回用户资料、部门、角色与数据权限范围。",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/info")
  public ApiResponse<UserInfoResponse> info(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    SysUser user = authService.requireUser(authorization);
    return ApiResponse.ok(userAccountService.toUserInfo(user, extractToken(authorization)));
  }

  @Operation(
      summary = "更新当前用户资料",
      description = "允许修改姓名与头像 URL。",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/profile")
  public ApiResponse<UserInfoResponse> updateProfile(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @Valid @RequestBody ProfileUpdateRequest request) {
    SysUser user = authService.requireUser(authorization);
    return ApiResponse.ok(
        userProfileService.updateProfile(user, request, extractToken(authorization)));
  }

  @Operation(
      summary = "上传当前用户头像",
      description = "上传图片并更新头像，支持 JPG/PNG/GIF/WEBP，最大 2MB。",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/avatar")
  public ApiResponse<UserInfoResponse> uploadAvatar(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam("file") MultipartFile file) {
    SysUser user = authService.requireUser(authorization);
    return ApiResponse.ok(
        userProfileService.uploadAvatar(user, file, extractToken(authorization)));
  }

  @Operation(
      summary = "修改当前用户密码",
      description = "校验旧密码后更新为新密码。",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/password")
  public ApiResponse<String> changePassword(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @Valid @RequestBody ChangePasswordRequest request) {
    SysUser user = authService.requireUser(authorization);
    userProfileService.changePassword(user, request);
    return ApiResponse.ok("");
  }

  private static String extractToken(String authorization) {
    return authorization != null && authorization.startsWith("Bearer ")
        ? authorization.substring("Bearer ".length()).trim()
        : "";
  }
}
