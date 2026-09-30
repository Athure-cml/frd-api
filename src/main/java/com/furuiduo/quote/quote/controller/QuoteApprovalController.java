package com.furuiduo.quote.quote.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.auth.AuthService;
import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.quote.dto.QuoteApprovalDetailResponse;
import com.furuiduo.quote.quote.dto.QuoteApprovalListItem;
import com.furuiduo.quote.quote.service.QuoteApprovalService;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "报价审批", description = "报价审批管理")
@RestController
@RequestMapping("/approvals/quotes")
public class QuoteApprovalController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final QuoteApprovalService quoteApprovalService;

  public QuoteApprovalController(
      AuthService authService,
      PermissionService permissionService,
      QuoteApprovalService quoteApprovalService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.quoteApprovalService = quoteApprovalService;
  }

  @Operation(
      summary = "报价审批分页列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<PageResult<QuoteApprovalListItem>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String approvalNo,
      @RequestParam(required = false) String status) {
    SysUser user = authService.requireUser(authorization);
    requireApprove(user);
    return ApiResponse.ok(
        quoteApprovalService.list(user, page, pageSize, approvalNo, status));
  }

  @Operation(
      summary = "报价审批详情",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}")
  public ApiResponse<QuoteApprovalDetailResponse> detail(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestParam(required = false) Integer cycle) {
    SysUser user = authService.requireUser(authorization);
    requireApprove(user);
    return ApiResponse.ok(quoteApprovalService.getDetail(user, id, cycle));
  }

  private void requireApprove(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.QUOTE_APPROVE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无审批权限");
    }
  }
}
