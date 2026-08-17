package com.furuiduo.quote.cost.service;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.common.RequestIds;
import com.furuiduo.quote.common.SearchText;
import com.furuiduo.quote.cost.dto.CostBatchDeleteRequest;
import com.furuiduo.quote.cost.dto.CostBatchUpdateRequest;
import com.furuiduo.quote.cost.dto.CostImportResult;
import com.furuiduo.quote.cost.dto.CostTableTemplateLayout;
import com.furuiduo.quote.cost.dto.FumigationCostResponse;
import com.furuiduo.quote.cost.dto.FumigationCostSaveRequest;
import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostStatus;
import com.furuiduo.quote.cost.repository.CostFumigationRepository;
import com.furuiduo.quote.cost.support.CostDataExcelExporter;
import com.furuiduo.quote.cost.support.CostExcelSupport;
import com.furuiduo.quote.cost.support.CostMasterRefValidator;
import com.furuiduo.quote.cost.support.CostTemplateImportSupport;
import com.furuiduo.quote.cost.support.CostValidityStatus;

@Service
public class CostFumigationService {

  private static final String[] IMPORT_HEADERS = {
    "REGION",
    "STATION",
    "FM-OUTDOOR NON OAK",
    "FM-OUTDOOR OAK",
    "FM-OUTDOOR VALIDITY",
    "FM-INDOOR NON OAK",
    "FM-INDOOR OAK",
    "FM-INDOOR VALIDITY",
    "ADDRESS"
  };

  private final CostFumigationRepository repository;
  private final CostGridTemplateService templateService;
  private final CostMasterRefValidator masterRefValidator;

  public CostFumigationService(
      CostFumigationRepository repository,
      CostGridTemplateService templateService,
      CostMasterRefValidator masterRefValidator) {
    this.repository = repository;
    this.templateService = templateService;
    this.masterRefValidator = masterRefValidator;
  }

  public PageResult<FumigationCostResponse> list(
      int page,
      int pageSize,
      String region,
      String station,
      String outdoorValidity,
      String indoorValidity,
      String status) {
    int safePage = Math.max(page, 1);
    int safePageSize = Math.min(Math.max(pageSize, 1), 200);
    String r = SearchText.orEmpty(region);
    String st = SearchText.orEmpty(station);
    String ov = SearchText.orEmpty(outdoorValidity);
    String iv = SearchText.orEmpty(indoorValidity);
    String statusFilter = status;
    boolean filterStatus = statusFilter != null && !statusFilter.isBlank();

    if (!filterStatus) {
      var pageable =
          PageRequest.of(safePage - 1, safePageSize, Sort.by(Sort.Direction.DESC, "id"));
      Page<CostFumigation> result = repository.search(r, st, ov, iv, pageable);
      return new PageResult<>(
          result.getContent().stream().map(FumigationCostResponse::from).toList(),
          result.getTotalElements());
    }

    var pageable = Pageable.unpaged(Sort.by(Sort.Direction.DESC, "id"));
    List<CostFumigation> filtered =
        repository.search(r, st, ov, iv, pageable).getContent().stream()
            .filter(
                item ->
                    CostValidityStatus.matchesFilter(
                        item.getStatus(),
                        statusFilter,
                        item.getOutdoorValidity(),
                        item.getIndoorValidity()))
            .toList();
    return paginate(filtered, safePage, safePageSize);
  }

  public FumigationCostResponse getById(Long id) {
    return FumigationCostResponse.from(requireEntity(id));
  }

  @Transactional
  public FumigationCostResponse create(FumigationCostSaveRequest request) {
    validateSave(request);
    CostFumigation entity = new CostFumigation();
    applySave(entity, request);
    validateMasterRefs(entity);
    entity.touch();
    return FumigationCostResponse.from(repository.save(entity));
  }

  @Transactional
  public FumigationCostResponse update(Long id, FumigationCostSaveRequest request) {
    validateSave(request);
    CostFumigation entity = requireEntity(id);
    applySave(entity, request);
    validateMasterRefs(entity);
    entity.touch();
    return FumigationCostResponse.from(repository.save(entity));
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
      CostFumigation entity = requireEntity(id);
      if (fields.containsKey("address")) {
        entity.setAddress(asString(fields.get("address")));
      }
      entity.touch();
      repository.save(entity);
      updated++;
    }
    return updated;
  }

  @Transactional
  public CostImportResult importExcel(MultipartFile file, Long templateId) throws IOException {
    return importExcel(file, templateId, false);
  }

  @Transactional
  public CostImportResult importExcel(MultipartFile file, Long templateId, boolean dryRun)
      throws IOException {
    CostTableTemplateLayout layout =
        templateService.resolveExportLayout("fumigation", templateId);
    return CostExcelSupport.importRows(
        file,
        IMPORT_HEADERS,
        (row) -> mapImportRow(row, layout),
        (entity) -> {
          String required =
              CostTemplateImportSupport.validateRequired(
                  "fumigation", layout, (field) -> fumigationFieldValue(entity, field));
          if (required != null) {
            return required;
          }
          return masterRefValidator.validateFumigation(entity);
        },
        (rowNum, entity) -> {
          entity.touch();
          repository.save(entity);
        },
        dryRun);
  }

  public byte[] exportExcel(
      String region,
      String station,
      String outdoorValidity,
      String indoorValidity,
      String status,
      Long templateId,
      List<Long> ids) {
    List<CostFumigation> items;
    if (RequestIds.present(ids)) {
      items =
          repository.findAllById(ids).stream()
              .sorted(Comparator.comparing(CostFumigation::getId))
              .toList();
    } else {
      String statusFilter = status;
      boolean filterStatus = statusFilter != null && !statusFilter.isBlank();
      var pageable = Pageable.unpaged(Sort.by(Sort.Direction.ASC, "id"));
      items =
          repository
              .search(
                  SearchText.orEmpty(region),
                  SearchText.orEmpty(station),
                  SearchText.orEmpty(outdoorValidity),
                  SearchText.orEmpty(indoorValidity),
                  pageable)
              .getContent();
      if (filterStatus) {
        items =
            items.stream()
                .filter(
                    item ->
                        CostValidityStatus.matchesFilter(
                            item.getStatus(),
                            statusFilter,
                            item.getOutdoorValidity(),
                            item.getIndoorValidity()))
                .sorted(Comparator.comparing(CostFumigation::getId))
                .toList();
      }
    }

    var layout = templateService.resolveExportLayout("fumigation", templateId);
    return CostDataExcelExporter.exportFumigation(items, layout);
  }

  private PageResult<FumigationCostResponse> paginate(
      List<CostFumigation> filtered, int page, int pageSize) {
    int total = filtered.size();
    int fromIndex = (page - 1) * pageSize;
    if (fromIndex >= total) {
      return new PageResult<>(List.of(), total);
    }
    int toIndex = Math.min(fromIndex + pageSize, total);
    List<FumigationCostResponse> items =
        filtered.subList(fromIndex, toIndex).stream().map(FumigationCostResponse::from).toList();
    return new PageResult<>(items, total);
  }

  private CostFumigation mapImportRow(Row row, CostTableTemplateLayout layout) {
    Sheet sheet = row.getSheet();
    Map<String, Integer> headers = CostExcelSupport.readHeaderMap(sheet.getRow(0));
    boolean nested =
        headers.containsKey(CostExcelSupport.normalizeHeader("FM-OUTDOOR"))
            || headers.containsKey(CostExcelSupport.normalizeHeader("FM-INDOOR"));

    CostFumigation entity;
    if (nested) {
      entity = mapNestedImportRow(row);
    } else {
      String region = readHeader(row, headers, "REGION", "PORT", "港口");
      String station = readHeader(row, headers, "STATION", "场站");
      if (region.isBlank() && station.isBlank()) {
        return null;
      }
      entity = new CostFumigation();
      entity.setRegion(region);
      entity.setStation(station);
      entity.setOutdoorNonOak(
          CostExcelSupport.readDecimalByHeader(
              row, headers, "FM-OUTDOOR NON OAK", "NON-OAK OUTDOOR"));
      entity.setOutdoorOak(
          CostExcelSupport.readDecimalByHeader(row, headers, "FM-OUTDOOR OAK", "OAK OUTDOOR"));
      entity.setOutdoorValidity(
          readHeader(row, headers, "FM-OUTDOOR VALIDITY", "FM OUTDOOR VALIDITY", "有效期"));
      entity.setIndoorNonOak(
          CostExcelSupport.readDecimalByHeader(
              row, headers, "FM-INDOOR NON OAK", "NON-OAK IN DOOR"));
      entity.setIndoorOak(
          CostExcelSupport.readDecimalByHeader(row, headers, "FM-INDOOR OAK", "OAK IN DOOR"));
      entity.setIndoorValidity(
          readHeader(row, headers, "FM-INDOOR VALIDITY", "FM INDOOR VALIDITY", "有效期"));
      entity.setAddress(readHeader(row, headers, "ADDRESS", "备注", "REMARK"));
      entity.setStatus(
          CostValidityStatus.resolve(
              CostStatus.active, entity.getOutdoorValidity(), entity.getIndoorValidity()));
    }
    if (entity == null) {
      return null;
    }
    Map<String, Object> extra =
        entity.getExtraFields() == null ? new HashMap<>() : new HashMap<>(entity.getExtraFields());
    CostTemplateImportSupport.applyCustomFields("fumigation", layout, row, headers, extra);
    entity.setExtraFields(extra);
    return entity;
  }

  /** 双行表头：REGION/STATION | FM-OUTDOOR(NON OAK/OAK/生效期/VALIDITY) | FM-INDOOR(...) | ADDRESS */
  private CostFumigation mapNestedImportRow(Row row) {
    if (row.getRowNum() <= 1) {
      return null;
    }
    String region = CostExcelSupport.cellString(row.getCell(0));
    String station = CostExcelSupport.cellString(row.getCell(1));
    if (region.isBlank() && station.isBlank()) {
      return null;
    }
    // 新模板：OUT/IN 各 4 列（NON OAK / OAK / 生效期 / VALIDITY）；旧模板各 3 列无生效期
    boolean hasEffColumns = row.getLastCellNum() >= 11;
    CostFumigation entity = new CostFumigation();
    entity.setRegion(region);
    entity.setStation(station);
    entity.setOutdoorNonOak(CostExcelSupport.cellDecimal(row.getCell(2)));
    entity.setOutdoorOak(CostExcelSupport.cellDecimal(row.getCell(3)));
    Map<String, Object> extra = new HashMap<>();
    if (hasEffColumns) {
      String outdoorEff = CostExcelSupport.cellString(row.getCell(4));
      if (!outdoorEff.isBlank()) {
        extra.put("cf_fum_outdoor_eff", outdoorEff);
      }
      entity.setOutdoorValidity(CostExcelSupport.cellString(row.getCell(5)));
      entity.setIndoorNonOak(CostExcelSupport.cellDecimal(row.getCell(6)));
      entity.setIndoorOak(CostExcelSupport.cellDecimal(row.getCell(7)));
      String indoorEff = CostExcelSupport.cellString(row.getCell(8));
      if (!indoorEff.isBlank()) {
        extra.put("cf_fum_indoor_eff", indoorEff);
      }
      entity.setIndoorValidity(CostExcelSupport.cellString(row.getCell(9)));
      entity.setAddress(CostExcelSupport.cellString(row.getCell(10)));
    } else {
      entity.setOutdoorValidity(CostExcelSupport.cellString(row.getCell(4)));
      entity.setIndoorNonOak(CostExcelSupport.cellDecimal(row.getCell(5)));
      entity.setIndoorOak(CostExcelSupport.cellDecimal(row.getCell(6)));
      entity.setIndoorValidity(CostExcelSupport.cellString(row.getCell(7)));
      entity.setAddress(CostExcelSupport.cellString(row.getCell(8)));
    }
    entity.setExtraFields(extra);
    entity.setStatus(
        CostValidityStatus.resolve(
            CostStatus.active, entity.getOutdoorValidity(), entity.getIndoorValidity()));
    return entity;
  }

  private String readHeader(Row row, Map<String, Integer> headers, String... aliases) {
    for (String alias : aliases) {
      String value = CostExcelSupport.readByHeader(row, headers, alias);
      if (!value.isBlank()) {
        return value;
      }
    }
    return "";
  }

  private void validateSave(FumigationCostSaveRequest request) {
    CostTableTemplateLayout layout = templateService.resolveExportLayout("fumigation", null);
    String error =
        CostTemplateImportSupport.validateRequired(
            "fumigation",
            layout,
            (field) ->
                switch (field) {
                  case "region" -> request.region();
                  case "station" -> request.station();
                  case "outdoorNonOak" -> request.outdoorNonOak();
                  case "outdoorOak" -> request.outdoorOak();
                  case "outdoorValidity" -> request.outdoorValidity();
                  case "indoorNonOak" -> request.indoorNonOak();
                  case "indoorOak" -> request.indoorOak();
                  case "indoorValidity" -> request.indoorValidity();
                  case "address" -> request.address();
                  default ->
                      request.extraFields() == null ? null : request.extraFields().get(field);
                });
    if (error != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
    }
  }

  private void validateMasterRefs(CostFumigation entity) {
    String error = masterRefValidator.validateFumigation(entity);
    if (error != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
    }
  }

  private Object fumigationFieldValue(CostFumigation entity, String field) {
    if (field != null && field.startsWith("cf_")) {
      return entity.getExtraFields() == null ? null : entity.getExtraFields().get(field);
    }
    return switch (field) {
      case "region" -> entity.getRegion();
      case "station" -> entity.getStation();
      case "outdoorNonOak" -> entity.getOutdoorNonOak();
      case "outdoorOak" -> entity.getOutdoorOak();
      case "outdoorValidity" -> entity.getOutdoorValidity();
      case "indoorNonOak" -> entity.getIndoorNonOak();
      case "indoorOak" -> entity.getIndoorOak();
      case "indoorValidity" -> entity.getIndoorValidity();
      case "address" -> entity.getAddress();
      default -> null;
    };
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private void applySave(CostFumigation entity, FumigationCostSaveRequest request) {
    entity.setRegion(request.region());
    entity.setStation(request.station());
    entity.setOutdoorNonOak(request.outdoorNonOak());
    entity.setOutdoorOak(request.outdoorOak());
    entity.setOutdoorValidity(request.outdoorValidity());
    entity.setIndoorNonOak(request.indoorNonOak());
    entity.setIndoorOak(request.indoorOak());
    entity.setIndoorValidity(request.indoorValidity());
    entity.setAddress(request.address());
    entity.setStatus(
        CostValidityStatus.resolve(
            CostStatus.active, request.outdoorValidity(), request.indoorValidity()));
    if (request.extraFields() != null) {
      entity.setExtraFields(new LinkedHashMap<>(request.extraFields()));
    }
  }

  private CostFumigation requireEntity(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在"));
  }

  private String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }
}
