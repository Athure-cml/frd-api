package com.furuiduo.quote.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "当前用户资料更新")
public record ProfileUpdateRequest(
    @NotBlank @Size(max = 64) @Schema(description = "姓名") String realName,
    @Size(max = 512) @Schema(description = "头像 URL") String avatar,
    @Size(max = 20) @Schema(description = "手机号") String phone,
    @Size(max = 128) @Schema(description = "邮箱") String email) {}
