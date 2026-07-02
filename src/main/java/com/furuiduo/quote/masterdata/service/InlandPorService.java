package com.furuiduo.quote.masterdata.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.common.SearchText;
import com.furuiduo.quote.cost.dto.CostImportResult;
import com.furuiduo.quote.cost.support.CostExcelSupport;
import com.furuiduo.quote.masterdata.dto.InlandPorResponse;
import com.furuiduo.quote.masterdata.dto.InlandPorSaveRequest;
import com.furuiduo.quote.masterdata.entity.MdGlobalPort;
import com.furuiduo.quote.masterdata.entity.MdInlandPor;
import com.furuiduo.quote.masterdata.repository.MdGlobalPortRepository;
import com.furuiduo.quote.masterdata.repository.MdInlandPorRepository;

@Service
public class InlandPorService {

  private static final String[] EXPORT_HEADERS = {"POR", "POL", "Region"};

  private final MdInlandPorRepository repository;
  private final MdGlobalPortRepository globalPortRepository;

  public InlandPorService(
      MdInlandPorRepository repository, MdGlobalPortRepository globalPortRepository) {
    this.repository = repository;
    this.globalPortRepository = globalPortRepository;
  }

  @Transactional(readOnly = true)
  public PageResult<InlandPorResponse> list(
      int page, int pageSize, String name, String region, Long polId) {
    int safePage = Math.max(page, 1);
    int safePageSize = Math.min(Math.max(pageSize, 1), 200);

    List<MdInlandPor> filtered =
        repository.search(SearchText.orEmpty(name), SearchText.orEmpty(region), polId);
    Map<Long, MdGlobalPort> portMap = loadPortMap(filtered);

    return paginate(filtered, portMap, safePage, safePageSize);
  }

  @Transactional(readOnly = true)
  public InlandPorResponse getById(Long id) {
    MdInlandPor entity = requireEntity(id);
    return toResponse(entity);
  }

  @Transactional
  public InlandPorResponse create(InlandPorSaveRequest request) {
    validateSave(request);
    MdInlandPor entity = new MdInlandPor();
    apply(entity, request);
    return toResponse(repository.save(entity));
  }

  @Transactional
  public InlandPorResponse update(Long id, InlandPorSaveRequest request) {
    MdInlandPor entity = requireEntity(id);
    validateSave(request);
    apply(entity, request);
    return toResponse(repository.save(entity));
  }

  @Transactional
  public void delete(Long id) {
    repository.delete(requireEntity(id));
  }

  @Transactional
  public CostImportResult importExcel(MultipartFile file) throws IOException {
    return CostExcelSupport.importRows(
        file,
        EXPORT_HEADERS,
        this::mapImportRow,
        this::validateImportRow,
        (rowNum, row) -> {
          MdGlobalPort pol =
              globalPortRepository
                  .findByCode(row.polCode().trim().toUpperCase())
                  .orElseThrow(
                      () ->
                          new ResponseStatusException(
                              HttpStatus.BAD_REQUEST, "POL 不存在：" + row.polCode()));
          MdInlandPor entity = new MdInlandPor();
          entity.setName(row.por().trim());
          entity.setPolId(pol.getId());
          entity.setRegion(trimToNull(row.region()));
          repository.save(entity);
        });
  }

  @Transactional(readOnly = true)
  public byte[] exportExcel(String name, String region, Long polId) {
    List<MdInlandPor> items =
        repository.search(SearchText.orEmpty(name), SearchText.orEmpty(region), polId);
    Map<Long, MdGlobalPort> portMap = loadPortMap(items);

    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Inland POR");
      CostExcelSupport.writeHeaderRow(sheet, EXPORT_HEADERS);
      int rowIndex = 1;
      for (MdInlandPor item : items) {
        MdGlobalPort pol = portMap.get(item.getPolId());
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(nullToEmpty(item.getName()));
        row.createCell(1).setCellValue(pol != null ? nullToEmpty(pol.getCode()) : "");
        row.createCell(2).setCellValue(nullToEmpty(item.getRegion()));
      }
      return CostExcelSupport.writeWorkbook(workbook);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to export inland PORs", ex);
    }
  }

  private PageResult<InlandPorResponse> paginate(
      List<MdInlandPor> filtered, Map<Long, MdGlobalPort> portMap, int page, int pageSize) {
    int total = filtered.size();
    int fromIndex = (page - 1) * pageSize;
    if (fromIndex >= total) {
      return new PageResult<>(List.of(), total);
    }
    int toIndex = Math.min(fromIndex + pageSize, total);
    List<InlandPorResponse> items =
        filtered.subList(fromIndex, toIndex).stream()
            .map(item -> InlandPorResponse.from(item, portMap.get(item.getPolId())))
            .toList();
    return new PageResult<>(items, total);
  }

  private InlandPorResponse toResponse(MdInlandPor entity) {
    MdGlobalPort pol =
        globalPortRepository.findById(entity.getPolId()).orElse(null);
    return InlandPorResponse.from(entity, pol);
  }

  private Map<Long, MdGlobalPort> loadPortMap(List<MdInlandPor> items) {
    Map<Long, MdGlobalPort> map = new HashMap<>();
    for (MdInlandPor item : items) {
      if (item.getPolId() != null && !map.containsKey(item.getPolId())) {
        globalPortRepository.findById(item.getPolId()).ifPresent(port -> map.put(port.getId(), port));
      }
    }
    return map;
  }

  private ImportRow mapImportRow(Row row) {
    var headers = CostExcelSupport.readHeaderMap(row.getSheet().getRow(0));
    String por = CostExcelSupport.readByHeader(row, headers, "POR");
    String pol = CostExcelSupport.readByHeader(row, headers, "POL");
    String region = CostExcelSupport.readByHeader(row, headers, "Region", "REGION");
    if (por.isBlank() && pol.isBlank() && region.isBlank()) {
      return null;
    }
    return new ImportRow(por, pol, region);
  }

  private String validateImportRow(ImportRow row) {
    if (row.por() == null || row.por().isBlank()) {
      return "POR is required";
    }
    if (row.polCode() == null || row.polCode().isBlank()) {
      return "POL is required";
    }
    return null;
  }

  private void validateSave(InlandPorSaveRequest request) {
    if (request.name() == null || request.name().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "POR 名称不能为空");
    }
    if (request.polId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "POL 不能为空");
    }
    globalPortRepository
        .findById(request.polId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "POL 不存在"));
  }

  private void apply(MdInlandPor entity, InlandPorSaveRequest request) {
    entity.setName(request.name().trim());
    entity.setPolId(request.polId());
    entity.setRegion(trimToNull(request.region()));
  }

  private MdInlandPor requireEntity(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "内陆 POR 不存在"));
  }

  private String trim(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private record ImportRow(String por, String polCode, String region) {}
}
