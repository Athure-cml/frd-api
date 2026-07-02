package com.furuiduo.quote.user;

import java.util.List;

import com.furuiduo.quote.sys.dto.DepartmentResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户信息")
public record UserInfoResponse(
    @Schema(description = "用户 ID", example = "1") String userId,
    @Schema(description = "工号", example = "vben") String username,
    @Schema(description = "显示名称", example = "系统管理员") String realName,
    @Schema(description = "头像 URL") String avatar,
    @Schema(description = "手机号") String phone,
    @Schema(description = "邮箱") String email,
    @Schema(description = "角色编码列表", example = "[\"super_admin\"]") List<String> roles,
    @Schema(description = "角色名称列表", example = "[\"超级管理员\"]") List<String> roleNames,
    @Schema(description = "所属部门") DepartmentResponse dept,
    @Schema(description = "数据权限范围", example = "ALL") String dataScope,
    @Schema(description = "用户描述") String desc,
    @Schema(description = "登录后默认首页", example = "/workspace") String homePath,
    @Schema(description = "密码安全评估") PasswordSecurityInfo passwordSecurity,
    @Schema(description = "当前 accessToken") String token) {}
