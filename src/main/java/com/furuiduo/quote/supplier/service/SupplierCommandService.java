package com.furuiduo.quote.supplier.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.common.PartyMasterExcelSupport;
import com.furuiduo.quote.common.PartyMasterExcelSupport.StatusCell;
import com.furuiduo.quote.common.RequestIds;
import com.furuiduo.quote.common.SearchText;
import com.furuiduo.quote.cost.dto.CostImportResult;
import com.furuiduo.quote.cost.support.CostExcelSupport;
import com.furuiduo.quote.supplier.dto.SupplierResponse;
import com.furuiduo.quote.supplier.dto.SupplierSaveRequest;
import com.furuiduo.quote.supplier.entity.Supplier;
import com.furuiduo.quote.supplier.repository.SupplierRepository;
import com.furuiduo.quote.supplier.support.SupplierCodeGenerator;
import com.furuiduo.quote.supplier.support.SupplierTypes;
import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class SupplierCommandService {

  private static final String[] EXPORT_HEADERS = {
    "编码",
    "名称",
    "类型",
    "邮箱",
    "备注",
    "非熏蒸打包价公式",
    "熏蒸打包价（非橡木）公式",
    "熏蒸打包价（橡木）公式",
    "状态"
  };

  private final SupplierRepository supplierRepository;
  private final SupplierCodeGenerator supplierCodeGenerator;

  public SupplierCommandService(
      SupplierRepository supplierRepository, SupplierCodeGenerator supplierCodeGenerator) {
    this.supplierRepository = supplierRepository;
    this.supplierCodeGenerator = supplierCodeGenerator;
  }

  @Transactional
  public SupplierResponse create(SysUser user, SupplierSaveRequest request) {
    validateSaveRequest(request, null);
    Supplier supplier = new Supplier();
    supplier.setCode(supplierCodeGenerator.next());
    supplier.setCreatedBy(user.getId());
    supplier.setCreatedByName(user.getRealName());
    supplier.setDeptId(user.getDepartment() != null ? user.getDepartment().getId() : null);
    apply(supplier, request);
    return SupplierResponse.from(supplierRepository.save(supplier));
  }

  @Transactional
  public SupplierResponse update(Long id, SupplierSaveRequest request) {
    validateSaveRequest(request, id);
    Supplier supplier =
        supplierRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "供应商不存在"));
    apply(supplier, request);
    supplier.setUpdatedAt(LocalDateTime.now());
    return SupplierResponse.from(supplierRepository.save(supplier));
  }

  @Transactional
  public void delete(Long id) {
    Supplier supplier =
        supplierRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "供应商不存在"));
    supplierRepository.delete(supplier);
  }

  public SupplierResponse getById(Long id) {
    return supplierRepository
        .findById(id)
        .map(SupplierResponse::from)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "供应商不存在"));
  }

  @Transactional
  public CostImportResult importExcel(SysUser user, MultipartFile file) throws IOException {
    Set<String> seenNames = new HashSet<>();
    return CostExcelSupport.importRows(
        file,
        EXPORT_HEADERS,
        this::mapImportRow,
        (row) -> validateImportRow(row, seenNames),
        (rowNum, row) -> upsertImported(user, row));
  }

  @Transactional(readOnly = true)
  public byte[] exportExcel(String code, String name, Integer status, List<Long> ids) {
    List<Supplier> items;
    if (RequestIds.present(ids)) {
      items =
          supplierRepository.findAllById(ids).stream()
              .sorted(
                  Comparator.comparing(
                      Supplier::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
              .toList();
    } else {
      var pageable =
          PageRequest.of(0, 10_000, Sort.by(Sort.Direction.DESC, "updatedAt"));
      items =
          supplierRepository
              .search(SearchText.orEmpty(code), SearchText.orEmpty(name), status, pageable)
              .getContent();
    }
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Suppliers");
      CostExcelSupport.writeHeaderRow(sheet, EXPORT_HEADERS);
      int rowIndex = 1;
      for (Supplier item : items) {
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getCode()));
        row.createCell(1).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getName()));
        row.createCell(2).setCellValue(SupplierTypes.toExcelValue(item.getTypes()));
        row.createCell(3).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getEmail()));
        row.createCell(4).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getRemark()));
        row.createCell(5)
            .setCellValue(
                PartyMasterExcelSupport.nullToEmpty(item.getNonFumigationPackageFormula()));
        row.createCell(6)
            .setCellValue(
                PartyMasterExcelSupport.nullToEmpty(item.getFumigationNonOakPackageFormula()));
        row.createCell(7)
            .setCellValue(
                PartyMasterExcelSupport.nullToEmpty(item.getFumigationOakPackageFormula()));
        row.createCell(8).setCellValue(PartyMasterExcelSupport.statusLabel(item.getStatus()));
      }
      return CostExcelSupport.writeWorkbook(workbook);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to export suppliers", ex);
    }
  }

  private void upsertImported(SysUser user, ImportRow row) {
    if (row.typeError() != null) {
      throw new IllegalArgumentException(row.typeError());
    }
    String name = PartyMasterExcelSupport.normalizeName(row.name());
    Supplier supplier = resolveForImport(row.code(), name);
    boolean creating = supplier.getId() == null;
    if (creating) {
      assertNameAvailable(name, null);
      supplier.setCode(supplierCodeGenerator.next());
      supplier.setCreatedBy(user.getId());
      supplier.setCreatedByName(user.getRealName());
      supplier.setDeptId(user.getDepartment() != null ? user.getDepartment().getId() : null);
      supplier.setTypes(row.types() == null ? List.of() : new ArrayList<>(row.types()));
    } else {
      assertNameAvailable(name, supplier.getId());
      if (row.typesProvided()) {
        supplier.setTypes(row.types() == null ? List.of() : new ArrayList<>(row.types()));
      }
    }
    supplier.setName(name);
    supplier.setEmail(PartyMasterExcelSupport.trimToNull(row.email()));
    supplier.setRemark(PartyMasterExcelSupport.trimToNull(row.remark()));
    supplier.setNonFumigationPackageFormula(
        PartyMasterExcelSupport.trimToNull(row.nonFumigationPackageFormula()));
    supplier.setFumigationNonOakPackageFormula(
        PartyMasterExcelSupport.trimToNull(row.fumigationNonOakPackageFormula()));
    supplier.setFumigationOakPackageFormula(
        PartyMasterExcelSupport.trimToNull(row.fumigationOakPackageFormula()));
    supplier.setStatus(
        PartyMasterExcelSupport.resolveStatus(
            row.status(), creating ? null : supplier.getStatus()));
    supplier.setUpdatedAt(LocalDateTime.now());
    supplierRepository.save(supplier);
  }

  /** 仅当编码命中时更新；名称已存在且编码未命中则拒绝。 */
  private Supplier resolveForImport(String code, String name) {
    if (code != null && !code.isBlank()) {
      Supplier byCode = supplierRepository.findByCode(code.trim()).orElse(null);
      if (byCode != null) {
        return byCode;
      }
    }
    if (supplierRepository.existsByNameNormalized(name, null)) {
      throw new IllegalArgumentException("供应商名称已存在：" + name + "（如需更新请填写正确编码）");
    }
    return new Supplier();
  }

  private ImportRow mapImportRow(Row row) {
    Map<String, Integer> headers = CostExcelSupport.readHeaderMap(row.getSheet().getRow(0));
    String code = CostExcelSupport.readByHeader(row, headers, "编码", "Code");
    String name = CostExcelSupport.readByHeader(row, headers, "名称", "Name", "供应商名称");
    boolean typesHeaderPresent =
        CostExcelSupport.findColumn(headers, "类型", "Type", "Types") >= 0;
    String typesRaw = CostExcelSupport.readByHeader(row, headers, "类型", "Type", "Types");
    String email = CostExcelSupport.readByHeader(row, headers, "邮箱", "Email");
    String remark = CostExcelSupport.readByHeader(row, headers, "备注", "Remark");
    String nonFumigation =
        CostExcelSupport.readByHeader(
            row, headers, "非熏蒸打包价公式", "Non-Fumigation Package Formula");
    String fumigationNonOak =
        CostExcelSupport.readByHeader(
            row, headers, "熏蒸打包价（非橡木）公式", "Fumigation Package Formula (Non-Oak)");
    String fumigationOak =
        CostExcelSupport.readByHeader(
            row, headers, "熏蒸打包价（橡木）公式", "Fumigation Package Formula (Oak)");
    String statusRaw = CostExcelSupport.readByHeader(row, headers, "状态", "Status");
    if (code.isBlank()
        && name.isBlank()
        && typesRaw.isBlank()
        && email.isBlank()
        && remark.isBlank()
        && nonFumigation.isBlank()
        && fumigationNonOak.isBlank()
        && fumigationOak.isBlank()
        && statusRaw.isBlank()) {
      return null;
    }
    List<String> types = List.of();
    String typeError = null;
    boolean typesProvided = typesHeaderPresent;
    if (typesHeaderPresent) {
      try {
        types = SupplierTypes.parseExcelValue(typesRaw);
      } catch (IllegalArgumentException ex) {
        typeError = ex.getMessage();
        types = null;
      }
    }
    return new ImportRow(
        code,
        name,
        types,
        typesProvided,
        email,
        remark,
        nonFumigation,
        fumigationNonOak,
        fumigationOak,
        PartyMasterExcelSupport.parseStatusCell(statusRaw),
        typeError);
  }

  private String validateImportRow(ImportRow row, Set<String> seenNames) {
    if (row.typeError() != null) {
      return row.typeError();
    }
    if (row.name() == null || row.name().isBlank()) {
      return "名称不能为空";
    }
    if (row.status() != null && row.status().isUnrecognized()) {
      return "状态无效（请填启用/停用或 1/0）";
    }
    String key = PartyMasterExcelSupport.nameKey(row.name());
    if (!seenNames.add(key)) {
      return "名称与文件中其他行重复";
    }
    return null;
  }

  private void validateSaveRequest(SupplierSaveRequest request, Long excludeId) {
    if (request.name() == null || request.name().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "供应商名称不能为空");
    }
    if (request.status() == null || (request.status() != 0 && request.status() != 1)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的供应商状态");
    }
    try {
      SupplierTypes.normalize(request.types());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
    assertNameAvailable(PartyMasterExcelSupport.normalizeName(request.name()), excludeId);
  }

  private void assertNameAvailable(String name, Long excludeId) {
    if (supplierRepository.existsByNameNormalized(name, excludeId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "供应商名称已存在：" + name);
    }
  }

  private void apply(Supplier supplier, SupplierSaveRequest request) {
    supplier.setName(PartyMasterExcelSupport.normalizeName(request.name()));
    supplier.setTypes(SupplierTypes.normalize(request.types()));
    supplier.setEmail(PartyMasterExcelSupport.trimToNull(request.email()));
    supplier.setRemark(PartyMasterExcelSupport.trimToNull(request.remark()));
    supplier.setNonFumigationPackageFormula(
        PartyMasterExcelSupport.trimToNull(request.nonFumigationPackageFormula()));
    supplier.setFumigationNonOakPackageFormula(
        PartyMasterExcelSupport.trimToNull(request.fumigationNonOakPackageFormula()));
    supplier.setFumigationOakPackageFormula(
        PartyMasterExcelSupport.trimToNull(request.fumigationOakPackageFormula()));
    supplier.setStatus(request.status());
    supplier.setUpdatedAt(LocalDateTime.now());
  }

  private record ImportRow(
      String code,
      String name,
      List<String> types,
      boolean typesProvided,
      String email,
      String remark,
      String nonFumigationPackageFormula,
      String fumigationNonOakPackageFormula,
      String fumigationOakPackageFormula,
      StatusCell status,
      String typeError) {}
}
