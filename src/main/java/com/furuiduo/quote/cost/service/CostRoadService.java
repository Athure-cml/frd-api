package com.furuiduo.quote.cost.service;

import java.io.IOException;
import java.math.BigDecimal;
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
import com.furuiduo.quote.common.RequestIds;
import com.furuiduo.quote.cost.dto.CostBatchDeleteRequest;
import com.furuiduo.quote.cost.dto.CostBatchUpdateRequest;
import com.furuiduo.quote.cost.dto.CostImportResult;
import com.furuiduo.quote.cost.dto.RoadCostResponse;
import com.furuiduo.quote.cost.dto.RoadCostSaveRequest;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.entity.CostStatus;
import com.furuiduo.quote.cost.repository.CostRoadRepository;
import com.furuiduo.quote.cost.support.CostDataExcelExporter;
import com.furuiduo.quote.cost.support.CostExcelSupport;
import com.furuiduo.quote.cost.support.CostValidityStatus;
import com.furuiduo.quote.cost.support.RoadAllInFormulaEvaluator;
import com.furuiduo.quote.supplier.repository.SupplierRepository;

@Service
public class CostRoadService {

  private static final String[] EXPORT_HEADERS = {
    "ZIP CODE",
    "City",
    "State",
    "POR",
    "POL",
    "ALL IN - NO FM",
    "ALL IN - FM (NON OAK)",
    "ALL IN - FM (OAK)",
    "SUPPLIER",
    "BASE FREIGHT",
    "FSC (%)",
    "CHASSIS",
    "OW/TRI-AXCEL",
    "SPLIT",
    "STOP OFF",
    "WAITING FEE",
    "REDELIVERY",
    "PREPULL",
    "NS LIFT",
    "OTHER FEE",
    "REMARK",
    "VALID DATE",
    "LOG YARD NAME & ADDRESS"
  };

  private final CostRoadRepository repository;
  private final CostGridTemplateService templateService;
  private final SupplierRepository supplierRepository;

  public CostRoadService(
      CostRoadRepository repository,
      CostGridTemplateService templateService,
      SupplierRepository supplierRepository) {
    this.repository = repository;
    this.templateService = templateService;
    this.supplierRepository = supplierRepository;
  }

  public PageResult<RoadCostResponse> list(
      int page,
      int pageSize,
      String zipCode,
      String city,
      String state,
      String por,
      String pol,
      String supplier,
      String status) {
    int safePage = Math.max(page, 1);
    int safePageSize = Math.min(Math.max(pageSize, 1), 200);

    List<CostRoad> filtered =
        repository.findAll().stream()
            .filter(item -> contains(item.getZipCode(), zipCode))
            .filter(item -> contains(item.getCity(), city))
            .filter(item -> contains(item.getState(), state))
            .filter(item -> contains(item.getPor(), por))
            .filter(item -> contains(item.getPol(), pol))
            .filter(item -> contains(item.getSupplier(), supplier))
            .filter(
                item ->
                    CostValidityStatus.matchesFilter(
                        item.getStatus(), status, item.getValidDate()))
            .sorted(Comparator.comparing(CostRoad::getId).reversed())
            .toList();

    return paginate(filtered, safePage, safePageSize);
  }

  public RoadCostResponse getById(Long id) {
    return RoadCostResponse.from(requireEntity(id));
  }

  @Transactional
  public RoadCostResponse create(RoadCostSaveRequest request) {
    CostRoad entity = new CostRoad();
    applySave(entity, request);
    // 表单已提交 ALL IN 时保留用户值；仅补全空值
    fillMissingAllInFromFormulas(entity);
    validateEntityRequired(entity);
    entity.touch();
    return RoadCostResponse.from(repository.save(entity));
  }

  @Transactional
  public RoadCostResponse update(Long id, RoadCostSaveRequest request) {
    CostRoad entity = requireEntity(id);
    applySave(entity, request);
    fillMissingAllInFromFormulas(entity);
    validateEntityRequired(entity);
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
      if (fields.containsKey("remark")) {
        entity.setRemark(asString(fields.get("remark")));
      }
      if (fields.containsKey("supplier")) {
        entity.setSupplier(asString(fields.get("supplier")));
      }
      if (fields.containsKey("baseFreight")) {
        entity.setBaseFreight(asDecimal(fields.get("baseFreight")));
      }
      if (fields.containsKey("supplier") || fields.containsKey("baseFreight")) {
        applyAllInFormulas(entity);
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
        (entity) -> {
          String formulaError = tryApplyAllInFormulas(entity, false);
          if (formulaError != null) {
            return formulaError;
          }
          return validateImportRow(entity);
        },
        (rowNum, entity) -> {
          entity.touch();
          repository.save(entity);
        });
  }

  public byte[] exportExcel(
      String zipCode,
      String city,
      String state,
      String por,
      String pol,
      String supplier,
      String status,
      Long templateId,
      List<Long> ids) {
    List<CostRoad> items =
        repository.findAll().stream()
            .filter(item -> !RequestIds.present(ids) || ids.contains(item.getId()))
            .filter(item -> RequestIds.present(ids) || contains(item.getZipCode(), zipCode))
            .filter(item -> RequestIds.present(ids) || contains(item.getCity(), city))
            .filter(item -> RequestIds.present(ids) || contains(item.getState(), state))
            .filter(item -> RequestIds.present(ids) || contains(item.getPor(), por))
            .filter(item -> RequestIds.present(ids) || contains(item.getPol(), pol))
            .filter(item -> RequestIds.present(ids) || contains(item.getSupplier(), supplier))
            .filter(
                item ->
                    RequestIds.present(ids)
                        || CostValidityStatus.matchesFilter(
                            item.getStatus(), status, item.getValidDate()))
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
    CostRoad entity = new CostRoad();
    entity.setZipCode(
        CostExcelSupport.readByHeader(row, headers, "ZIP CODE", "*ZIP CODE", "ZIPCODE"));
    entity.setCity(CostExcelSupport.readByHeader(row, headers, "City", "CITY", "*CITY"));
    entity.setState(CostExcelSupport.readByHeader(row, headers, "State", "STATE", "*STATE"));
    String por =
        CostExcelSupport.readByHeader(row, headers, "POR", "*POR");
    entity.setPor(por);
    entity.setPol(CostExcelSupport.readByHeader(row, headers, "POL", "*POL"));
    entity.setSupplier(CostExcelSupport.readByHeader(row, headers, "SUPPLIER", "*SUPPLIER"));
    entity.setBaseFreight(
        CostExcelSupport.readDecimalByHeader(
            row, headers, "BASE FREIGHT", "*BASE FREIGHT"));
    entity.setFsc(CostExcelSupport.readDecimalByHeader(row, headers, "FSC (%)", "FSC", "*FSC"));
    entity.setChassis(CostExcelSupport.readDecimalByHeader(row, headers, "CHASSIS"));
    entity.setTriTandemAxle(
        CostExcelSupport.readDecimalByHeader(
            row,
            headers,
            "OW/TRI-AXCEL",
            "OW/TRI-AXLE",
            "TRI/TANDEM AXLE",
            "TRI TANDEM AXLE"));
    entity.setSplit(CostExcelSupport.readDecimalByHeader(row, headers, "SPLIT"));
    entity.setStopOff(
        CostExcelSupport.readDecimalByHeader(row, headers, "STOP OFF", "STOP OFF FEE"));
    entity.setAllInNoFm(
        CostExcelSupport.readDecimalByHeader(
            row, headers, "ALL IN - NO FM", "*ALL IN - NO FM"));
    entity.setAllInFmOneWay(
        CostExcelSupport.readDecimalByHeader(
            row,
            headers,
            "ALL IN - FM (NON OAK)",
            "*ALL IN - FM (NON OAK)",
            "ALL IN - FM ONE WAY",
            "*ALL IN - FM ONE WAY"));
    entity.setAllInFmRound(
        CostExcelSupport.readDecimalByHeader(
            row,
            headers,
            "ALL IN - FM (OAK)",
            "*ALL IN - FM (OAK)",
            "ALL IN - FM ROUND",
            "*ALL IN - FM ROUND"));
    entity.setWaitingFee(
        CostExcelSupport.readDecimalByHeader(row, headers, "WAITING FEE", "WAITING"));
    entity.setRedelivery(CostExcelSupport.readDecimalByHeader(row, headers, "REDELIVERY"));
    entity.setPrepull(CostExcelSupport.readDecimalByHeader(row, headers, "PREPULL"));
    entity.setNsLift(CostExcelSupport.readDecimalByHeader(row, headers, "NS LIFT", "TO LIFT"));
    entity.setOtherFee(CostExcelSupport.readDecimalByHeader(row, headers, "OTHER FEE"));
    entity.setRemark(CostExcelSupport.readByHeader(row, headers, "REMARK"));
    entity.setValidDate(
        CostExcelSupport.readByHeader(row, headers, "VALID DATE", "*VALID DATE", "有效期"));
    entity.setLogYardNameAddress(
        CostExcelSupport.readByHeader(
            row,
            headers,
            "LOG YARD NAME & ADDRESS",
            "LOG YARD NAME / ADDRESS",
            "LOG YARD NAME &ADDRESS",
            "LOG YARD NAME",
            "LOG YARD"));
    entity.setStatus(CostValidityStatus.resolve(CostStatus.active, entity.getValidDate()));
    if (isImportRowEmpty(entity)) {
      return null;
    }
    return entity;
  }

  private boolean isImportRowEmpty(CostRoad entity) {
    return isBlank(entity.getZipCode())
        && isBlank(entity.getCity())
        && isBlank(entity.getState())
        && isBlank(entity.getPor())
        && isBlank(entity.getPol())
        && isBlank(entity.getSupplier())
        && entity.getBaseFreight() == null
        && entity.getFsc() == null
        && entity.getChassis() == null
        && entity.getTriTandemAxle() == null
        && entity.getSplit() == null
        && entity.getStopOff() == null
        && entity.getAllInNoFm() == null
        && entity.getAllInFmOneWay() == null
        && entity.getAllInFmRound() == null
        && entity.getWaitingFee() == null
        && entity.getRedelivery() == null
        && entity.getPrepull() == null
        && entity.getNsLift() == null
        && entity.getOtherFee() == null
        && isBlank(entity.getRemark())
        && isBlank(entity.getValidDate())
        && isBlank(entity.getLogYardNameAddress());
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String validateImportRow(CostRoad entity) {
    if (isBlank(entity.getZipCode())) {
      return "ZIP CODE 为必填项";
    }
    if (isBlank(entity.getCity())) {
      return "City 为必填项";
    }
    if (isBlank(entity.getState())) {
      return "State 为必填项";
    }
    if (isBlank(entity.getPor())) {
      return "POR 为必填项";
    }
    if (isBlank(entity.getPol())) {
      return "POL 为必填项";
    }
    if (isBlank(entity.getSupplier())) {
      return "SUPPLIER 为必填项";
    }
    if (entity.getAllInNoFm() == null) {
      return "ALL IN - NO FM 为必填项";
    }
    if (entity.getAllInFmOneWay() == null) {
      return "ALL IN - FM (NON OAK) 为必填项";
    }
    if (entity.getAllInFmRound() == null) {
      return "ALL IN - FM (OAK) 为必填项";
    }
    return null;
  }

  private void validateEntityRequired(CostRoad entity) {
    if (isBlank(entity.getZipCode())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ZIP CODE 为必填项");
    }
    if (isBlank(entity.getCity())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "City 为必填项");
    }
    if (isBlank(entity.getState())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "State 为必填项");
    }
    if (isBlank(entity.getPor())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "POR 为必填项");
    }
    if (isBlank(entity.getPol())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "POL 为必填项");
    }
    if (isBlank(entity.getSupplier())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SUPPLIER 为必填项");
    }
    if (entity.getAllInNoFm() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ALL IN - NO FM 为必填项");
    }
    if (entity.getAllInFmOneWay() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ALL IN - FM (NON OAK) 为必填项");
    }
    if (entity.getAllInFmRound() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ALL IN - FM (OAK) 为必填项");
    }
  }

  private void applyAllInFormulas(CostRoad entity) {
    String error = tryApplyAllInFormulas(entity, false);
    if (error != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
    }
  }

  private void fillMissingAllInFromFormulas(CostRoad entity) {
    String error = tryApplyAllInFormulas(entity, true);
    if (error != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
    }
  }

  /** @return 错误信息；成功返回 null */
  private String tryApplyAllInFormulas(CostRoad entity, boolean onlyIfMissing) {
    if (isBlank(entity.getSupplier())) {
      return null;
    }
    return supplierRepository
        .findFirstByNameIgnoreCase(entity.getSupplier().trim())
        .map(
            supplier -> {
              try {
                RoadAllInFormulaEvaluator.applySupplierFormulas(
                    entity, supplier, onlyIfMissing);
                return null;
              } catch (IllegalArgumentException ex) {
                return "供应商公式计算失败: " + ex.getMessage();
              }
            })
        .orElse(null);
  }

  private void applySave(CostRoad entity, RoadCostSaveRequest request) {
    entity.setZipCode(request.zipCode());
    entity.setCity(request.city());
    entity.setState(request.state());
    entity.setPor(request.por());
    entity.setPol(request.pol());
    entity.setSupplier(request.supplier());
    entity.setBaseFreight(request.baseFreight());
    entity.setFsc(request.fsc());
    entity.setChassis(request.chassis());
    entity.setTriTandemAxle(request.triTandemAxle());
    entity.setSplit(request.split());
    entity.setStopOff(request.stopOff());
    entity.setAllInNoFm(request.allInNoFm());
    entity.setAllInFmOneWay(request.allInFmOneWay());
    entity.setAllInFmRound(request.allInFmRound());
    entity.setWaitingFee(request.waitingFee());
    entity.setRedelivery(request.redelivery());
    entity.setPrepull(request.prepull());
    entity.setNsLift(request.nsLift());
    entity.setOtherFee(request.otherFee());
    entity.setRemark(request.remark());
    entity.setValidDate(request.validDate());
    entity.setLogYardNameAddress(request.logYardNameAddress());
    entity.setStatus(CostValidityStatus.resolve(CostStatus.active, request.validDate()));
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
