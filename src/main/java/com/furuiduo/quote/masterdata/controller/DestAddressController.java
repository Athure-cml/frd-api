package com.furuiduo.quote.masterdata.controller;

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

import com.furuiduo.quote.auth.AuthService;
import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.cost.dto.CostImportResult;
import com.furuiduo.quote.masterdata.dto.DestAddressRowResponse;
import com.furuiduo.quote.masterdata.dto.DestAddressTreeNodeResponse;
import com.furuiduo.quote.masterdata.dto.DestCityResponse;
import com.furuiduo.quote.masterdata.dto.DestCitySaveRequest;
import com.furuiduo.quote.masterdata.dto.DestZipResponse;
import com.furuiduo.quote.masterdata.dto.DestZipSaveRequest;
import com.furuiduo.quote.masterdata.service.DestAddressService;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "美国州邮政编码", description = "美国州/城市/邮编主数据")
@RestController
@RequestMapping("/dest-addresses")
public class DestAddressController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final DestAddressService destAddressService;

  public DestAddressController(
      AuthService authService,
      PermissionService permissionService,
      DestAddressService destAddressService) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.destAddressService = destAddressService;
  }

  @Operation(
      summary = "美国州邮政编码分页列表",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping
  public ApiResponse<PageResult<DestAddressRowResponse>> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String stateCode,
      @RequestParam(required = false) String keyword) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(destAddressService.list(page, pageSize, stateCode, keyword));
  }

  @Operation(
      summary = "美国州邮政编码树",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/tree")
  public ApiResponse<List<DestAddressTreeNodeResponse>> tree(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(destAddressService.tree());
  }

  @Operation(
      summary = "目的地址州节点（懒加载根）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/tree/states")
  public ApiResponse<List<DestAddressTreeNodeResponse>> listStateNodes(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String stateCode,
      @RequestParam(required = false) String keyword) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(destAddressService.listStateNodes(stateCode, keyword));
  }

  @Operation(
      summary = "目的地址城市节点（懒加载）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/tree/states/{stateId}/cities")
  public ApiResponse<List<DestAddressTreeNodeResponse>> listCityNodes(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long stateId,
      @RequestParam(required = false) String keyword) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(destAddressService.listCityNodes(stateId, keyword));
  }

  @Operation(
      summary = "目的地址邮编节点（懒加载）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/tree/cities/{cityId}/zips")
  public ApiResponse<List<DestAddressTreeNodeResponse>> listZipNodes(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long cityId,
      @RequestParam(required = false) String keyword) {
    requireView(authService.requireUser(authorization));
    return ApiResponse.ok(destAddressService.listZipNodes(cityId, keyword));
  }

  @Operation(
      summary = "新建城市",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/cities")
  public ApiResponse<DestCityResponse> createCity(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody DestCitySaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(destAddressService.createCity(request));
  }

  @Operation(
      summary = "更新城市",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/cities/{id}")
  public ApiResponse<DestCityResponse> updateCity(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody DestCitySaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(destAddressService.updateCity(id, request));
  }

  @Operation(
      summary = "删除城市",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/cities/{id}")
  public ApiResponse<Void> deleteCity(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireManage(authService.requireUser(authorization));
    destAddressService.deleteCity(id);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "新建邮编",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/zips")
  public ApiResponse<DestZipResponse> createZip(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody DestZipSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(destAddressService.createZip(request));
  }

  @Operation(
      summary = "更新邮编",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PutMapping("/zips/{id}")
  public ApiResponse<DestZipResponse> updateZip(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @RequestBody DestZipSaveRequest request) {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(destAddressService.updateZip(id, request));
  }

  @Operation(
      summary = "删除邮编",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @DeleteMapping("/zips/{id}")
  public ApiResponse<Void> deleteZip(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    requireManage(authService.requireUser(authorization));
    destAddressService.deleteZip(id);
    return ApiResponse.ok(null);
  }

  @Operation(
      summary = "导入美国州邮政编码",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<CostImportResult> importExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam("file") MultipartFile file)
      throws IOException {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(destAddressService.importExcel(file));
  }

  @Operation(
      summary = "从 GeoNames US.txt 导入美国州邮政编码",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping(value = "/import-geonames", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<CostImportResult> importGeonames(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam("file") MultipartFile file)
      throws IOException {
    requireManage(authService.requireUser(authorization));
    return ApiResponse.ok(destAddressService.importGeonames(file));
  }

  @Operation(
      summary = "导出美国州邮政编码",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/export")
  public ResponseEntity<byte[]> exportExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    requireView(authService.requireUser(authorization));
    byte[] bytes = destAddressService.exportExcel();
    String filename =
        URLEncoder.encode("us-state-zip.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }

  private void requireView(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.MD_DEST_ADDRESS_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireManage(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.MD_DEST_ADDRESS_MANAGE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }
}
