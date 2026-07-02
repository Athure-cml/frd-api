package com.furuiduo.quote.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一 API 响应")
public record ApiResponse<T>(
    @Schema(description = "业务状态码，0 成功，-1 失败", example = "0") int code,
    @Schema(description = "业务数据") T data,
    @Schema(description = "错误信息，成功时为 null") String error,
    @Schema(description = "提示信息", example = "ok") String message) {

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(0, data, null, "ok");
  }

  public static <T> ApiResponse<T> fail(String message) {
    return new ApiResponse<>(-1, null, message, message);
  }
}
