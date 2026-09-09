package com.furuiduo.quote.quoterule.controller;

import java.util.List;

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
import com.furuiduo.quote.common.BatchIdsRequest;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.quoterule.dto.QuoteRuleResponse;
import com.furuiduo.quote.quoterule.dto.QuoteRuleSaveRequest;
import com.furuiduo.quote.quoterule.service.QuoteRuleCommandService;
import com.furuiduo.quote.quoterule.service.QuoteRuleQueryService;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "报价单规则", description = "报价单费用计算规则")
@RestController
@RequestMapping("/quote-rules")
public class QuoteRuleController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final QuoteRuleQueryService quoteRuleQueryService;
  private final QuoteRuleCommandService quoteRuleCommandService;

  public QuoteRuleController(
      AuthService authService,
      PermissionService permissionService,
      QuoteRuleQueryService quoteRuleQueryService,
      QuoteRuleCommandService quoteRuleCommandService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.quoteRuleQueryService = quoteRuleQueryService;
    this.quoteRuleCommandService = quoteRuleCommandService;
  }

  @Operation(
      summary = "规则列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<List<QuoteRuleResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String targetField,
      @RequestParam(required = false) Integer status) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(quoteRuleQueryService.list(name, targetField, status));
  }

  @Operation(
      summary = "规则详情",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}")
  public ApiResponse<QuoteRuleResponse> get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(quoteRuleCommandService.getById(id));
  }

  @Operation(
      summary = "新建规则",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<QuoteRuleResponse> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody QuoteRuleSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(quoteRuleCommandService.create(request));
  }

  @Operation(
      summary = "更新规则",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<QuoteRuleResponse> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody QuoteRuleSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(quoteRuleCommandService.update(id, request));
  }

  @Operation(
      summary = "删除规则",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireManage(authService.requireUser(authorization));
    quoteRuleCommandService.delete(id);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "批量删除规则",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/batch-delete")
  public ApiResponse<Void> batchDelete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody BatchIdsRequest request) {
    requireManage(authService.requireUser(authorization));
    quoteRuleCommandService.batchDelete(request.ids() == null ? List.of() : request.ids());
    return ApiResponse.ok(null);
  }

  private void requireView(SysUser user) {
    if (permissionService.hasPermission(user, PermissionCodes.MD_QUOTE_RULE_VIEW)
        || permissionService.hasPermission(user, PermissionCodes.QUOTE_VIEW)) {
      return;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
  }

  private void requireManage(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.MD_QUOTE_RULE_MANAGE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }
}
