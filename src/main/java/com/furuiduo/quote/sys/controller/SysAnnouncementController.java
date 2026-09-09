package com.furuiduo.quote.sys.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.furuiduo.quote.auth.AuthService;
import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.sys.dto.AnnouncementResponse;
import com.furuiduo.quote.sys.dto.AnnouncementSaveRequest;
import com.furuiduo.quote.sys.service.AnnouncementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "系统-公告", description = "全员功能更新通知")
@RestController
@RequestMapping("/sys/announcements")
public class SysAnnouncementController {

  private final AuthService authService;
  private final AnnouncementService announcementService;

  public SysAnnouncementController(
      AuthService authService, AnnouncementService announcementService) {
    this.authService = authService;
    this.announcementService = announcementService;
  }

  @Operation(
      summary = "当前用户未读公告",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/pending")
  public ApiResponse<List<AnnouncementResponse>> pending(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return ApiResponse.ok(
        announcementService.listPending(authService.requireUser(authorization)));
  }

  @Operation(
      summary = "确认已读公告",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/ack")
  public ApiResponse<Void> acknowledge(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    announcementService.acknowledge(authService.requireUser(authorization), id);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "公告列表（管理员）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<List<AnnouncementResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String createdByName,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime startAt,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime endAt) {
    return ApiResponse.ok(
        announcementService.listAll(
            authService.requireUser(authorization),
            keyword,
            status,
            createdByName,
            startAt,
            endAt));
  }

  @Operation(
      summary = "创建公告（管理员）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<AnnouncementResponse> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @Valid @RequestBody AnnouncementSaveRequest request) {
    return ApiResponse.ok(
        announcementService.create(authService.requireUser(authorization), request));
  }

  @Operation(
      summary = "更新公告（管理员）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<AnnouncementResponse> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @Valid @RequestBody AnnouncementSaveRequest request) {
    return ApiResponse.ok(
        announcementService.update(authService.requireUser(authorization), id, request));
  }

  @Operation(
      summary = "复制公告为草稿（管理员）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/copy")
  public ApiResponse<AnnouncementResponse> copy(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    return ApiResponse.ok(
        announcementService.copy(authService.requireUser(authorization), id));
  }

  @Operation(
      summary = "停用公告（管理员）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/disable")
  public ApiResponse<AnnouncementResponse> disable(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    return ApiResponse.ok(
        announcementService.disable(authService.requireUser(authorization), id));
  }

  @Operation(
      summary = "删除公告（管理员）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    announcementService.delete(authService.requireUser(authorization), id);
    return ApiResponse.ok(null);
  }
}
