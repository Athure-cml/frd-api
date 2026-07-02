package com.furuiduo.quote.masterdata.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.cost.dto.CostImportResult;
import com.furuiduo.quote.cost.support.CostExcelSupport;
import com.furuiduo.quote.masterdata.dto.DestAddressRowResponse;
import com.furuiduo.quote.masterdata.dto.DestAddressTreeNodeResponse;
import com.furuiduo.quote.masterdata.dto.DestCityResponse;
import com.furuiduo.quote.masterdata.dto.DestCitySaveRequest;
import com.furuiduo.quote.masterdata.dto.DestZipResponse;
import com.furuiduo.quote.masterdata.dto.DestZipSaveRequest;
import com.furuiduo.quote.masterdata.entity.MdDestCity;
import com.furuiduo.quote.masterdata.entity.MdDestZip;
import com.furuiduo.quote.masterdata.entity.MdUsState;
import com.furuiduo.quote.masterdata.repository.MdDestCityRepository;
import com.furuiduo.quote.masterdata.repository.MdDestZipRepository;
import com.furuiduo.quote.masterdata.repository.MdUsStateRepository;

@Service
public class DestAddressService {

  private static final String[] EXPORT_HEADERS = {"State", "City", "Zip code"};

  private final MdUsStateRepository stateRepository;
  private final MdDestCityRepository cityRepository;
  private final MdDestZipRepository zipRepository;

  public DestAddressService(
      MdUsStateRepository stateRepository,
      MdDestCityRepository cityRepository,
      MdDestZipRepository zipRepository) {
    this.stateRepository = stateRepository;
    this.cityRepository = cityRepository;
    this.zipRepository = zipRepository;
  }

  @Transactional(readOnly = true)
  public PageResult<DestAddressRowResponse> list(
      int page, int pageSize, String stateCode, String keyword) {
    int safePage = Math.max(page, 1);
    int safePageSize = Math.min(Math.max(pageSize, 1), 200);
    Page<DestAddressRowResponse> result =
        zipRepository.searchRows(
            normalizeFilter(stateCode),
            normalizeFilter(keyword),
            PageRequest.of(safePage - 1, safePageSize));
    return new PageResult<>(result.getContent(), result.getTotalElements());
  }

  @Transactional(readOnly = true)
  public List<DestAddressRowResponse> lookup(String keyword, int limit) {
    String normalized = normalizeFilter(keyword);
    if (normalized == null) {
      return List.of();
    }
    int size = Math.min(Math.max(limit, 1), 50);
    return zipRepository
        .searchRowsByZipPrefix(normalized, PageRequest.of(0, size))
        .getContent();
  }

  @Transactional(readOnly = true)
  public List<DestAddressTreeNodeResponse> tree() {
    List<MdUsState> states = stateRepository.findAll().stream()
        .sorted(Comparator.comparing(MdUsState::getCode))
        .toList();
    List<MdDestCity> cities = cityRepository.findAllByOrderByStateIdAscNameAsc();
    Map<Long, List<MdDestZip>> zipsByCity = new HashMap<>();
    for (MdDestZip zip : zipRepository.findAllByOrderByCityIdAscZipCodeAsc()) {
      zipsByCity.computeIfAbsent(zip.getCityId(), key -> new ArrayList<>()).add(zip);
    }

    List<DestAddressTreeNodeResponse> nodes = new ArrayList<>();
    for (MdUsState state : states) {
      String stateNodeId = "state-" + state.getId();
      nodes.add(
          new DestAddressTreeNodeResponse(
              "state",
              stateNodeId,
              null,
              state.getCode(),
              null,
              null,
              state.getId(),
              null,
              null,
              cityRepository.existsByStateId(state.getId())));

      for (MdDestCity city : cities) {
        if (!state.getId().equals(city.getStateId())) {
          continue;
        }
        String cityNodeId = "city-" + city.getId();
        nodes.add(
            new DestAddressTreeNodeResponse(
                "city",
                cityNodeId,
                stateNodeId,
                state.getCode(),
                city.getName(),
                null,
                state.getId(),
                city.getId(),
                null,
                zipRepository.existsByCityId(city.getId())));

        List<MdDestZip> zips = zipsByCity.getOrDefault(city.getId(), List.of());
        for (MdDestZip zip : zips) {
          nodes.add(
              new DestAddressTreeNodeResponse(
                  "zip",
                  "zip-" + zip.getId(),
                  cityNodeId,
                  state.getCode(),
                  city.getName(),
                  zip.getZipCode(),
                  state.getId(),
                  city.getId(),
                  zip.getId(),
                  false));
        }
      }
    }
    return nodes;
  }

  @Transactional(readOnly = true)
  public List<DestAddressTreeNodeResponse> listStateNodes(String stateCode, String keyword) {
    String normalizedState = normalizeFilter(stateCode);
    String normalizedKeyword = normalizeFilter(keyword);
    List<MdUsState> states;
    if (normalizedKeyword == null && normalizedState == null) {
      states =
          stateRepository.findAll().stream()
              .sorted(Comparator.comparing(MdUsState::getCode))
              .toList();
    } else if (normalizedKeyword == null) {
      states =
          stateRepository.findAll().stream()
              .filter(state -> state.getCode().equalsIgnoreCase(normalizedState))
              .toList();
    } else {
      states = stateRepository.findForDestAddressTree(normalizedState, normalizedKeyword);
    }

    List<DestAddressTreeNodeResponse> nodes = new ArrayList<>();
    for (MdUsState state : states) {
      nodes.add(
          new DestAddressTreeNodeResponse(
              "state",
              "state-" + state.getId(),
              null,
              state.getCode(),
              null,
              null,
              state.getId(),
              null,
              null,
              cityRepository.existsByStateId(state.getId())));
    }
    return nodes;
  }

  @Transactional(readOnly = true)
  public List<DestAddressTreeNodeResponse> listCityNodes(Long stateId, String keyword) {
    MdUsState state =
        stateRepository
            .findById(stateId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "州不存在"));
    String normalizedKeyword = normalizeFilter(keyword);
    String stateNodeId = "state-" + state.getId();
    List<DestAddressTreeNodeResponse> nodes = new ArrayList<>();
    for (MdDestCity city : cityRepository.findByStateIdWithKeyword(stateId, normalizedKeyword)) {
      nodes.add(
          new DestAddressTreeNodeResponse(
              "city",
              "city-" + city.getId(),
              stateNodeId,
              state.getCode(),
              city.getName(),
              null,
              state.getId(),
              city.getId(),
              null,
              zipRepository.existsByCityId(city.getId())));
    }
    return nodes;
  }

  @Transactional(readOnly = true)
  public List<DestAddressTreeNodeResponse> listZipNodes(Long cityId, String keyword) {
    MdDestCity city =
        cityRepository
            .findById(cityId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "城市不存在"));
    MdUsState state =
        stateRepository
            .findById(city.getStateId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "州不存在"));
    String normalizedKeyword = normalizeFilter(keyword);
    String cityNodeId = "city-" + city.getId();
    List<DestAddressTreeNodeResponse> nodes = new ArrayList<>();
    for (MdDestZip zip : zipRepository.findByCityIdWithKeyword(cityId, normalizedKeyword)) {
      nodes.add(
          new DestAddressTreeNodeResponse(
              "zip",
              "zip-" + zip.getId(),
              cityNodeId,
              state.getCode(),
              city.getName(),
              zip.getZipCode(),
              state.getId(),
              city.getId(),
              zip.getId(),
              false));
    }
    return nodes;
  }

  private static String normalizeFilter(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  @Transactional
  public DestCityResponse createCity(DestCitySaveRequest request) {
    validateCitySave(request, null);
    MdDestCity entity = new MdDestCity();
    applyCity(entity, request);
    return DestCityResponse.from(cityRepository.save(entity));
  }

  @Transactional
  public DestCityResponse updateCity(Long id, DestCitySaveRequest request) {
    MdDestCity entity = requireCity(id);
    validateCitySave(request, entity);
    applyCity(entity, request);
    return DestCityResponse.from(cityRepository.save(entity));
  }

  @Transactional
  public void deleteCity(Long id) {
    MdDestCity entity = requireCity(id);
    zipRepository.deleteByCityId(entity.getId());
    cityRepository.delete(entity);
  }

  @Transactional
  public DestZipResponse createZip(DestZipSaveRequest request) {
    validateZipSave(request, null);
    MdDestZip entity = new MdDestZip();
    applyZip(entity, request);
    return DestZipResponse.from(zipRepository.save(entity));
  }

  @Transactional
  public DestZipResponse updateZip(Long id, DestZipSaveRequest request) {
    MdDestZip entity = requireZip(id);
    validateZipSave(request, entity);
    applyZip(entity, request);
    return DestZipResponse.from(zipRepository.save(entity));
  }

  @Transactional
  public void deleteZip(Long id) {
    zipRepository.delete(requireZip(id));
  }

  @Transactional
  public CostImportResult importExcel(MultipartFile file) throws IOException {
    return CostExcelSupport.importRows(
        file,
        EXPORT_HEADERS,
        this::mapImportRow,
        this::validateImportRow,
        (rowNum, row) -> upsertImportRow(row));
  }

  @Transactional(readOnly = true)
  public byte[] exportExcel() {
    List<MdUsState> states = stateRepository.findAll();
    Map<Long, MdUsState> stateMap = new HashMap<>();
    for (MdUsState state : states) {
      stateMap.put(state.getId(), state);
    }

    List<ExportRow> rows = new ArrayList<>();
    for (MdDestCity city : cityRepository.findAllByOrderByStateIdAscNameAsc()) {
      MdUsState state = stateMap.get(city.getStateId());
      String stateCode = state != null ? state.getCode() : "";
      List<MdDestZip> zips = zipRepository.findByCityIdOrderByZipCodeAsc(city.getId());
      if (zips.isEmpty()) {
        rows.add(new ExportRow(stateCode, city.getName(), ""));
      } else {
        for (MdDestZip zip : zips) {
          rows.add(new ExportRow(stateCode, city.getName(), zip.getZipCode()));
        }
      }
    }
    rows.sort(
        Comparator.comparing(ExportRow::state)
            .thenComparing(ExportRow::city)
            .thenComparing(ExportRow::zipCode));

    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Dest Address");
      CostExcelSupport.writeHeaderRow(sheet, EXPORT_HEADERS);
      int rowIndex = 1;
      for (ExportRow item : rows) {
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(item.state());
        row.createCell(1).setCellValue(item.city());
        row.createCell(2).setCellValue(item.zipCode());
      }
      return CostExcelSupport.writeWorkbook(workbook);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to export dest addresses", ex);
    }
  }

  private void upsertImportRow(ImportRow row) {
    MdUsState state =
        stateRepository
            .findByCode(row.state().trim().toUpperCase())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "州不存在：" + row.state()));

    MdDestCity city =
        cityRepository
            .findByStateIdAndNameIgnoreCase(state.getId(), row.city().trim())
            .orElseGet(
                () -> {
                  MdDestCity created = new MdDestCity();
                  created.setStateId(state.getId());
                  created.setName(row.city().trim());
                  return cityRepository.save(created);
                });

    zipRepository
        .findByCityIdAndZipCodeIgnoreCase(city.getId(), row.zipCode().trim())
        .ifPresentOrElse(
            existing -> {
              existing.setZipCode(row.zipCode().trim());
              zipRepository.save(existing);
            },
            () -> {
              MdDestZip zip = new MdDestZip();
              zip.setCityId(city.getId());
              zip.setZipCode(row.zipCode().trim());
              zipRepository.save(zip);
            });
  }

  private ImportRow mapImportRow(Row row) {
    var headers = CostExcelSupport.readHeaderMap(row.getSheet().getRow(0));
    String state = CostExcelSupport.readByHeader(row, headers, "State", "STATE");
    String city = CostExcelSupport.readByHeader(row, headers, "City", "CITY");
    String zipCode = CostExcelSupport.readByHeader(row, headers, "Zip code", "ZIP CODE", "ZIPCODE");
    if (state.isBlank() && city.isBlank() && zipCode.isBlank()) {
      return null;
    }
    return new ImportRow(state, city, zipCode);
  }

  private String validateImportRow(ImportRow row) {
    if (row.state() == null || row.state().isBlank()) {
      return "State 不能为空";
    }
    if (row.city() == null || row.city().isBlank()) {
      return "City 不能为空";
    }
    if (row.zipCode() == null || row.zipCode().isBlank()) {
      return "Zip code 不能为空";
    }
    return null;
  }

  private void validateCitySave(DestCitySaveRequest request, MdDestCity existing) {
    if (request.stateId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "州 ID 不能为空");
    }
    if (request.name() == null || request.name().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "城市名称不能为空");
    }
    stateRepository
        .findById(request.stateId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "州不存在"));
    cityRepository
        .findByStateIdAndNameIgnoreCase(request.stateId(), request.name().trim())
        .ifPresent(
            duplicate -> {
              if (existing == null || !duplicate.getId().equals(existing.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "该州下城市已存在");
              }
            });
  }

  private void validateZipSave(DestZipSaveRequest request, MdDestZip existing) {
    if (request.cityId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "城市 ID 不能为空");
    }
    if (request.zipCode() == null || request.zipCode().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮编不能为空");
    }
    cityRepository
        .findById(request.cityId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "城市不存在"));
    zipRepository
        .findByCityIdAndZipCodeIgnoreCase(request.cityId(), request.zipCode().trim())
        .ifPresent(
            duplicate -> {
              if (existing == null || !duplicate.getId().equals(existing.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "该城市下邮编已存在");
              }
            });
  }

  private void applyCity(MdDestCity entity, DestCitySaveRequest request) {
    entity.setStateId(request.stateId());
    entity.setName(request.name().trim());
  }

  private void applyZip(MdDestZip entity, DestZipSaveRequest request) {
    entity.setCityId(request.cityId());
    entity.setZipCode(request.zipCode().trim());
  }

  private MdDestCity requireCity(Long id) {
    return cityRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "城市不存在"));
  }

  private MdDestZip requireZip(Long id) {
    return zipRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "邮编不存在"));
  }

  private record ImportRow(String state, String city, String zipCode) {}

  private record ExportRow(String state, String city, String zipCode) {}
}
