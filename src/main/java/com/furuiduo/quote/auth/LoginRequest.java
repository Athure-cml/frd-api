package com.furuiduo.quote.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录请求")
public record LoginRequest(
    @Schema(description = "工号", example = "vben") String username,
    @Schema(description = "密码", example = "123456") String password) {}
