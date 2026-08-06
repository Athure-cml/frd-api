package com.furuiduo.quote.cost.service;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.common.RequestIds;
import com.furuiduo.quote.cost.dto.CostBatchDeleteRequest;
import com.furuiduo.quote.cost.dto.CostBatchUpdateRequest;
import com.furuiduo.quote.cost.dto.CostImportResult;
import com.furuiduo.quote.cost.dto.FumigationCostResponse;
import com.furuiduo.quote.cost.dto.FumigationCostSaveRequest;
import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostStatus;
import com.furuiduo.quote.cost.repository.CostFumigationRepository;
import com.furuiduo.quote.cost.support.CostExcelSupport;
import com.furuiduo.quote.cost.support.CostValidityStatus;
import com.furuiduo.quote.cost.support.FumigationCostExcelExporter;

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

  public CostFumigationService(
      CostFumigationRepository repository, CostGridTemplateService templateService) {
    this.repository = repository;
    this.templateService = templateService;
  }

  public PageResult<FumigationCostResponse> list(
      int page, int pageSize, String region, String station, String status) {
    int safePage = Math.max(page, 1);
    int safePageSize = Math.min(Math.max(pageSize, 1), 200);

    List<CostFumigation> filtered =
        repository.findAll().stream()
            .filter(item -> contains(item.getRegion(), region))
            .filter(item -> contains(item.getStation(), station))
            .filter(
                item ->
                    CostValidityStatus.matchesFilter(
                        item.getStatus(),
                        status,
                        item.getOutdoorValidity(),
                        item.getIndoorValidity()))
            .sorted(Comparator.comparing(CostFumigation::getId).reversed())
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
    entity.touch();
    return FumigationCostResponse.from(repository.save(entity));
  }

  @Transactional
  public FumigationCostResponse update(Long id, FumigationCostSaveRequest request) {
    validateSave(request);
    CostFumigation entity = requireEntity(id);
    applySave(entity, request);
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
  public CostImportResult importExcel(MultipartFile file) throws IOException {
    return CostExcelSupport.importRows(
        file,
        IMPORT_HEADERS,
        this::mapImportRow,
        this::validateImportRow,
        (rowNum, entity) -> {
          entity.touch();
          repository.save(entity);
        });
  }

  public byte[] exportExcel(
      String region, String station, String status, Long templateId, List<Long> ids) {
    List<CostFumigation> items =
        repository.findAll().stream()
            .filter(item -> !RequestIds.present(ids) || ids.contains(item.getId()))
            .filter(item -> RequestIds.present(ids) || contains(item.getRegion(), region))
            .filter(item -> RequestIds.present(ids) || contains(item.getStation(), station))
            .filter(
                item ->
                    RequestIds.present(ids)
                        || CostValidityStatus.matchesFilter(
                            item.getStatus(),
                            status,
                            item.getOutdoorValidity(),
                            item.getIndoorValidity()))
            .sorted(Comparator.comparing(CostFumigation::getId))
            .toList();

    templateService.resolveExportLayout("fumigation", templateId);
    return FumigationCostExcelExporter.export(items);
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

  private CostFumigation mapImportRow(Row row) {
    Sheet sheet = row.getSheet();
    Map<String, Integer> headers = CostExcelSupport.readHeaderMap(sheet.getRow(0));
    boolean nested =
        headers.containsKey(CostExcelSupport.normalizeHeader("FM-OUTDOOR"))
            || headers.containsKey(CostExcelSupport.normalizeHeader("FM-INDOOR"));

    if (nested) {
      return mapNestedImportRow(row);
    }

    String region = readHeader(row, headers, "REGION", "PORT", "港口");
    String station = readHeader(row, headers, "STATION", "场站");
    if (region.isBlank() && station.isBlank()) {
      return null;
    }
    CostFumigation entity = new CostFumigation();
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
    return entity;
  }

  /** 双行表头：REGION/STATION | FM-OUTDOOR(NON OAK/OAK/VALIDITY) | FM-INDOOR(...) | ADDRESS */
  private CostFumigation mapNestedImportRow(Row row) {
    if (row.getRowNum() <= 1) {
      return null;
    }
    String region = CostExcelSupport.cellString(row.getCell(0));
    String station = CostExcelSupport.cellString(row.getCell(1));
    if (region.isBlank() && station.isBlank()) {
      return null;
    }
    CostFumigation entity = new CostFumigation();
    entity.setRegion(region);
    entity.setStation(station);
    entity.setOutdoorNonOak(CostExcelSupport.cellDecimal(row.getCell(2)));
    entity.setOutdoorOak(CostExcelSupport.cellDecimal(row.getCell(3)));
    entity.setOutdoorValidity(CostExcelSupport.cellString(row.getCell(4)));
    entity.setIndoorNonOak(CostExcelSupport.cellDecimal(row.getCell(5)));
    entity.setIndoorOak(CostExcelSupport.cellDecimal(row.getCell(6)));
    entity.setIndoorValidity(CostExcelSupport.cellString(row.getCell(7)));
    entity.setAddress(CostExcelSupport.cellString(row.getCell(8)));
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

  private String validateImportRow(CostFumigation entity) {
    if (isBlank(entity.getRegion())) {
      return "REGION 为必填项";
    }
    if (isBlank(entity.getStation())) {
      return "STATION 为必填项";
    }
    if (entity.getOutdoorNonOak() == null) {
      return "FM-OUTDOOR NON OAK 为必填项";
    }
    if (entity.getOutdoorOak() == null) {
      return "FM-OUTDOOR OAK 为必填项";
    }
    if (isBlank(entity.getOutdoorValidity())) {
      return "FM-OUTDOOR VALIDITY 为必填项";
    }
    if (entity.getIndoorNonOak() == null) {
      return "FM-INDOOR NON OAK 为必填项";
    }
    if (entity.getIndoorOak() == null) {
      return "FM-INDOOR OAK 为必填项";
    }
    if (isBlank(entity.getIndoorValidity())) {
      return "FM-INDOOR VALIDITY 为必填项";
    }
    if (isBlank(entity.getAddress())) {
      return "ADDRESS 为必填项";
    }
    return null;
  }

  private void validateSave(FumigationCostSaveRequest request) {
    if (isBlank(request.region())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "REGION 为必填项");
    }
    if (isBlank(request.station())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "STATION 为必填项");
    }
    if (request.outdoorNonOak() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FM-OUTDOOR NON OAK 为必填项");
    }
    if (request.outdoorOak() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FM-OUTDOOR OAK 为必填项");
    }
    if (isBlank(request.outdoorValidity())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FM-OUTDOOR VALIDITY 为必填项");
    }
    if (request.indoorNonOak() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FM-INDOOR NON OAK 为必填项");
    }
    if (request.indoorOak() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FM-INDOOR OAK 为必填项");
    }
    if (isBlank(request.indoorValidity())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FM-INDOOR VALIDITY 为必填项");
    }
    if (isBlank(request.address())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ADDRESS 为必填项");
    }
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
      entity.setExtraFields(request.extraFields());
    }
  }

  private CostFumigation requireEntity(Long id) {
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

  private String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }
}
