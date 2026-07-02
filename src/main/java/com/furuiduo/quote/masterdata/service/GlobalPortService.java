package com.furuiduo.quote.masterdata.service;

import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.common.SearchText;
import com.furuiduo.quote.cost.dto.CostImportResult;
import com.furuiduo.quote.cost.support.CostExcelSupport;
import com.furuiduo.quote.masterdata.dto.GlobalPortResponse;
import com.furuiduo.quote.masterdata.dto.GlobalPortSaveRequest;
import com.furuiduo.quote.masterdata.entity.MdGlobalPort;
import com.furuiduo.quote.masterdata.entity.PortType;
import com.furuiduo.quote.masterdata.repository.MdGlobalPortRepository;
import com.furuiduo.quote.masterdata.repository.MdInlandPorRepository;

@Service
public class GlobalPortService {

  private static final String[] EXPORT_HEADERS = {
    "Port Code", "Name EN", "Name ZH", "Route", "Country/Region", "Port Type"
  };

  private final MdGlobalPortRepository repository;
  private final MdInlandPorRepository inlandPorRepository;

  public GlobalPortService(
      MdGlobalPortRepository repository, MdInlandPorRepository inlandPorRepository) {
    this.repository = repository;
    this.inlandPorRepository = inlandPorRepository;
  }

  @Transactional(readOnly = true)
  public PageResult<GlobalPortResponse> list(
      int page,
      int pageSize,
      String code,
      String nameEn,
      String nameZh,
      String route,
      String countryRegion,
      PortType portType) {
    int safePage = Math.max(page, 1);
    int safePageSize = Math.min(Math.max(pageSize, 1), 200);

    String normalizedCode = SearchText.orEmpty(code);
    String normalizedNameEn = SearchText.orEmpty(nameEn);
    String normalizedNameZh = SearchText.orEmpty(nameZh);
    String normalizedRoute = SearchText.orEmpty(route);
    String normalizedCountryRegion = SearchText.orEmpty(countryRegion);
    if (normalizedCode.isEmpty()
        && normalizedNameEn.isEmpty()
        && normalizedNameZh.isEmpty()
        && normalizedRoute.isEmpty()
        && normalizedCountryRegion.isEmpty()
        && portType == null) {
      return paginate(repository.findAll(Sort.by("code")), safePage, safePageSize);
    }

    List<MdGlobalPort> filtered =
        repository.search(
            normalizedCode,
            normalizedNameEn,
            normalizedNameZh,
            normalizedRoute,
            normalizedCountryRegion,
            portType);

    return paginate(filtered, safePage, safePageSize);
  }

  @Transactional(readOnly = true)
  public GlobalPortResponse getById(Long id) {
    return GlobalPortResponse.from(requireEntity(id));
  }

  @Transactional
  public GlobalPortResponse create(GlobalPortSaveRequest request) {
    validateSave(request, null);
    MdGlobalPort entity = new MdGlobalPort();
    apply(entity, request);
    return GlobalPortResponse.from(repository.save(entity));
  }

  @Transactional
  public GlobalPortResponse update(Long id, GlobalPortSaveRequest request) {
    MdGlobalPort entity = requireEntity(id);
    validateSave(request, entity);
    apply(entity, request);
    return GlobalPortResponse.from(repository.save(entity));
  }

  @Transactional
  public void delete(Long id) {
    MdGlobalPort entity = requireEntity(id);
    if (inlandPorRepository.existsByPolId(entity.getId())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "港口已被内陆 POR 引用，无法删除");
    }
    repository.delete(entity);
  }

  @Transactional
  public CostImportResult importExcel(MultipartFile file) throws IOException {
    return CostExcelSupport.importRows(
        file,
        EXPORT_HEADERS,
        this::mapImportRow,
        this::validateImportRow,
        (rowNum, entity) -> {
          repository
              .findByCode(entity.getCode())
              .ifPresentOrElse(
                  existing -> {
                    apply(existing, toSaveRequest(entity));
                    repository.save(existing);
                  },
                  () -> repository.save(entity));
        });
  }

  @Transactional(readOnly = true)
  public byte[] exportExcel(
      String code,
      String nameEn,
      String nameZh,
      String route,
      String countryRegion,
      PortType portType) {
    List<MdGlobalPort> items =
        repository.search(
            SearchText.orEmpty(code),
            SearchText.orEmpty(nameEn),
            SearchText.orEmpty(nameZh),
            SearchText.orEmpty(route),
            SearchText.orEmpty(countryRegion),
            portType);

    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Global Port");
      CostExcelSupport.writeHeaderRow(sheet, EXPORT_HEADERS);
      int rowIndex = 1;
      for (MdGlobalPort item : items) {
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(nullToEmpty(item.getCode()));
        row.createCell(1).setCellValue(nullToEmpty(item.getNameEn()));
        row.createCell(2).setCellValue(nullToEmpty(item.getNameZh()));
        row.createCell(3).setCellValue(nullToEmpty(item.getRoute()));
        row.createCell(4).setCellValue(nullToEmpty(item.getCountryRegion()));
        row.createCell(5)
            .setCellValue(item.getPortType() == null ? "" : item.getPortType().name());
      }
      return CostExcelSupport.writeWorkbook(workbook);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to export global ports", ex);
    }
  }

  @Transactional(readOnly = true)
  public MdGlobalPort requireByCode(String code) {
    return repository
        .findByCode(normalizeCode(code))
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "港口代码不存在：" + code));
  }

  private PageResult<GlobalPortResponse> paginate(
      List<MdGlobalPort> filtered, int page, int pageSize) {
    int total = filtered.size();
    int fromIndex = (page - 1) * pageSize;
    if (fromIndex >= total) {
      return new PageResult<>(List.of(), total);
    }
    int toIndex = Math.min(fromIndex + pageSize, total);
    List<GlobalPortResponse> items =
        filtered.subList(fromIndex, toIndex).stream().map(GlobalPortResponse::from).toList();
    return new PageResult<>(items, total);
  }

  private MdGlobalPort mapImportRow(Row row) {
    var headers = CostExcelSupport.readHeaderMap(row.getSheet().getRow(0));
    String code = CostExcelSupport.readByHeader(row, headers, "Port Code", "PORT CODE", "CODE");
    String nameEn = CostExcelSupport.readByHeader(row, headers, "Name EN", "NAME EN");
    String nameZh = CostExcelSupport.readByHeader(row, headers, "Name ZH", "NAME ZH");
    String route = CostExcelSupport.readByHeader(row, headers, "Route", "ROUTE");
    String countryRegion =
        CostExcelSupport.readByHeader(row, headers, "Country/Region", "COUNTRY/REGION");
    String portTypeRaw = CostExcelSupport.readByHeader(row, headers, "Port Type", "PORT TYPE");
    if (code.isBlank() && nameEn.isBlank() && nameZh.isBlank() && route.isBlank()
        && countryRegion.isBlank() && portTypeRaw.isBlank()) {
      return null;
    }
    MdGlobalPort entity = new MdGlobalPort();
    entity.setCode(normalizeCode(code));
    entity.setNameEn(nameEn.trim());
    entity.setNameZh(trimToNull(nameZh));
    entity.setRoute(trimToNull(route));
    entity.setCountryRegion(trimToNull(countryRegion));
    entity.setPortType(parsePortType(portTypeRaw));
    return entity;
  }

  private String validateImportRow(MdGlobalPort entity) {
    if (entity.getCode() == null || entity.getCode().isBlank()) {
      return "Port Code is required";
    }
    if (entity.getNameEn() == null || entity.getNameEn().isBlank()) {
      return "Name EN is required";
    }
    return null;
  }

  private void validateSave(GlobalPortSaveRequest request, MdGlobalPort existing) {
    if (request.code() == null || request.code().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "港口代码不能为空");
    }
    if (request.nameEn() == null || request.nameEn().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "英文名称不能为空");
    }
    String code = normalizeCode(request.code());
    if (existing == null && repository.existsByCode(code)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "港口代码已存在");
    }
    if (existing != null
        && !existing.getCode().equals(code)
        && repository.existsByCode(code)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "港口代码已存在");
    }
  }

  private void apply(MdGlobalPort entity, GlobalPortSaveRequest request) {
    entity.setCode(normalizeCode(request.code()));
    entity.setNameEn(request.nameEn().trim());
    entity.setNameZh(trimToNull(request.nameZh()));
    entity.setRoute(trimToNull(request.route()));
    entity.setCountryRegion(trimToNull(request.countryRegion()));
    entity.setPortType(request.portType() == null ? PortType.SEAPORT : request.portType());
  }

  private GlobalPortSaveRequest toSaveRequest(MdGlobalPort entity) {
    return new GlobalPortSaveRequest(
        entity.getCode(),
        entity.getNameEn(),
        entity.getNameZh(),
        entity.getRoute(),
        entity.getCountryRegion(),
        entity.getPortType());
  }

  private MdGlobalPort requireEntity(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "港口不存在"));
  }

  private String normalizeCode(String code) {
    return code.trim().toUpperCase();
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

  private PortType parsePortType(String raw) {
    if (raw == null || raw.isBlank()) {
      return PortType.SEAPORT;
    }
    try {
      return PortType.valueOf(raw.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      return PortType.SEAPORT;
    }
  }
}
