package com.furuiduo.quote.quote.controller;

import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.auth.AuthService;
import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.quote.dto.InquiryDetailResponse;
import com.furuiduo.quote.quote.dto.InquiryListItem;
import com.furuiduo.quote.quote.dto.InquirySaveRequest;
import com.furuiduo.quote.quote.service.InquiryCommandService;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "询价单", description = "询价单管理")
@RestController
@RequestMapping("/inquiries")
public class InquiryController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final InquiryCommandService inquiryCommandService;

  public InquiryController(
      AuthService authService,
      PermissionService permissionService,
      InquiryCommandService inquiryCommandService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.inquiryCommandService = inquiryCommandService;
  }

  @Operation(
      summary = "询价单列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<PageResult<InquiryListItem>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String inquiryNo,
      @RequestParam(required = false) String customerName,
      @RequestParam(required = false) String por,
      @RequestParam(required = false) String pol,
      @RequestParam(required = false) String pod,
      @RequestParam(required = false) String zipCode,
      @RequestParam(required = false) String city,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String pickUpAddress,
      @RequestParam(required = false) String fumigationPoint,
      @RequestParam(required = false) Long createdBy,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    SysUser user = authService.requireUser(authorization);
    requireView(user);
    return ApiResponse.ok(
        inquiryCommandService.list(
            status,
            inquiryNo,
            customerName,
            por,
            pol,
            pod,
            zipCode,
            city,
            state,
            pickUpAddress,
            fumigationPoint,
            createdBy,
            page,
            pageSize));
  }

  @Operation(
      summary = "询价单详情",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}")
  public ApiResponse<InquiryDetailResponse> detail(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireView(user);
    return ApiResponse.ok(inquiryCommandService.getById(id));
  }

  @Operation(
      summary = "新建询价单",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<InquiryDetailResponse> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody InquirySaveRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireCreate(user);
    return ApiResponse.ok(inquiryCommandService.create(user, request));
  }

  @Operation(
      summary = "更新询价单",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<InquiryDetailResponse> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody InquirySaveRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireEdit(user);
    return ApiResponse.ok(inquiryCommandService.update(user, id, request));
  }

  @Operation(
      summary = "递交询价单（生成报价单）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/submit")
  public ApiResponse<InquiryDetailResponse> submit(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireSubmit(user);
    return ApiResponse.ok(inquiryCommandService.submit(user, id));
  }

  @Operation(
      summary = "删除询价单",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    requireDelete(user);
    inquiryCommandService.delete(user, id);
    return ApiResponse.ok(null);
  }

  private void requireView(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.INQUIRY_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "缺少询价单查看权限");
    }
  }

  private void requireCreate(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.INQUIRY_CREATE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "缺少询价单新建权限");
    }
  }

  private void requireEdit(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.INQUIRY_EDIT)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "缺少询价单编辑权限");
    }
  }

  private void requireSubmit(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.INQUIRY_SUBMIT)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "缺少询价单递交权限");
    }
  }

  private void requireDelete(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.INQUIRY_DELETE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "缺少询价单删除权限");
    }
  }
}
