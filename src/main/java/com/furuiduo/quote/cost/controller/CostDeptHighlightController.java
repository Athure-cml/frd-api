package com.furuiduo.quote.cost.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.auth.AuthService;
import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.cost.dto.CostHighlightBatchRequest;
import com.furuiduo.quote.cost.dto.CostHighlightIdsRequest;
import com.furuiduo.quote.cost.entity.CostHighlightMode;
import com.furuiduo.quote.cost.service.CostDeptHighlightService;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "成本库-常用标记", description = "部门共享成本常用行标记")
@RestController
@RequestMapping("/cost-library/highlights")
public class CostDeptHighlightController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final CostDeptHighlightService highlightService;

  public CostDeptHighlightController(
      AuthService authService,
      PermissionService permissionService,
      CostDeptHighlightService highlightService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.highlightService = highlightService;
  }

  @Operation(
      summary = "批量标记常用",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{mode}")
  public ApiResponse<Map<String, Integer>> mark(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable String mode,
      @Validated @RequestBody CostHighlightBatchRequest request) {
    SysUser user = authService.requireUser(authorization);
    CostHighlightMode costMode = parseMode(mode);
    requireView(user, costMode);
    int updated = highlightService.mark(costMode, user, request);
    return ApiResponse.ok(Map.of("updated", updated));
  }

  @Operation(
      summary = "批量取消常用",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{mode}")
  public ApiResponse<Map<String, Integer>> unmark(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable String mode,
      @Validated @RequestBody CostHighlightIdsRequest request) {
    SysUser user = authService.requireUser(authorization);
    CostHighlightMode costMode = parseMode(mode);
    requireView(user, costMode);
    int removed = highlightService.unmark(costMode, user, request.ids());
    return ApiResponse.ok(Map.of("removed", removed));
  }

  private CostHighlightMode parseMode(String raw) {
    try {
      return CostHighlightMode.parse(raw);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的成本库类型");
    }
  }

  private void requireView(SysUser user, CostHighlightMode mode) {
    String code =
        switch (mode) {
          case road -> PermissionCodes.COST_ROAD_VIEW;
          case sea -> PermissionCodes.COST_SEA_VIEW;
          case fumigation -> PermissionCodes.COST_FUMIGATION_VIEW;
        };
    if (!permissionService.hasPermission(user, code)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }
}
