package com.furuiduo.quote.sys.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "用户创建请求")
public record UserCreateRequest(
    @NotBlank @Size(max = 64) @Schema(description = "登录账号") String username,
    @NotBlank @Size(min = 6, max = 64) @Schema(description = "初始密码") String password,
    @NotBlank @Size(max = 64) @Schema(description = "姓名") String realName,
    @NotNull @Schema(description = "部门 ID") Long deptId,
    @NotEmpty @Schema(description = "角色编码列表") List<String> roleCodes,
    @NotNull @Schema(description = "状态：1 启用，0 停用") Integer status,
    @Size(max = 512) @Schema(description = "头像 URL") String avatar,
    @Size(max = 128) @Schema(description = "默认首页") String homePath) {}
