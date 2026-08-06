package com.furuiduo.quote.shippingline.service;

import java.io.IOException;
import java.time.LocalDateTime;
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
import com.furuiduo.quote.shippingline.dto.ShippingLineResponse;
import com.furuiduo.quote.shippingline.dto.ShippingLineSaveRequest;
import com.furuiduo.quote.shippingline.entity.ShippingLine;
import com.furuiduo.quote.shippingline.repository.ShippingLineRepository;
import com.furuiduo.quote.shippingline.support.ShippingLineCodeGenerator;
import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class ShippingLineCommandService {

  private static final String[] EXPORT_HEADERS = {"编码", "名称", "邮箱", "备注", "状态"};

  private final ShippingLineRepository repository;
  private final ShippingLineCodeGenerator codeGenerator;

  public ShippingLineCommandService(
      ShippingLineRepository repository, ShippingLineCodeGenerator codeGenerator) {
    this.repository = repository;
    this.codeGenerator = codeGenerator;
  }

  @Transactional
  public ShippingLineResponse create(SysUser user, ShippingLineSaveRequest request) {
    validateSaveRequest(request, null);
    ShippingLine entity = new ShippingLine();
    entity.setCode(codeGenerator.next());
    entity.setCreatedBy(user.getId());
    entity.setCreatedByName(user.getRealName());
    entity.setDeptId(user.getDepartment() != null ? user.getDepartment().getId() : null);
    apply(entity, request);
    return ShippingLineResponse.from(repository.save(entity));
  }

  @Transactional
  public ShippingLineResponse update(Long id, ShippingLineSaveRequest request) {
    validateSaveRequest(request, id);
    ShippingLine entity =
        repository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "船公司不存在"));
    apply(entity, request);
    entity.setUpdatedAt(LocalDateTime.now());
    return ShippingLineResponse.from(repository.save(entity));
  }

  @Transactional
  public void delete(Long id) {
    ShippingLine entity =
        repository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "船公司不存在"));
    repository.delete(entity);
  }

  public ShippingLineResponse getById(Long id) {
    return repository
        .findById(id)
        .map(ShippingLineResponse::from)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "船公司不存在"));
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
    List<ShippingLine> items;
    if (RequestIds.present(ids)) {
      items =
          repository.findAllById(ids).stream()
              .sorted(
                  Comparator.comparing(
                      ShippingLine::getUpdatedAt,
                      Comparator.nullsLast(Comparator.reverseOrder())))
              .toList();
    } else {
      var pageable =
          PageRequest.of(0, 10_000, Sort.by(Sort.Direction.DESC, "updatedAt"));
      items =
          repository
              .search(SearchText.orEmpty(code), SearchText.orEmpty(name), status, pageable)
              .getContent();
    }
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Shipping Lines");
      CostExcelSupport.writeHeaderRow(sheet, EXPORT_HEADERS);
      int rowIndex = 1;
      for (ShippingLine item : items) {
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getCode()));
        row.createCell(1).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getName()));
        row.createCell(2).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getEmail()));
        row.createCell(3).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getRemark()));
        row.createCell(4).setCellValue(PartyMasterExcelSupport.statusLabel(item.getStatus()));
      }
      return CostExcelSupport.writeWorkbook(workbook);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to export shipping lines", ex);
    }
  }

  private void upsertImported(SysUser user, ImportRow row) {
    String name = PartyMasterExcelSupport.normalizeName(row.name());
    ShippingLine entity = resolveForImport(row.code(), name);
    boolean creating = entity.getId() == null;
    if (creating) {
      assertNameAvailable(name, null);
      entity.setCode(codeGenerator.next());
      entity.setCreatedBy(user.getId());
      entity.setCreatedByName(user.getRealName());
      entity.setDeptId(user.getDepartment() != null ? user.getDepartment().getId() : null);
    } else {
      assertNameAvailable(name, entity.getId());
    }
    entity.setName(name);
    entity.setEmail(PartyMasterExcelSupport.trimToNull(row.email()));
    entity.setRemark(PartyMasterExcelSupport.trimToNull(row.remark()));
    entity.setStatus(
        PartyMasterExcelSupport.resolveStatus(row.status(), creating ? null : entity.getStatus()));
    entity.setUpdatedAt(LocalDateTime.now());
    repository.save(entity);
  }

  /** 仅当编码命中时更新；名称已存在且编码未命中则拒绝。 */
  private ShippingLine resolveForImport(String code, String name) {
    if (code != null && !code.isBlank()) {
      ShippingLine byCode = repository.findByCode(code.trim()).orElse(null);
      if (byCode != null) {
        return byCode;
      }
    }
    if (repository.existsByNameNormalized(name, null)) {
      throw new IllegalArgumentException("船公司名称已存在：" + name + "（如需更新请填写正确编码）");
    }
    return new ShippingLine();
  }

  private ImportRow mapImportRow(Row row) {
    Map<String, Integer> headers = CostExcelSupport.readHeaderMap(row.getSheet().getRow(0));
    String code = CostExcelSupport.readByHeader(row, headers, "编码", "Code");
    String name = CostExcelSupport.readByHeader(row, headers, "名称", "Name", "船公司名称");
    String email = CostExcelSupport.readByHeader(row, headers, "邮箱", "Email");
    String remark = CostExcelSupport.readByHeader(row, headers, "备注", "Remark");
    String statusRaw = CostExcelSupport.readByHeader(row, headers, "状态", "Status");
    if (code.isBlank()
        && name.isBlank()
        && email.isBlank()
        && remark.isBlank()
        && statusRaw.isBlank()) {
      return null;
    }
    return new ImportRow(
        code, name, email, remark, PartyMasterExcelSupport.parseStatusCell(statusRaw));
  }

  private String validateImportRow(ImportRow row, Set<String> seenNames) {
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

  private void validateSaveRequest(ShippingLineSaveRequest request, Long excludeId) {
    if (request.name() == null || request.name().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "船公司名称不能为空");
    }
    if (request.status() == null || (request.status() != 0 && request.status() != 1)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的船公司状态");
    }
    assertNameAvailable(PartyMasterExcelSupport.normalizeName(request.name()), excludeId);
  }

  private void assertNameAvailable(String name, Long excludeId) {
    if (repository.existsByNameNormalized(name, excludeId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "船公司名称已存在：" + name);
    }
  }

  private void apply(ShippingLine entity, ShippingLineSaveRequest request) {
    entity.setName(PartyMasterExcelSupport.normalizeName(request.name()));
    entity.setEmail(PartyMasterExcelSupport.trimToNull(request.email()));
    entity.setRemark(PartyMasterExcelSupport.trimToNull(request.remark()));
    entity.setStatus(request.status());
    entity.setUpdatedAt(LocalDateTime.now());
  }

  private record ImportRow(
      String code, String name, String email, String remark, StatusCell status) {}
}
