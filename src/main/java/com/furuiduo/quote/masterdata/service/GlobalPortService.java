package com.furuiduo.quote.masterdata.service;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

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
import com.furuiduo.quote.common.RequestIds;
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

  /** 与业务港口 Excel 表头一致 */
  private static final String[] EXPORT_HEADERS = {"名称", "类型", "国家"};

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
      return paginate(repository.findAll(Sort.by("nameEn")), safePage, safePageSize);
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

  /** 录入下拉：按关键词搜索港口（名称 / 国家），限制返回条数。 */
  @Transactional(readOnly = true)
  public List<GlobalPortResponse> listOptions(
      String keyword, Collection<PortType> portTypes, int limit) {
    int safeLimit = Math.min(Math.max(limit, 1), 100);
    String normalizedKeyword = SearchText.orEmpty(keyword);
    Collection<PortType> types =
        (portTypes == null || portTypes.isEmpty())
            ? List.of(PortType.values())
            : portTypes;
    return repository
        .searchOptions(
            normalizedKeyword,
            types,
            false,
            org.springframework.data.domain.PageRequest.of(0, safeLimit))
        .stream()
        .map(GlobalPortResponse::from)
        .toList();
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
    Set<String> seenBusinessKeys = new HashSet<>();
    return CostExcelSupport.importRows(
        file,
        EXPORT_HEADERS,
        this::mapImportRow,
        (entity) -> {
          String error = validateImportRow(entity);
          if (error != null) {
            return error;
          }
          String businessKey = businessKey(entity);
          if (!seenBusinessKeys.add(businessKey)) {
            return "已有该行数据（文件内重复）：" + entity.getNameEn();
          }
          if (findExistingByBusinessKey(entity).isPresent()) {
            return "已有该行数据";
          }
          return null;
        },
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
      PortType portType,
      List<Long> ids) {
    List<MdGlobalPort> items;
    if (RequestIds.present(ids)) {
      items =
          repository.findAllById(ids).stream()
              .sorted(java.util.Comparator.comparing(MdGlobalPort::getId))
              .toList();
    } else {
      items =
          repository.search(
              SearchText.orEmpty(code),
              SearchText.orEmpty(nameEn),
              SearchText.orEmpty(nameZh),
              SearchText.orEmpty(route),
              SearchText.orEmpty(countryRegion),
              portType);
    }

    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("港口");
      CostExcelSupport.writeHeaderRow(sheet, EXPORT_HEADERS);
      int rowIndex = 1;
      for (MdGlobalPort item : items) {
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(nullToEmpty(item.getNameEn()));
        row.createCell(1).setCellValue(formatPortTypeLabel(item.getPortType()));
        row.createCell(2).setCellValue(nullToEmpty(item.getCountryRegion()));
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
    String nameEn =
        firstNonBlank(
            CostExcelSupport.readByHeader(row, headers, "名称", "NAME", "Name EN", "NAME EN"),
            CostExcelSupport.readByHeader(row, headers, "Port Code", "PORT CODE", "CODE"));
    String countryRegion =
        firstNonBlank(
            CostExcelSupport.readByHeader(row, headers, "国家", "COUNTRY", "Country/Region", "COUNTRY/REGION"),
            "");
    String portTypeRaw =
        firstNonBlank(
            CostExcelSupport.readByHeader(row, headers, "类型", "TYPE", "Port Type", "PORT TYPE"),
            "");
    String code =
        CostExcelSupport.readByHeader(row, headers, "Port Code", "PORT CODE", "CODE", "编码");
    if (nameEn.isBlank() && countryRegion.isBlank() && portTypeRaw.isBlank() && code.isBlank()) {
      return null;
    }
    String resolvedName = sanitizeName(nameEn);
    if (resolvedName.isBlank()) {
      resolvedName = sanitizeName(code);
    }
    MdGlobalPort entity = new MdGlobalPort();
    entity.setNameEn(resolvedName);
    entity.setCode(code.isBlank() ? generateCode(resolvedName) : normalizeCode(code));
    entity.setNameZh(null);
    entity.setRoute(null);
    entity.setCountryRegion(trimToNull(countryRegion));
    entity.setPortType(parsePortType(portTypeRaw));
    return entity;
  }

  private String validateImportRow(MdGlobalPort entity) {
    if (entity.getNameEn() == null || entity.getNameEn().isBlank()) {
      return "名称不能为空";
    }
    if (entity.getCode() == null || entity.getCode().isBlank()) {
      return "无法根据名称生成编码";
    }
    return null;
  }

  private Optional<MdGlobalPort> findExistingByBusinessKey(MdGlobalPort entity) {
    String country =
        entity.getCountryRegion() == null ? "" : entity.getCountryRegion().trim();
    PortType portType = entity.getPortType() == null ? PortType.SEAPORT : entity.getPortType();
    return repository.findByBusinessKey(entity.getNameEn().trim(), country, portType);
  }

  private String businessKey(MdGlobalPort entity) {
    String name = sanitizeName(entity.getNameEn()).toUpperCase(Locale.ROOT);
    String country =
        entity.getCountryRegion() == null
            ? ""
            : entity.getCountryRegion().trim().toUpperCase(Locale.ROOT);
    PortType portType = entity.getPortType() == null ? PortType.SEAPORT : entity.getPortType();
    return name + '\u0001' + portType.name() + '\u0001' + country;
  }

  private void validateSave(GlobalPortSaveRequest request, MdGlobalPort existing) {
    if (request.nameEn() == null || request.nameEn().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "名称不能为空");
    }
    String name = sanitizeName(request.nameEn());
    String code =
        (request.code() == null || request.code().isBlank())
            ? generateCode(name)
            : normalizeCode(request.code());
    if (existing == null && repository.existsByCode(code)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "港口已存在");
    }
    if (existing != null
        && !existing.getCode().equals(code)
        && repository.existsByCode(code)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "港口已存在");
    }
  }

  private void apply(MdGlobalPort entity, GlobalPortSaveRequest request) {
    String name = sanitizeName(request.nameEn());
    String code =
        (request.code() == null || request.code().isBlank())
            ? generateCode(name)
            : normalizeCode(request.code());
    entity.setCode(code);
    entity.setNameEn(name);
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
    return code.trim().toUpperCase(Locale.ROOT);
  }

  /** 由名称生成稳定内部编码（仅字母数字，大写）。 */
  static String generateCode(String name) {
    String cleaned =
        sanitizeName(name)
            .replaceAll("[^A-Za-z0-9]", "")
            .toUpperCase(Locale.ROOT);
    if (cleaned.isBlank()) {
      return "PORT";
    }
    return cleaned.length() > 64 ? cleaned.substring(0, 64) : cleaned;
  }

  private static String sanitizeName(String value) {
    if (value == null) {
      return "";
    }
    // 去掉零宽字符等不可见符号
    return value.replaceAll("[\\u200B-\\u200D\\uFEFF]", "").trim();
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return "";
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
    String normalized = raw.trim();
    return switch (normalized) {
      case "港口", "海港", "SEAPORT", "Seaport", "seaport" -> PortType.SEAPORT;
      case "内陆点", "内陆", "INLAND", "Inland", "inland" -> PortType.INLAND;
      case "铁路场站", "铁路", "RAIL", "Rail", "rail" -> PortType.RAIL;
      case "机场", "AIRPORT", "Airport", "airport" -> PortType.AIRPORT;
      case "其他", "OTHER", "Other", "other" -> PortType.OTHER;
      default -> {
        try {
          yield PortType.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
          yield PortType.SEAPORT;
        }
      }
    };
  }

  private String formatPortTypeLabel(PortType portType) {
    if (portType == null) {
      return "";
    }
    return switch (portType) {
      case SEAPORT -> "港口";
      case INLAND -> "内陆点";
      case RAIL -> "铁路场站";
      case AIRPORT -> "机场";
      case OTHER -> "其他";
    };
  }
}
