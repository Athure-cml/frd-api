package com.furuiduo.quote.agent.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.agent.dto.AgentResponse;
import com.furuiduo.quote.agent.dto.AgentSaveRequest;
import com.furuiduo.quote.agent.service.AgentCommandService;
import com.furuiduo.quote.agent.service.AgentQueryService;
import com.furuiduo.quote.auth.AuthService;
import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.common.BatchIdsRequest;
import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.common.ReorderRequest;
import com.furuiduo.quote.common.RequestIds;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.cost.dto.CostImportResult;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "代理商", description = "代理商主数据")
@RestController
@RequestMapping("/agents")
public class AgentController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final AgentQueryService queryService;
  private final AgentCommandService commandService;

  public AgentController(
      AuthService authService,
      PermissionService permissionService,
      AgentQueryService queryService,
      AgentCommandService commandService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.queryService = queryService;
    this.commandService = commandService;
  }

  @Operation(
      summary = "代理商分页列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<PageResult<AgentResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Integer status) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(queryService.list(page, pageSize, code, name, status));
  }

  @Operation(
      summary = "代理商详情",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}")
  public ApiResponse<AgentResponse> get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(commandService.getById(id));
  }

  @Operation(
      summary = "新建代理商",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<AgentResponse> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody AgentSaveRequest request) {
    SysUser user = authService.requireUser(authorization);
    requireCreate(user);
    return ApiResponse.ok(commandService.create(user, request));
  }

  @Operation(
      summary = "更新代理商",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<AgentResponse> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody AgentSaveRequest request) {
    requireEdit(authService.requireUser(authorization));
    return ApiResponse.ok(commandService.update(id, request));
  }

  @Operation(
      summary = "置顶代理商",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/pin")
  public ApiResponse<AgentResponse> pin(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireEdit(authService.requireUser(authorization));
    return ApiResponse.ok(commandService.setPinned(id, true));
  }

  @Operation(
      summary = "取消置顶代理商",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/unpin")
  public ApiResponse<AgentResponse> unpin(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireEdit(authService.requireUser(authorization));
    return ApiResponse.ok(commandService.setPinned(id, false));
  }

  @Operation(
      summary = "拖拽排序",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/reorder")
  public ApiResponse<Void> reorder(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody ReorderRequest request) {
    requireEdit(authService.requireUser(authorization));
    commandService.reorder(request.ids() == null ? List.of() : request.ids());
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "删除代理商",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireDelete(authService.requireUser(authorization));
    commandService.delete(id);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "批量删除代理商",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/batch-delete")
  public ApiResponse<Void> batchDelete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody BatchIdsRequest request) {
    requireDelete(authService.requireUser(authorization));
    commandService.batchDelete(request.ids() == null ? List.of() : request.ids());
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "导入代理商",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<CostImportResult> importExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam("file") MultipartFile file,
      @RequestParam(required = false, defaultValue = "false") boolean dryRun)
      throws IOException {
    SysUser user = authService.requireUser(authorization);
    requireCreate(user);
    return ApiResponse.ok(commandService.importExcel(user, file, dryRun));
  }

  @Operation(
      summary = "导出代理商",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/export")
  public ResponseEntity<byte[]> exportExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Integer status,
      @RequestParam(required = false) String ids) {
    requireView(authService.requireUser(authorization));
    byte[] bytes = commandService.exportExcel(code, name, status, RequestIds.parse(ids));
    String filename =
        URLEncoder.encode("代理商.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }

  private void requireView(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.AGENT_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireCreate(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.AGENT_CREATE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireEdit(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.AGENT_EDIT)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireDelete(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.AGENT_DELETE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }
}
