package com.furuiduo.quote.quote.controller;

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
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.auth.AuthService;
import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.cost.dto.CostTableTemplateResponse;
import com.furuiduo.quote.cost.dto.CostTableTemplateSaveRequest;
import com.furuiduo.quote.cost.dto.TemplateExcelFile;
import com.furuiduo.quote.cost.entity.CostGridTemplate;
import com.furuiduo.quote.cost.service.CostGridTemplateService;
import com.furuiduo.quote.sys.QuoteLibraryTemplatePermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "报价库-表格模板", description = "报价库列表视图模板，与成本库模板不共用")
@RestController
@RequestMapping("/quote-library/templates")
public class QuoteLibraryTemplateController {

  private static final String LIBRARY = CostGridTemplate.LIBRARY_QUOTE;

  private final AuthService authService;
  private final PermissionService permissionService;
  private final CostGridTemplateService templateService;

  public QuoteLibraryTemplateController(
      AuthService authService,
      PermissionService permissionService,
      CostGridTemplateService templateService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.templateService = templateService;
  }

  @Operation(
      summary = "按模式列出报价库表格模板",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<List<CostTableTemplateResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam String mode) {
    String normalized = templateService.normalizeMode(mode);
    requireView(authService.requireUser(authorization), normalized);
    return ApiResponse.ok(templateService.listByLibrary(LIBRARY, normalized));
  }

  @Operation(
      summary = "报价库表格模板详情",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}")
  public ApiResponse<CostTableTemplateResponse> get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    SysUser user = authService.requireUser(authorization);
    CostTableTemplateResponse template = templateService.getById(LIBRARY, id);
    requireView(user, template.mode());
    return ApiResponse.ok(template);
  }

  @Operation(
      summary = "新建报价库表格模板",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping
  public ApiResponse<CostTableTemplateResponse> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody CostTableTemplateSaveRequest request) {
    String mode = templateService.normalizeMode(request.mode());
    requireEdit(authService.requireUser(authorization), mode);
    return ApiResponse.ok(templateService.create(LIBRARY, request));
  }

  @Operation(
      summary = "更新报价库表格模板",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/{id}")
  public ApiResponse<CostTableTemplateResponse> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody CostTableTemplateSaveRequest request) {
    CostTableTemplateResponse existing = templateService.getById(LIBRARY, id);
    requireEdit(authService.requireUser(authorization), existing.mode());
    return ApiResponse.ok(templateService.update(LIBRARY, id, request));
  }

  @Operation(
      summary = "删除报价库表格模板",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/{id}")
  public ApiResponse<CostTableTemplateResponse> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    CostTableTemplateResponse existing = templateService.getById(LIBRARY, id);
    requireDelete(authService.requireUser(authorization), existing.mode());
    templateService.delete(LIBRARY, id);
    return ApiResponse.ok(existing);
  }

  @Operation(
      summary = "设为报价库默认模板",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/{id}/set-default")
  public ApiResponse<CostTableTemplateResponse> setDefault(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    CostTableTemplateResponse existing = templateService.getById(LIBRARY, id);
    requireEdit(authService.requireUser(authorization), existing.mode());
    return ApiResponse.ok(templateService.setDefault(LIBRARY, id));
  }

  @Operation(
      summary = "导出报价库表格模板 Excel",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/{id}/export")
  public ResponseEntity<byte[]> exportById(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    CostTableTemplateResponse existing = templateService.getById(LIBRARY, id);
    requireView(userOrForbidden(authorization), existing.mode());
    return excelResponse(templateService.exportById(LIBRARY, id));
  }

  @Operation(
      summary = "按布局预览导出报价库模板 Excel",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/export")
  public ResponseEntity<byte[]> exportPreview(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody CostTableTemplateSaveRequest request) {
    String mode = templateService.normalizeMode(request.mode());
    requireEdit(authService.requireUser(authorization), mode);
    return excelResponse(templateService.exportPreview(LIBRARY, request));
  }

  private SysUser userOrForbidden(String authorization) {
    return authService.requireUser(authorization);
  }

  private ResponseEntity<byte[]> excelResponse(TemplateExcelFile file) {
    String filename =
        URLEncoder.encode(file.filename(), StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(file.content());
  }

  private void requireView(SysUser user, String mode) {
    if (!permissionService.hasPermission(user, QuoteLibraryTemplatePermissionCodes.view(mode))
        && !permissionService.canViewQuoteLibrary(user, mode)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "缺少报价库模板查看权限");
    }
  }

  private void requireEdit(SysUser user, String mode) {
    if (!permissionService.hasPermission(user, QuoteLibraryTemplatePermissionCodes.edit(mode))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "缺少报价库模板编辑权限");
    }
  }

  private void requireDelete(SysUser user, String mode) {
    if (!permissionService.hasPermission(user, QuoteLibraryTemplatePermissionCodes.delete(mode))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "缺少报价库模板删除权限");
    }
  }
}
