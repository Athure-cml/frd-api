package com.furuiduo.quote.cost.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.cost.dto.CostBatchDeleteRequest;
import com.furuiduo.quote.cost.dto.CostBatchUpdateRequest;
import com.furuiduo.quote.cost.dto.CostImportResult;
import com.furuiduo.quote.cost.dto.FreightCostResponse;
import com.furuiduo.quote.cost.dto.FreightCostSaveRequest;
import com.furuiduo.quote.cost.entity.CostSea;
import com.furuiduo.quote.cost.entity.CostStatus;
import com.furuiduo.quote.cost.repository.CostSeaRepository;
import com.furuiduo.quote.cost.support.CostDataExcelExporter;
import com.furuiduo.quote.cost.support.CostExcelSupport;

@Service
public class CostSeaService {

  private static final String[] EXPORT_HEADERS = {
    "POL",
    "POD",
    "O/F RATE (USD)",
    "BUC",
    "附加费有效期",
    "ALL IN",
    "SSL",
    "备注",
    "有效期",
    "状态"
  };

  private final CostSeaRepository repository;
  private final CostGridTemplateService templateService;

  public CostSeaService(
      CostSeaRepository repository, CostGridTemplateService templateService) {
    this.repository = repository;
    this.templateService = templateService;
  }

  public PageResult<FreightCostResponse> list(
      int page,
      int pageSize,
      String origin,
      String destination,
      String carrier,
      CostStatus status) {
    int safePage = Math.max(page, 1);
    int safePageSize = Math.min(Math.max(pageSize, 1), 200);

    List<CostSea> filtered =
        repository.findAll().stream()
            .filter(item -> contains(item.getOrigin(), origin))
            .filter(item -> contains(item.getDestination(), destination))
            .filter(item -> contains(item.getCarrier(), carrier))
            .filter(item -> status == null || item.getStatus() == status)
            .sorted(Comparator.comparing(CostSea::getId).reversed())
            .toList();

    return paginate(filtered, safePage, safePageSize);
  }

  public FreightCostResponse getById(Long id) {
    return FreightCostResponse.fromSea(requireEntity(id));
  }

  @Transactional
  public FreightCostResponse create(FreightCostSaveRequest request) {
    validateSave(request);
    CostSea entity = new CostSea();
    applySave(entity, request);
    entity.touch();
    return FreightCostResponse.fromSea(repository.save(entity));
  }

  @Transactional
  public FreightCostResponse update(Long id, FreightCostSaveRequest request) {
    validateSave(request);
    CostSea entity = requireEntity(id);
    applySave(entity, request);
    entity.touch();
    return FreightCostResponse.fromSea(repository.save(entity));
  }

  @Transactional
  public void delete(Long id) {
    if (!repository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在");
    }
    repository.deleteById(id);
  }

  @Transactional
  public void batchDelete(CostBatchDeleteRequest request) {
    if (request.ids() == null || request.ids().isEmpty()) {
      return;
    }
    repository.deleteAllById(request.ids());
  }

  @Transactional
  public int batchUpdate(CostBatchUpdateRequest request) {
    if (request.ids() == null || request.ids().isEmpty()) {
      return 0;
    }
    Map<String, Object> fields = request.fields() == null ? Map.of() : request.fields();
    int updated = 0;
    for (Long id : request.ids()) {
      CostSea entity = requireEntity(id);
      if (fields.containsKey("status")) {
        entity.setStatus(CostStatus.valueOf(String.valueOf(fields.get("status"))));
      }
      if (fields.containsKey("validFrom")) {
        entity.setValidFrom(parseDate(fields.get("validFrom")));
      }
      if (fields.containsKey("validTo")) {
        entity.setValidTo(parseDate(fields.get("validTo")));
      }
      if (fields.containsKey("validDate")) {
        entity.setValidDate(asString(fields.get("validDate")));
      }
      if (fields.containsKey("buc")) {
        entity.setBuc(asDecimal(fields.get("buc")));
      }
      if (fields.containsKey("allIn")) {
        entity.setAllIn(asDecimal(fields.get("allIn")));
      }
      if (fields.containsKey("surchargeValidDate")) {
        entity.setSurchargeValidDate(parseDate(fields.get("surchargeValidDate")));
      }
      if (fields.containsKey("currency")) {
        entity.setCurrency(asString(fields.get("currency")));
      }
      if (fields.containsKey("remark")) {
        entity.setRemark(asString(fields.get("remark")));
      }
      entity.touch();
      repository.save(entity);
      updated++;
    }
    return updated;
  }

  @Transactional
  public CostImportResult importExcel(MultipartFile file) throws IOException {
    return CostExcelSupport.importRows(
        file,
        EXPORT_HEADERS,
        this::mapImportRow,
        this::validateImportRow,
        (rowNum, entity) -> {
          entity.touch();
          repository.save(entity);
        });
  }

  public byte[] exportExcel(
      String origin,
      String destination,
      String carrier,
      CostStatus status,
      Long templateId) {
    List<CostSea> items =
        repository.findAll().stream()
            .filter(item -> contains(item.getOrigin(), origin))
            .filter(item -> contains(item.getDestination(), destination))
            .filter(item -> contains(item.getCarrier(), carrier))
            .filter(item -> status == null || item.getStatus() == status)
            .sorted(Comparator.comparing(CostSea::getId))
            .toList();

    var layout = templateService.resolveExportLayout("sea", templateId);
    return CostDataExcelExporter.exportSea(items, layout);
  }

  private PageResult<FreightCostResponse> paginate(List<CostSea> filtered, int page, int pageSize) {
    int total = filtered.size();
    int fromIndex = (page - 1) * pageSize;
    if (fromIndex >= total) {
      return new PageResult<>(List.of(), total);
    }
    int toIndex = Math.min(fromIndex + pageSize, total);
    List<FreightCostResponse> items =
        filtered.subList(fromIndex, toIndex).stream().map(FreightCostResponse::fromSea).toList();
    return new PageResult<>(items, total);
  }

  private CostSea mapImportRow(Row row) {
    var headers = CostExcelSupport.readHeaderMap(row.getSheet().getRow(0));
    String origin = CostExcelSupport.readByHeader(row, headers, "POL", "起运港", "ORIGIN");
    String destination =
        CostExcelSupport.readByHeader(row, headers, "POD", "目的港", "DESTINATION");
    if (origin.isBlank() && destination.isBlank()) {
      return null;
    }
    CostSea entity = new CostSea();
    entity.setOrigin(origin);
    entity.setDestination(destination);
    entity.setUnitPrice(
        CostExcelSupport.readDecimalByHeader(
            row, headers, "O/F RATE (USD)", "海运费", "单价", "UNIT PRICE"));
    entity.setBuc(CostExcelSupport.readDecimalByHeader(row, headers, "BUC"));
    entity.setSurchargeValidDate(
        parseDate(CostExcelSupport.readByHeader(row, headers, "附加费有效期", "SURCHARGE VALID DATE")));
    entity.setAllIn(CostExcelSupport.readDecimalByHeader(row, headers, "ALL IN"));
    entity.setCarrier(CostExcelSupport.readByHeader(row, headers, "SSL", "承运商", "CARRIER"));
    entity.setRemark(CostExcelSupport.readByHeader(row, headers, "备注", "REMARK"));
    entity.setValidDate(CostExcelSupport.readByHeader(row, headers, "有效期", "VALID DATE"));
    String status = CostExcelSupport.readByHeader(row, headers, "状态", "STATUS");
    entity.setStatus(status.isBlank() ? CostStatus.draft : CostStatus.valueOf(status));
    if (entity.getCurrency() == null || entity.getCurrency().isBlank()) {
      entity.setCurrency("USD");
    }
    if (entity.getUnit() == null || entity.getUnit().isBlank()) {
      entity.setUnit("箱");
    }
    if (entity.getSpec() == null) {
      entity.setSpec("");
    }
    return entity;
  }

  private String validateImportRow(CostSea entity) {
    return null;
  }

  private void validateSave(FreightCostSaveRequest request) {}

  private void applySave(CostSea entity, FreightCostSaveRequest request) {
    entity.setOrigin(request.origin());
    entity.setDestination(request.destination());
    entity.setCarrier(request.carrier());
    entity.setSpec(request.spec());
    entity.setUnit(request.unit());
    entity.setUnitPrice(request.unitPrice());
    entity.setBuc(request.buc());
    entity.setSurchargeValidDate(parseDate(request.surchargeValidDate()));
    entity.setAllIn(request.allIn());
    entity.setValidDate(request.validDate());
    entity.setCurrency(request.currency());
    entity.setValidFrom(parseDate(request.validFrom()));
    entity.setValidTo(parseDate(request.validTo()));
    entity.setStatus(request.status() == null ? CostStatus.draft : request.status());
    entity.setRemark(request.remark());
    if (request.extraFields() != null) {
      entity.setExtraFields(request.extraFields());
    }
  }

  private CostSea requireEntity(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在"));
  }

  private boolean contains(String source, String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return true;
    }
    if (source == null) {
      return false;
    }
    return source.toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));
  }

  private LocalDate parseDate(Object value) {
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value).trim();
    if (text.isBlank()) {
      return null;
    }
    return LocalDate.parse(text.length() > 10 ? text.substring(0, 10) : text);
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private void setDecimal(Row row, int col, BigDecimal value) {
    if (value != null) {
      row.createCell(col).setCellValue(value.doubleValue());
    } else {
      row.createCell(col).setBlank();
    }
  }

  private BigDecimal asDecimal(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    String text = String.valueOf(value).trim();
    if (text.isBlank()) {
      return null;
    }
    return new BigDecimal(text);
  }

  private String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }
}
