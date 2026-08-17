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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.common.PartyMasterExcelSupport;
import com.furuiduo.quote.common.PartyMasterExcelSupport.StatusCell;
import com.furuiduo.quote.common.PartyReorderSupport;
import com.furuiduo.quote.common.RequestIds;
import com.furuiduo.quote.common.SearchText;
import com.furuiduo.quote.cost.dto.CostImportResult;
import com.furuiduo.quote.cost.support.CostExcelSupport;
import com.furuiduo.quote.supplier.dto.SupplierResponse;
import com.furuiduo.quote.supplier.dto.SupplierSaveRequest;
import com.furuiduo.quote.supplier.entity.Supplier;
import com.furuiduo.quote.supplier.repository.SupplierRepository;
import com.furuiduo.quote.supplier.repository.SupplierTypeRepository;
import com.furuiduo.quote.supplier.support.SupplierCategories;
import com.furuiduo.quote.supplier.support.SupplierCodeGenerator;
import com.furuiduo.quote.supplier.support.SupplierOtherTypes;
import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class SupplierCommandService {

  private final SupplierRepository supplierRepository;
  private final SupplierTypeRepository supplierTypeRepository;
  private final SupplierCodeGenerator supplierCodeGenerator;

  public SupplierCommandService(
      SupplierRepository supplierRepository,
      SupplierTypeRepository supplierTypeRepository,
      SupplierCodeGenerator supplierCodeGenerator) {
    this.supplierRepository = supplierRepository;
    this.supplierTypeRepository = supplierTypeRepository;
    this.supplierCodeGenerator = supplierCodeGenerator;
  }

  @Transactional
  public SupplierResponse create(SysUser user, SupplierSaveRequest request) {
    String category = resolveCategory(request.category());
    validateSaveRequest(request, category, null);
    Supplier supplier = new Supplier();
    supplier.setCode(supplierCodeGenerator.next());
    supplier.setCategory(category);
    supplier.setCreatedBy(user.getId());
    supplier.setCreatedByName(user.getRealName());
    supplier.setDeptId(user.getDepartment() != null ? user.getDepartment().getId() : null);
    apply(supplier, request, category);
    return SupplierResponse.from(supplierRepository.save(supplier));
  }

  @Transactional
  public SupplierResponse update(Long id, SupplierSaveRequest request) {
    Supplier supplier =
        supplierRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "供应商不存在"));
    String category = supplier.getCategory();
    validateSaveRequest(request, category, id);
    apply(supplier, request, category);
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

  @Transactional
  public void batchDelete(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    for (Long id : ids) {
      delete(id);
    }
  }

  public SupplierResponse getById(Long id) {
    return supplierRepository
        .findById(id)
        .map(SupplierResponse::from)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "供应商不存在"));
  }

  @Transactional
  public SupplierResponse setPinned(Long id, boolean pinned) {
    Supplier supplier =
        supplierRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "供应商不存在"));
    String category = supplier.getCategory();
    if (pinned) {
      supplier.setPinnedAt(LocalDateTime.now());
      supplier.setSortOrder(supplierRepository.minPinnedSortOrder(category) - 1);
    } else {
      supplier.setPinnedAt(null);
      supplier.setSortOrder(supplierRepository.maxUnpinnedSortOrder(category) + 1);
    }
    return SupplierResponse.from(supplierRepository.save(supplier));
  }

  @Transactional
  public void reorder(List<Long> orderedIds) {
    if (orderedIds == null || orderedIds.isEmpty()) {
      return;
    }
    Long firstId =
        orderedIds.stream().filter(id -> id != null).findFirst().orElse(null);
    if (firstId == null) {
      return;
    }
    Supplier anchor =
        supplierRepository
            .findById(firstId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "供应商不存在"));
    String category = anchor.getCategory();
    PartyReorderSupport.reorder(
        orderedIds,
        id -> supplierRepository.findById(id).orElse(null),
        Supplier::getId,
        e -> e.getPinnedAt() != null,
        pinned -> supplierRepository.findAllByCategoryAndPinned(category, pinned),
        Supplier::setSortOrder,
        supplierRepository::saveAll);
  }

  @Transactional
  public CostImportResult importExcel(SysUser user, MultipartFile file, String category)
      throws IOException {
    String normalized = resolveCategory(category);
    Set<String> seenNames = new HashSet<>();
    return CostExcelSupport.importRows(
        file,
        exportHeaders(normalized),
        (row) -> mapImportRow(row, normalized),
        (row) -> validateImportRow(row, seenNames),
        (rowNum, row) -> upsertImported(user, row, normalized));
  }

  @Transactional(readOnly = true)
  public byte[] exportExcel(
      String category, String code, String name, Integer status, String typeId, List<Long> ids) {
    String normalized = resolveCategory(category);
    List<Supplier> items;
    if (RequestIds.present(ids)) {
      items =
          supplierRepository.findAllById(ids).stream()
              .filter(item -> normalized.equals(item.getCategory()))
              .sorted(
                  Comparator.comparing(
                      Supplier::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
              .toList();
    } else {
      var pageable = PageRequest.of(0, 10_000);
      items =
          supplierRepository
              .search(
                  normalized,
                  SearchText.orEmpty(code),
                  SearchText.orEmpty(name),
                  status,
                  SearchText.orEmpty(typeId),
                  pageable)
              .getContent();
    }
    Map<Long, String> typeNames = SupplierOtherTypes.loadNameMap(supplierTypeRepository);
    boolean withTypes = SupplierCategories.supportsTypes(normalized);
    boolean withFormulas = SupplierCategories.supportsFormulas(normalized);
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Suppliers");
      CostExcelSupport.writeHeaderRow(sheet, exportHeaders(normalized));
      int rowIndex = 1;
      for (Supplier item : items) {
        Row row = sheet.createRow(rowIndex++);
        int col = 0;
        row.createCell(col++).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getCode()));
        row.createCell(col++).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getName()));
        row.createCell(col++).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getShortName()));
        if (withTypes) {
          row.createCell(col++)
              .setCellValue(SupplierOtherTypes.toExcelValue(item.getTypes(), typeNames));
        }
        row.createCell(col++)
            .setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getContactName()));
        row.createCell(col++).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getPhone()));
        row.createCell(col++).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getEmail()));
        row.createCell(col++).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getRemark()));
        if (withFormulas) {
          row.createCell(col++)
              .setCellValue(
                  PartyMasterExcelSupport.nullToEmpty(item.getNonFumigationPackageFormula()));
          row.createCell(col++)
              .setCellValue(
                  PartyMasterExcelSupport.nullToEmpty(item.getFumigationNonOakPackageFormula()));
          row.createCell(col++)
              .setCellValue(
                  PartyMasterExcelSupport.nullToEmpty(item.getFumigationOakPackageFormula()));
        }
        row.createCell(col).setCellValue(PartyMasterExcelSupport.statusLabel(item.getStatus()));
      }
      return CostExcelSupport.writeWorkbook(workbook);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to export suppliers", ex);
    }
  }

  private void upsertImported(SysUser user, ImportRow row, String category) {
    if (row.typeError() != null) {
      throw new IllegalArgumentException(row.typeError());
    }
    String name = PartyMasterExcelSupport.normalizeName(row.name());
    Supplier supplier = resolveForImport(row.code(), name, category);
    boolean creating = supplier.getId() == null;
    if (creating) {
      assertNameAvailable(category, name, null);
      supplier.setCode(supplierCodeGenerator.next());
      supplier.setCategory(category);
      supplier.setCreatedBy(user.getId());
      supplier.setCreatedByName(user.getRealName());
      supplier.setDeptId(user.getDepartment() != null ? user.getDepartment().getId() : null);
      supplier.setTypes(
          SupplierCategories.supportsTypes(category)
              ? (row.types() == null ? List.of() : new ArrayList<>(row.types()))
              : List.of());
    } else {
      if (!category.equals(supplier.getCategory())) {
        throw new IllegalArgumentException(
            "编码已存在于其他供应商分类，无法导入到"
                + SupplierCategories.displayName(category));
      }
      assertNameAvailable(category, name, supplier.getId());
      if (SupplierCategories.supportsTypes(category) && row.typesProvided()) {
        supplier.setTypes(row.types() == null ? List.of() : new ArrayList<>(row.types()));
      }
    }
    supplier.setName(name);
    supplier.setShortName(PartyMasterExcelSupport.trimToNull(row.shortName()));
    supplier.setContactName(PartyMasterExcelSupport.trimToNull(row.contactName()));
    supplier.setPhone(PartyMasterExcelSupport.trimToNull(row.phone()));
    supplier.setEmail(PartyMasterExcelSupport.trimToNull(row.email()));
    supplier.setRemark(PartyMasterExcelSupport.trimToNull(row.remark()));
    if (SupplierCategories.supportsFormulas(category)) {
      supplier.setNonFumigationPackageFormula(
          PartyMasterExcelSupport.trimToNull(row.nonFumigationPackageFormula()));
      supplier.setFumigationNonOakPackageFormula(
          PartyMasterExcelSupport.trimToNull(row.fumigationNonOakPackageFormula()));
      supplier.setFumigationOakPackageFormula(
          PartyMasterExcelSupport.trimToNull(row.fumigationOakPackageFormula()));
    } else {
      supplier.setNonFumigationPackageFormula(null);
      supplier.setFumigationNonOakPackageFormula(null);
      supplier.setFumigationOakPackageFormula(null);
    }
    supplier.setStatus(
        PartyMasterExcelSupport.resolveStatus(
            row.status(), creating ? null : supplier.getStatus()));
    supplier.setUpdatedAt(LocalDateTime.now());
    supplierRepository.save(supplier);
  }

  private Supplier resolveForImport(String code, String name, String category) {
    if (code != null && !code.isBlank()) {
      Supplier byCode = supplierRepository.findByCode(code.trim()).orElse(null);
      if (byCode != null) {
        return byCode;
      }
    }
    if (supplierRepository.existsByCategoryAndNameNormalized(category, name, null)) {
      throw new IllegalArgumentException("供应商名称已存在：" + name + "（如需更新请填写正确编码）");
    }
    return new Supplier();
  }

  private ImportRow mapImportRow(Row row, String category) {
    Map<String, Integer> headers = CostExcelSupport.readHeaderMap(row.getSheet().getRow(0));
    String code = CostExcelSupport.readByHeader(row, headers, "编码", "Code");
    String name = CostExcelSupport.readByHeader(row, headers, "名称", "Name", "供应商名称");
    String shortName =
        CostExcelSupport.readByHeader(row, headers, "简称", "Short Name", "ShortName");
    boolean typesHeaderPresent =
        CostExcelSupport.findColumn(headers, "类型", "Type", "Types") >= 0;
    String typesRaw = CostExcelSupport.readByHeader(row, headers, "类型", "Type", "Types");
    String contactName =
        CostExcelSupport.readByHeader(row, headers, "联系人", "Contact", "Contact Name");
    String phone = CostExcelSupport.readByHeader(row, headers, "电话", "Phone", "Mobile");
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
        && shortName.isBlank()
        && typesRaw.isBlank()
        && contactName.isBlank()
        && phone.isBlank()
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
    boolean typesProvided = typesHeaderPresent && SupplierCategories.supportsTypes(category);
    if (typesProvided) {
      try {
        types = SupplierOtherTypes.parseExcelValue(typesRaw, supplierTypeRepository);
      } catch (IllegalArgumentException ex) {
        typeError = ex.getMessage();
        types = null;
      }
    }
    return new ImportRow(
        code,
        name,
        shortName,
        types,
        typesProvided,
        contactName,
        phone,
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

  private void validateSaveRequest(
      SupplierSaveRequest request, String category, Long excludeId) {
    if (request.name() == null || request.name().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "供应商名称不能为空");
    }
    if (request.status() == null || (request.status() != 0 && request.status() != 1)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的供应商状态");
    }
    if (SupplierCategories.supportsTypes(category)) {
      try {
        SupplierOtherTypes.normalizeIds(request.types(), supplierTypeRepository);
      } catch (IllegalArgumentException ex) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
      }
    }
    assertNameAvailable(
        category, PartyMasterExcelSupport.normalizeName(request.name()), excludeId);
  }

  private void assertNameAvailable(String category, String name, Long excludeId) {
    if (supplierRepository.existsByCategoryAndNameNormalized(category, name, excludeId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "供应商名称已存在：" + name);
    }
  }

  private void apply(Supplier supplier, SupplierSaveRequest request, String category) {
    supplier.setName(PartyMasterExcelSupport.normalizeName(request.name()));
    supplier.setShortName(PartyMasterExcelSupport.trimToNull(request.shortName()));
    supplier.setContactName(PartyMasterExcelSupport.trimToNull(request.contactName()));
    supplier.setPhone(PartyMasterExcelSupport.trimToNull(request.phone()));
    supplier.setEmail(PartyMasterExcelSupport.trimToNull(request.email()));
    supplier.setRemark(PartyMasterExcelSupport.trimToNull(request.remark()));
    if (SupplierCategories.supportsTypes(category)) {
      supplier.setTypes(SupplierOtherTypes.normalizeIds(request.types(), supplierTypeRepository));
    } else {
      supplier.setTypes(List.of());
    }
    if (SupplierCategories.supportsFormulas(category)) {
      supplier.setNonFumigationPackageFormula(
          PartyMasterExcelSupport.trimToNull(request.nonFumigationPackageFormula()));
      supplier.setFumigationNonOakPackageFormula(
          PartyMasterExcelSupport.trimToNull(request.fumigationNonOakPackageFormula()));
      supplier.setFumigationOakPackageFormula(
          PartyMasterExcelSupport.trimToNull(request.fumigationOakPackageFormula()));
    } else {
      supplier.setNonFumigationPackageFormula(null);
      supplier.setFumigationNonOakPackageFormula(null);
      supplier.setFumigationOakPackageFormula(null);
    }
    supplier.setStatus(request.status());
    supplier.setUpdatedAt(LocalDateTime.now());
  }

  private String resolveCategory(String category) {
    try {
      return SupplierCategories.normalize(category);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
  }

  private String[] exportHeaders(String category) {
    List<String> headers = new ArrayList<>();
    headers.add("编码");
    headers.add("名称");
    headers.add("简称");
    if (SupplierCategories.supportsTypes(category)) {
      headers.add("类型");
    }
    headers.add("联系人");
    headers.add("电话");
    headers.add("邮箱");
    headers.add("备注");
    if (SupplierCategories.supportsFormulas(category)) {
      headers.add("非熏蒸打包价公式");
      headers.add("熏蒸打包价（非橡木）公式");
      headers.add("熏蒸打包价（橡木）公式");
    }
    headers.add("状态");
    return headers.toArray(String[]::new);
  }

  private record ImportRow(
      String code,
      String name,
      String shortName,
      List<String> types,
      boolean typesProvided,
      String contactName,
      String phone,
      String email,
      String remark,
      String nonFumigationPackageFormula,
      String fumigationNonOakPackageFormula,
      String fumigationOakPackageFormula,
      StatusCell status,
      String typeError) {}
}
