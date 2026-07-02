package com.furuiduo.quote.cost.service;

import java.io.IOException;
import java.math.BigDecimal;
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
import com.furuiduo.quote.cost.dto.RoadCostResponse;
import com.furuiduo.quote.cost.dto.RoadCostSaveRequest;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.repository.CostRoadRepository;
import com.furuiduo.quote.cost.support.CostDataExcelExporter;
import com.furuiduo.quote.cost.support.CostExcelSupport;

@Service
public class CostRoadService {

  private static final String[] EXPORT_HEADERS = {
    "*VALID DATE",
    "*SUPPLIER",
    "LOG YARD NAME &ADDRESS",
    "*City",
    "*State",
    "*POR",
    "*POL",
    "*BASE FREIGHT",
    "FSC",
    "CHASSIS",
    "OW/TRI-AXCEL",
    "SPLIT",
    "STOP OFF",
    "*ALL IN",
    "ALL IN (NON OAK)",
    "ALL IN (OAK)",
    "WAITING FEE",
    "REDILEVEY",
    "PREPULL",
    "NS LIFT",
    "REMARK"
  };

  private final CostRoadRepository repository;
  private final CostGridTemplateService templateService;

  public CostRoadService(
      CostRoadRepository repository, CostGridTemplateService templateService) {
    this.repository = repository;
    this.templateService = templateService;
  }

  public PageResult<RoadCostResponse> list(
      int page, int pageSize, String supplier, String city, String state, String pol) {
    int safePage = Math.max(page, 1);
    int safePageSize = Math.min(Math.max(pageSize, 1), 200);

    List<CostRoad> filtered =
        repository.findAll().stream()
            .filter(item -> contains(item.getSupplier(), supplier))
            .filter(item -> contains(item.getCity(), city))
            .filter(item -> contains(item.getState(), state))
            .filter(item -> contains(item.getPol(), pol))
            .sorted(Comparator.comparing(CostRoad::getId).reversed())
            .toList();

    return paginate(filtered, safePage, safePageSize);
  }

  public RoadCostResponse getById(Long id) {
    return RoadCostResponse.from(requireEntity(id));
  }

  @Transactional
  public RoadCostResponse create(RoadCostSaveRequest request) {
    validateSave(request);
    CostRoad entity = new CostRoad();
    applySave(entity, request);
    entity.touch();
    return RoadCostResponse.from(repository.save(entity));
  }

  @Transactional
  public RoadCostResponse update(Long id, RoadCostSaveRequest request) {
    validateSave(request);
    CostRoad entity = requireEntity(id);
    applySave(entity, request);
    entity.touch();
    return RoadCostResponse.from(repository.save(entity));
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
      CostRoad entity = requireEntity(id);
      if (fields.containsKey("validDate")) {
        entity.setValidDate(asString(fields.get("validDate")));
      }
      if (fields.containsKey("fsc")) {
        entity.setFsc(asDecimal(fields.get("fsc")));
      }
      if (fields.containsKey("remark")) {
        entity.setRemark(asString(fields.get("remark")));
      }
      if (fields.containsKey("supplier")) {
        entity.setSupplier(asString(fields.get("supplier")));
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
      String supplier, String city, String state, String pol, Long templateId) {
    List<CostRoad> items =
        repository.findAll().stream()
            .filter(item -> contains(item.getSupplier(), supplier))
            .filter(item -> contains(item.getCity(), city))
            .filter(item -> contains(item.getState(), state))
            .filter(item -> contains(item.getPol(), pol))
            .sorted(Comparator.comparing(CostRoad::getId))
            .toList();

    var layout = templateService.resolveExportLayout("road", templateId);
    return CostDataExcelExporter.exportRoad(items, layout);
  }

  private PageResult<RoadCostResponse> paginate(List<CostRoad> filtered, int page, int pageSize) {
    int total = filtered.size();
    int fromIndex = (page - 1) * pageSize;
    if (fromIndex >= total) {
      return new PageResult<>(List.of(), total);
    }
    int toIndex = Math.min(fromIndex + pageSize, total);
    List<RoadCostResponse> items =
        filtered.subList(fromIndex, toIndex).stream().map(RoadCostResponse::from).toList();
    return new PageResult<>(items, total);
  }

  private CostRoad mapImportRow(Row row) {
    var headers = CostExcelSupport.readHeaderMap(row.getSheet().getRow(0));
    String supplier = CostExcelSupport.readByHeader(row, headers, "SUPPLIER");
    if (supplier.isBlank()) {
      return null;
    }
    CostRoad entity = new CostRoad();
    entity.setValidDate(CostExcelSupport.readByHeader(row, headers, "VALID DATE"));
    entity.setSupplier(supplier);
    entity.setLogYardNameAddress(
        CostExcelSupport.readByHeader(
            row, headers, "LOG YARD NAME &ADDRESS", "LOG YARD NAME", "LOG YARD", "LOC YARD"));
    entity.setCity(CostExcelSupport.readByHeader(row, headers, "City", "CITY"));
    entity.setState(CostExcelSupport.readByHeader(row, headers, "State", "STATE"));
    entity.setPor(CostExcelSupport.readByHeader(row, headers, "POR"));
    entity.setPol(CostExcelSupport.readByHeader(row, headers, "POL"));
    entity.setBaseFreight(
        CostExcelSupport.readDecimalByHeader(row, headers, "BASE FREIGHT", "BASE FRE"));
    entity.setFsc(CostExcelSupport.readDecimalByHeader(row, headers, "FSC"));
    entity.setChassis(CostExcelSupport.readDecimalByHeader(row, headers, "CHASSIS"));
    entity.setOwTriAxle(
        CostExcelSupport.readDecimalByHeader(row, headers, "OW/TRI-AXCEL", "OW/TRI-A", "OW/TRI"));
    entity.setSplit(CostExcelSupport.readDecimalByHeader(row, headers, "SPLIT"));
    entity.setStopOff(CostExcelSupport.readDecimalByHeader(row, headers, "STOP OFF"));
    entity.setAllIn(CostExcelSupport.readDecimalByHeader(row, headers, "ALL IN"));
    entity.setAllInNonOak(
        CostExcelSupport.readDecimalByHeader(row, headers, "ALL IN (NON OAK)", "NON OAK"));
    entity.setAllInOak(
        CostExcelSupport.readDecimalByHeader(row, headers, "ALL IN (OAK)", "ALL IN (O)"));
    entity.setWaitingFee(
        CostExcelSupport.readDecimalByHeader(row, headers, "WAITING FEE", "WAITING"));
    entity.setRedelivery(
        CostExcelSupport.readDecimalByHeader(
            row, headers, "REDILEVEY", "REDELIVEY", "REDELIVERY"));
    entity.setPrepull(CostExcelSupport.readDecimalByHeader(row, headers, "PREPULL"));
    entity.setNsLift(CostExcelSupport.readDecimalByHeader(row, headers, "NS LIFT"));
    entity.setRemark(CostExcelSupport.readByHeader(row, headers, "REMARK"));
    return entity;
  }

  private String validateImportRow(CostRoad entity) {
    if (entity.getSupplier() == null || entity.getSupplier().isBlank()) {
      return "供应商不能为空";
    }
    return null;
  }

  private void validateSave(RoadCostSaveRequest request) {
    if (request.supplier() == null || request.supplier().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "供应商不能为空");
    }
  }

  private void applySave(CostRoad entity, RoadCostSaveRequest request) {
    entity.setValidDate(request.validDate());
    entity.setSupplier(request.supplier());
    entity.setLogYardNameAddress(request.logYardNameAddress());
    entity.setCity(request.city());
    entity.setState(request.state());
    entity.setPor(request.por());
    entity.setPol(request.pol());
    entity.setBaseFreight(request.baseFreight());
    entity.setFsc(request.fsc());
    entity.setChassis(request.chassis());
    entity.setOwTriAxle(request.owTriAxle());
    entity.setSplit(request.split());
    entity.setStopOff(request.stopOff());
    entity.setAllIn(request.allIn());
    entity.setAllInNonOak(request.allInNonOak());
    entity.setAllInOak(request.allInOak());
    entity.setWaitingFee(request.waitingFee());
    entity.setRedelivery(request.redelivery());
    entity.setPrepull(request.prepull());
    entity.setNsLift(request.nsLift());
    entity.setRemark(request.remark());
    if (request.extraFields() != null) {
      entity.setExtraFields(request.extraFields());
    }
  }

  private CostRoad requireEntity(Long id) {
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

  private String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private BigDecimal asDecimal(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    return new BigDecimal(String.valueOf(value));
  }
}
