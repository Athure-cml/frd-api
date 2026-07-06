package com.furuiduo.quote.sys.dto;


import com.furuiduo.quote.quote.support.QuoteDateTimes;
import com.furuiduo.quote.sys.entity.OperationAction;
import com.furuiduo.quote.sys.entity.SysOperationLog;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "操作日志")
public record OperationLogResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "用户 ID") Long userId,
    @Schema(description = "账号") String username,
    @Schema(description = "姓名") String realName,
    @Schema(description = "模块") String module,
    @Schema(description = "操作") OperationAction action,
    @Schema(description = "资源类型") String resourceType,
    @Schema(description = "资源 ID") String resourceId,
    @Schema(description = "摘要") String summary,
    @Schema(description = "请求方法") String requestMethod,
    @Schema(description = "请求路径") String requestUri,
    @Schema(description = "请求体") String requestBody,
    @Schema(description = "IP") String ipAddress,
    @Schema(description = "是否成功") Boolean success,
    @Schema(description = "错误信息") String errorMessage,
    @Schema(description = "时间") String createdAt) {

  public static OperationLogResponse from(SysOperationLog log) {
    return new OperationLogResponse(
        log.getId(),
        log.getUserId(),
        log.getUsername(),
        log.getRealName(),
        log.getModule(),
        log.getAction(),
        log.getResourceType(),
        log.getResourceId(),
        log.getSummary(),
        log.getRequestMethod(),
        log.getRequestUri(),
        log.getRequestBody(),
        log.getIpAddress(),
        log.getSuccess(),
        log.getErrorMessage(),
        QuoteDateTimes.format(log.getCreatedAt()));
  }
}
