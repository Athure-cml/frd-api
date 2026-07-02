package com.furuiduo.quote.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "当前用户修改密码")
public record ChangePasswordRequest(
    @NotBlank @Schema(description = "当前密码") String oldPassword,
    @NotBlank @Size(min = 6, max = 64) @Schema(description = "新密码") String newPassword) {}
