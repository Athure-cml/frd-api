package com.furuiduo.quote.cost.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

import com.furuiduo.quote.auth.AuthService;
import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.cost.dto.CostBatchDeleteRequest;
import com.furuiduo.quote.cost.dto.CostBatchUpdateRequest;
import com.furuiduo.quote.cost.dto.CostImportResult;
import com.furuiduo.quote.cost.dto.FumigationCostResponse;
import com.furuiduo.quote.cost.dto.FumigationCostSaveRequest;
import com.furuiduo.quote.cost.service.CostFumigationService;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "成本库-熏蒸", description = "熏蒸成本")
@RestController
@RequestMapping("/cost-library/fumigation")
public class CostFumigationController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final CostFumigationService costFumigationService;

  public CostFumigationController(
      AuthService authService,
      PermissionService permissionService,
      CostFumigationService costFumigationService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.costFumigationService = costFumigationService;
  }

  @GetMapping
  public ApiResponse<PageResult<FumigationCostResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String port,
      @RequestParam(required = false) String station) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(costFumigationService.list(page, pageSize, port, station));
  }

  @GetMapping("/{id}")
  public ApiResponse<FumigationCostResponse> get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(costFumigationService.getById(id));
  }

  @PostMapping
  public ApiResponse<FumigationCostResponse> create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody FumigationCostSaveRequest request) {
    requireEdit(authService.requireUser(authorization));
    return ApiResponse.ok(costFumigationService.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<FumigationCostResponse> update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody FumigationCostSaveRequest request) {
    requireEdit(authService.requireUser(authorization));
    return ApiResponse.ok(costFumigationService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireEdit(authService.requireUser(authorization));
    costFumigationService.delete(id);
    return ApiResponse.ok(null);
  }

  @PostMapping("/batch-delete")
  public ApiResponse<Void> batchDelete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody CostBatchDeleteRequest request) {
    requireEdit(authService.requireUser(authorization));
    costFumigationService.batchDelete(request);
    return ApiResponse.ok(null);
  }

  @PatchMapping("/batch")
  public ApiResponse<Map<String, Integer>> batchUpdate(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody CostBatchUpdateRequest request) {
    requireEdit(authService.requireUser(authorization));
    return ApiResponse.ok(Map.of("updated", costFumigationService.batchUpdate(request)));
  }

  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<CostImportResult> importExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam("file") MultipartFile file)
      throws IOException {
    requireEdit(authService.requireUser(authorization));
    return ApiResponse.ok(costFumigationService.importExcel(file));
  }

  @GetMapping("/export")
  public ResponseEntity<byte[]> exportExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String port,
      @RequestParam(required = false) String station,
      @RequestParam(required = false) Long templateId) {
    requireView(authService.requireUser(authorization));
    byte[] bytes = costFumigationService.exportExcel(port, station, templateId);
    String filename =
        URLEncoder.encode("fumigation-cost.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }

  private void requireView(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.COST_FUMIGATION_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireEdit(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.COST_FUMIGATION_EDIT)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }
}
