package com.furuiduo.quote.cost.service;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.cost.dto.CostBatchDeleteRequest;
import com.furuiduo.quote.cost.dto.CostBatchUpdateRequest;
import com.furuiduo.quote.cost.dto.CostImportResult;
import com.furuiduo.quote.cost.dto.FumigationCostResponse;
import com.furuiduo.quote.cost.dto.FumigationCostSaveRequest;
import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.repository.CostFumigationRepository;
import com.furuiduo.quote.cost.support.CostExcelSupport;
import com.furuiduo.quote.cost.support.FumigationCostExcelExporter;

@Service
public class CostFumigationService {

  private static final String[] IMPORT_HEADERS = {
    "PORT",
    "STATION",
    "NON-OAK OUTDOOR",
    "NON-OAK IN DOOR",
    "NON-OAK 报价(夏季)",
    "NON-OAK 报价(冬季)",
    "OAK OUTDOOR",
    "OAK IN DOOR",
    "OAK 报价(夏季)",
    "OAK 报价(冬季)",
    "备注"
  };

  private final CostFumigationRepository repository;
  private final CostGridTemplateService templateService;

  public CostFumigationService(
      CostFumigationRepository repository, CostGridTemplateService templateService) {
    this.repository = repository;
    this.templateService = templateService;
  }

  public PageResult<FumigationCostResponse> list(
      int page, int pageSize, String port, String station) {
    int safePage = Math.max(page, 1);
    int safePageSize = Math.min(Math.max(pageSize, 1), 200);

    List<CostFumigation> filtered =
        repository.findAll().stream()
            .filter(item -> contains(item.getPort(), port))
            .filter(item -> contains(item.getStation(), station))
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
        IMPORT_HEADERS,
        this::mapImportRow,
        this::validateImportRow,
        (rowNum, entity) -> {
          entity.touch();
          repository.save(entity);
        });
  }

  public byte[] exportExcel(String port, String station, Long templateId) {
    List<CostFumigation> items =
        repository.findAll().stream()
            .filter(item -> contains(item.getPort(), port))
            .filter(item -> contains(item.getStation(), station))
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
    var headers = CostExcelSupport.readHeaderMap(row.getSheet().getRow(0));
    String port = readHeader(row, headers, "PORT", "港口");
    String station = readHeader(row, headers, "STATION", "场站");
    if (port.isBlank() && station.isBlank()) {
      return null;
    }
    CostFumigation entity = new CostFumigation();
    entity.setPort(port);
    entity.setStation(station);
    entity.setNonOakOutdoor(
        CostExcelSupport.readDecimalByHeader(
            row, headers, "NON-OAK OUTDOOR", "NON-OAK OUTDOOR"));
    entity.setNonOakIndoor(
        CostExcelSupport.readDecimalByHeader(row, headers, "NON-OAK IN DOOR", "NON-OAK IN DOOR"));
    entity.setNonOakQuoteSummer(
        readHeader(row, headers, "NON-OAK 报价(夏季)", "NON-OAK 报价夏季"));
    entity.setNonOakQuoteWinter(
        readHeader(row, headers, "NON-OAK 报价(冬季)", "NON-OAK 报价冬季"));
    entity.setOakOutdoor(
        CostExcelSupport.readDecimalByHeader(row, headers, "OAK OUTDOOR", "OAK OUTDOOR"));
    entity.setOakIndoor(
        CostExcelSupport.readDecimalByHeader(row, headers, "OAK IN DOOR", "OAK IN DOOR"));
    entity.setOakQuoteSummer(readHeader(row, headers, "OAK 报价(夏季)", "OAK 报价夏季"));
    entity.setOakQuoteWinter(readHeader(row, headers, "OAK 报价(冬季)", "OAK 报价冬季"));
    entity.setRemark(readHeader(row, headers, "备注", "REMARK"));
    return entity;
  }

  private String readHeader(
      Row row, Map<String, Integer> headers, String... aliases) {
    for (String alias : aliases) {
      String value = CostExcelSupport.readByHeader(row, headers, alias);
      if (!value.isBlank()) {
        return value;
      }
    }
    return "";
  }

  private String validateImportRow(CostFumigation entity) {
    return null;
  }

  private void validateSave(FumigationCostSaveRequest request) {}

  private void applySave(CostFumigation entity, FumigationCostSaveRequest request) {
    entity.setPort(request.port());
    entity.setStation(request.station());
    entity.setNonOakOutdoor(request.nonOakOutdoor());
    entity.setNonOakIndoor(request.nonOakIndoor());
    entity.setNonOakQuoteSummer(request.nonOakQuoteSummer());
    entity.setNonOakQuoteWinter(request.nonOakQuoteWinter());
    entity.setOakOutdoor(request.oakOutdoor());
    entity.setOakIndoor(request.oakIndoor());
    entity.setOakQuoteSummer(request.oakQuoteSummer());
    entity.setOakQuoteWinter(request.oakQuoteWinter());
    entity.setRemark(request.remark());
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
