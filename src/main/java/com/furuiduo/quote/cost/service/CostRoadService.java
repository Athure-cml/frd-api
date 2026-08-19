package com.furuiduo.quote.cost.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
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
import com.furuiduo.quote.cost.dto.CostRoadBatchCopyRequest;
import com.furuiduo.quote.cost.dto.CostTableTemplateLayout;
import com.furuiduo.quote.cost.dto.RoadCostResponse;
import com.furuiduo.quote.cost.dto.RoadCostSaveRequest;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.entity.CostStatus;
import com.furuiduo.quote.cost.repository.CostRoadRepository;
import com.furuiduo.quote.cost.support.CostDataExcelExporter;
import com.furuiduo.quote.cost.support.CostExcelSupport;
import com.furuiduo.quote.cost.support.CostMasterRefValidator;
import com.furuiduo.quote.cost.support.CostRoadZipPlaceholder;
import com.furuiduo.quote.cost.support.CostTemplateImportSupport;
import com.furuiduo.quote.cost.support.CostValidityStatus;
import com.furuiduo.quote.cost.support.RoadAllInFormulaEvaluator;
import com.furuiduo.quote.masterdata.dto.DestZipResolveItemResponse;
import com.furuiduo.quote.masterdata.service.DestAddressService;
import com.furuiduo.quote.supplier.repository.SupplierRepository;

@Service
public class CostRoadService {

  /** 新行记录续期来源 id */
  public static final String EXTRA_RENEWED_FROM = "cf_road_renewed_from";
  /** 源行记录续期后新行 id */
  public static final String EXTRA_RENEWED_TO = "cf_road_renewed_to";

  private static final String[] EXPORT_HEADERS = {
    "ZIP CODE",
    "CITY",
    "STATE",
    "POR",
    "SUPPLIER",
    "BASE",
    "FSC",
    "CHASSIS",
    "OW",
    "SPLIT",
    "STOP OFF",
    "ALL IN",
    "ALL IN FM NON OAK",
    "ALL IN FM OAK",
    "WAITING",
    "REDELIVERY",
    "YARD STORAGE",
    "EXTRA CHASSIS",
    "PREPULL",
    "LIFT",
    "OTHERS",
    "REMARK",
    "EFFECTIVE TIME",
    "VALID TIME",
    "PICK UP ADDRESS"
  };

  private final CostRoadRepository repository;
  private final CostGridTemplateService templateService;
  private final SupplierRepository supplierRepository;
  private final DestAddressService destAddressService;
  private final CostMasterRefValidator masterRefValidator;

  public CostRoadService(
      CostRoadRepository repository,
      CostGridTemplateService templateService,
      SupplierRepository supplierRepository,
      DestAddressService destAddressService,
      CostMasterRefValidator masterRefValidator) {
    this.repository = repository;
    this.templateService = templateService;
    this.supplierRepository = supplierRepository;
    this.destAddressService = destAddressService;
    this.masterRefValidator = masterRefValidator;
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
      BigDecimal redelivery,
      String validDate,
      String status) {
    int safePage = Math.max(page, 1);
    int safePageSize = Math.min(Math.max(pageSize, 1), 200);
    String z = SearchText.orEmpty(zipCode);
    String c = SearchText.orEmpty(city);
    String st = SearchText.orEmpty(state);
    String p = SearchText.orEmpty(por);
    String pl = SearchText.orEmpty(pol);
    String sup = SearchText.orEmpty(supplier);
    String vd = SearchText.orEmpty(validDate);
    String statusFilter = status;
    boolean filterStatus = statusFilter != null && !statusFilter.isBlank();

    if (!filterStatus) {
      var pageable =
          PageRequest.of(safePage - 1, safePageSize, Sort.by(Sort.Direction.DESC, "id"));
      Page<CostRoad> result = repository.search(z, c, st, p, pl, sup, redelivery, vd, pageable);
      return new PageResult<>(
          result.getContent().stream().map(RoadCostResponse::from).toList(),
          result.getTotalElements());
    }

    var pageable = Pageable.unpaged(Sort.by(Sort.Direction.DESC, "id"));
    List<CostRoad> filtered =
        repository.search(z, c, st, p, pl, sup, redelivery, vd, pageable).getContent().stream()
            .filter(
                item ->
                    CostValidityStatus.matchesFilter(
                        item.getStatus(), statusFilter, item.getValidDate()))
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
    validateEntityRequired(entity, null);
    validateMasterRefs(entity);
    entity.touch();
    return RoadCostResponse.from(repository.save(entity));
  }

  /**
   * 续期：新建一版运价，并把源行有效期写成「新生效期 − 1 天」。
   * 新生效期取 extraFields.cf_road_eff。新旧行通过 extraFields 互相关联。
   */
  @Transactional
  public RoadCostResponse renew(Long sourceId, RoadCostSaveRequest request) {
    if (sourceId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少源记录");
    }
    CostRoad source = requireEntity(sourceId);
    String effectiveRaw = readExtraText(request.extraFields(), "cf_road_eff");
    if (effectiveRaw == null || effectiveRaw.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "续期须填写生效期");
    }
    LocalDate effective = CostValidityStatus.tryParseDate(effectiveRaw);
    if (effective == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "生效期格式无法识别：" + effectiveRaw);
    }
    validateRenewValidDate(request.validDate(), effective);

    Map<String, Object> extras =
        request.extraFields() == null
            ? new LinkedHashMap<>()
            : new LinkedHashMap<>(request.extraFields());
    extras.put(EXTRA_RENEWED_FROM, sourceId);
    RoadCostResponse created = create(withExtraFields(request, extras));

    LocalDate previousValid = effective.minusDays(1);
    String previousValidText = previousValid.toString();
    Map<String, Object> sourceExtras =
        source.getExtraFields() == null
            ? new LinkedHashMap<>()
            : new LinkedHashMap<>(source.getExtraFields());
    sourceExtras.put(EXTRA_RENEWED_TO, created.id());
    source.setExtraFields(sourceExtras);
    source.setValidDate(previousValidText);
    source.setStatus(CostValidityStatus.resolve(CostStatus.active, previousValidText));
    source.touch();
    repository.save(source);
    return created;
  }

  private static void validateRenewValidDate(String validRaw, LocalDate effective) {
    if (validRaw == null || validRaw.isBlank()) {
      return;
    }
    LocalDate valid = CostValidityStatus.tryParseDate(validRaw.trim());
    if (valid == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "有效期格式无法识别：" + validRaw);
    }
    if (valid.isBefore(effective)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "有效期不能早于生效期");
    }
  }

  private static RoadCostSaveRequest withExtraFields(
      RoadCostSaveRequest request, Map<String, Object> extraFields) {
    return new RoadCostSaveRequest(
        request.zipCode(),
        request.city(),
        request.state(),
        request.por(),
        request.pol(),
        request.supplier(),
        request.baseFreight(),
        request.fsc(),
        request.chassis(),
        request.triTandemAxle(),
        request.split(),
        request.stopOff(),
        request.allInNoFm(),
        request.allInFmOneWay(),
        request.allInFmRound(),
        request.waitingFee(),
        request.redelivery(),
        request.prepull(),
        request.nsLift(),
        request.otherFee(),
        request.remark(),
        request.validDate(),
        request.logYardNameAddress(),
        request.status(),
        extraFields);
  }

  private static String readExtraText(Map<String, Object> extraFields, String key) {
    if (extraFields == null || key == null) {
      return null;
    }
    Object value = extraFields.get(key);
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value).trim();
    return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
  }

  @Transactional
  public RoadCostResponse update(Long id, RoadCostSaveRequest request) {
    CostRoad entity = requireEntity(id);
    applySave(entity, request);
    fillMissingAllInFromFormulas(entity);
    validateEntityRequired(entity, null);
    validateMasterRefs(entity);
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
      boolean fscChanged = false;
      if (fields.containsKey("fsc")) {
        entity.setFsc(asDecimal(fields.get("fsc")));
        fscChanged = true;
      }
      if (fields.containsKey("validDate")) {
        entity.setValidDate(asString(fields.get("validDate")));
        entity.setStatus(CostValidityStatus.resolve(CostStatus.active, entity.getValidDate()));
      }
      // 兼容旧批量字段
      if (fields.containsKey("remark")) {
        entity.setRemark(asString(fields.get("remark")));
      }
      if (fields.containsKey("supplier")) {
        entity.setSupplier(asString(fields.get("supplier")));
      }
      if (fields.containsKey("baseFreight")) {
        entity.setBaseFreight(asDecimal(fields.get("baseFreight")));
      }
      if (fscChanged
          || fields.containsKey("supplier")
          || fields.containsKey("baseFreight")) {
        applyAllInFormulas(entity);
      }
      if (fields.containsKey("supplier")) {
        validateMasterRefs(entity);
      }
      entity.touch();
      repository.save(entity);
      updated++;
    }
    return updated;
  }

  @Transactional
  public int batchCopy(CostRoadBatchCopyRequest request) {
    if (request.ids() == null || request.ids().isEmpty()) {
      return 0;
    }
    boolean applyOverrides = Boolean.TRUE.equals(request.applyOverrides());
    if (applyOverrides
        && request.fsc() == null
        && (request.validDate() == null || request.validDate().isBlank())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请至少填写燃油或有效期");
    }
    int created = 0;
    for (Long id : request.ids()) {
      CostRoad source = requireEntity(id);
      CostRoad copy = copyOf(source);
      if (applyOverrides) {
        boolean fscChanged = false;
        if (request.fsc() != null) {
          copy.setFsc(request.fsc());
          fscChanged = true;
        }
        if (request.validDate() != null && !request.validDate().isBlank()) {
          copy.setValidDate(request.validDate().trim());
        }
        copy.setStatus(CostValidityStatus.resolve(CostStatus.active, copy.getValidDate()));
        if (fscChanged) {
          applyAllInFormulas(copy);
        }
      }
      copy.touch();
      repository.save(copy);
      created++;
    }
    return created;
  }

  private CostRoad copyOf(CostRoad source) {
    CostRoad target = new CostRoad();
    target.setZipCode(source.getZipCode());
    target.setCity(source.getCity());
    target.setState(source.getState());
    target.setPor(source.getPor());
    target.setPol(source.getPol());
    target.setSupplier(source.getSupplier());
    target.setBaseFreight(source.getBaseFreight());
    target.setFsc(source.getFsc());
    target.setChassis(source.getChassis());
    target.setTriTandemAxle(source.getTriTandemAxle());
    target.setSplit(source.getSplit());
    target.setStopOff(source.getStopOff());
    target.setAllInNoFm(source.getAllInNoFm());
    target.setAllInFmOneWay(source.getAllInFmOneWay());
    target.setAllInFmRound(source.getAllInFmRound());
    target.setWaitingFee(source.getWaitingFee());
    target.setRedelivery(source.getRedelivery());
    target.setPrepull(source.getPrepull());
    target.setNsLift(source.getNsLift());
    target.setOtherFee(source.getOtherFee());
    target.setRemark(source.getRemark());
    target.setValidDate(source.getValidDate());
    target.setStatus(source.getStatus());
    target.setLogYardNameAddress(source.getLogYardNameAddress());
    if (source.getExtraFields() != null) {
      target.setExtraFields(new HashMap<>(source.getExtraFields()));
    }
    return target;
  }

  @Transactional
  public CostImportResult importExcel(MultipartFile file, Long templateId) throws IOException {
    return importExcel(file, templateId, false);
  }

  @Transactional
  public CostImportResult importExcel(MultipartFile file, Long templateId, boolean dryRun)
      throws IOException {
    CostTableTemplateLayout layout = templateService.resolveExportLayout("road", templateId);
    return CostExcelSupport.importRows(
        file,
        EXPORT_HEADERS,
        (row) -> mapImportRow(row, layout),
        (entity) -> {
          // 先把客商简称规范成全称，再跑公式与其它校验
          String partyError = masterRefValidator.resolveSupplierTruck(entity);
          if (partyError != null) {
            return partyError;
          }
          String enrichError = tryEnrichZipFromCityState(entity);
          if (enrichError != null) {
            return enrichError;
          }
          String formulaError = tryApplyAllInFormulas(entity, false);
          if (formulaError != null) {
            return formulaError;
          }
          return validateImportRow(entity, layout);
        },
        (rowNum, entity) -> {
          entity.touch();
          repository.save(entity);
        },
        dryRun);
  }

  public byte[] exportExcel(
      String zipCode,
      String city,
      String state,
      String por,
      String pol,
      String supplier,
      BigDecimal redelivery,
      String validDate,
      String status,
      Long templateId,
      List<Long> ids) {
    List<CostRoad> items;
    if (RequestIds.present(ids)) {
      items =
          repository.findAllById(ids).stream()
              .sorted(Comparator.comparing(CostRoad::getId))
              .toList();
    } else {
      String statusFilter = status;
      boolean filterStatus = statusFilter != null && !statusFilter.isBlank();
      var pageable = Pageable.unpaged(Sort.by(Sort.Direction.ASC, "id"));
      items =
          repository
              .search(
                  SearchText.orEmpty(zipCode),
                  SearchText.orEmpty(city),
                  SearchText.orEmpty(state),
                  SearchText.orEmpty(por),
                  SearchText.orEmpty(pol),
                  SearchText.orEmpty(supplier),
                  redelivery,
                  SearchText.orEmpty(validDate),
                  pageable)
              .getContent();
      if (filterStatus) {
        items =
            items.stream()
                .filter(
                    item ->
                        CostValidityStatus.matchesFilter(
                            item.getStatus(), statusFilter, item.getValidDate()))
                .sorted(Comparator.comparing(CostRoad::getId))
                .toList();
      }
    }

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

  private CostRoad mapImportRow(Row row, CostTableTemplateLayout layout) {
    var headers = CostExcelSupport.readHeaderMap(row.getSheet().getRow(0));
    CostRoad entity = new CostRoad();
    entity.setZipCode(
        CostMasterRefValidator.normalizeToken(
            CostExcelSupport.readByHeader(
                row, headers, "邮编", "*邮编", "ZIP CODE", "*ZIP CODE", "ZIPCODE")));
    entity.setCity(
        CostMasterRefValidator.normalizeToken(
            CostExcelSupport.readByHeader(row, headers, "城市", "*城市", "City", "CITY", "*CITY")));
    String state =
        CostMasterRefValidator.normalizeToken(
            CostExcelSupport.readByHeader(row, headers, "州", "*州", "State", "STATE", "*STATE"));
    entity.setState(state.isEmpty() ? state : state.toUpperCase(java.util.Locale.ROOT));
    entity.setPor(
        CostMasterRefValidator.normalizeToken(
            CostExcelSupport.readByHeader(row, headers, "接货地", "*接货地", "POR", "*POR")));
    entity.setPol(
        CostMasterRefValidator.normalizeToken(
            CostExcelSupport.readByHeader(row, headers, "卸货港", "POL", "*POL")));
    entity.setSupplier(
        CostMasterRefValidator.normalizeToken(
            CostExcelSupport.readByHeader(
                row, headers, "卡车供应商", "*卡车供应商", "SUPPLIER", "*SUPPLIER")));
    entity.setBaseFreight(
        CostExcelSupport.readDecimalByHeader(
            row, headers, "基础", "BASE", "BASE FREIGHT", "*BASE", "*BASE FREIGHT"));
    entity.setFsc(
        CostExcelSupport.readPercentDecimalByHeader(
            row, headers, "燃油", "FSC (%)", "FSC", "*FSC"));
    entity.setChassis(
        CostExcelSupport.readDecimalByHeader(row, headers, "车架", "CHASSIS"));
    entity.setTriTandemAxle(
        CostExcelSupport.readDecimalByHeader(
            row,
            headers,
            "超重",
            "OW",
            "OW/TRI-AXCEL",
            "OW/TRI-AXLE",
            "TRI/TANDEM AXLE",
            "TRI TANDEM AXLE"));
    entity.setSplit(CostExcelSupport.readDecimalByHeader(row, headers, "分离", "SPLIT"));
    entity.setStopOff(
        CostExcelSupport.readDecimalByHeader(row, headers, "停留", "STOP OFF", "STOP OFF FEE"));
    entity.setAllInNoFm(
        CostExcelSupport.readDecimalByHeader(
            row, headers, "总价 非熏蒸", "ALL IN", "ALL IN - NO FM", "*ALL IN - NO FM"));
    entity.setAllInFmOneWay(
        CostExcelSupport.readDecimalByHeader(
            row,
            headers,
            "总价 熏非橡",
            "ALL IN FM NON OAK",
            "ALL IN - FM (NON OAK)",
            "*ALL IN - FM (NON OAK)",
            "ALL IN - FM ONE WAY",
            "*ALL IN - FM ONE WAY"));
    entity.setAllInFmRound(
        CostExcelSupport.readDecimalByHeader(
            row,
            headers,
            "总价 熏橡",
            "ALL IN FM OAK",
            "ALL IN - FM (OAK)",
            "*ALL IN - FM (OAK)",
            "ALL IN - FM ROUND",
            "*ALL IN - FM ROUND"));
    entity.setWaitingFee(
        CostExcelSupport.readDecimalByHeader(row, headers, "待时费", "WAITING FEE", "WAITING"));
    entity.setRedelivery(
        CostExcelSupport.readDecimalByHeader(row, headers, "后段运费", "REDELIVERY"));
    entity.setPrepull(CostExcelSupport.readDecimalByHeader(row, headers, "预提费", "PREPULL"));
    entity.setNsLift(
        CostExcelSupport.readDecimalByHeader(
            row, headers, "上下车费", "LIFT", "NS LIFT", "TO LIFT"));
    entity.setOtherFee(
        CostExcelSupport.readDecimalByHeader(row, headers, "其他费", "OTHERS", "OTHER FEE"));
    entity.setRemark(CostExcelSupport.readByHeader(row, headers, "备注", "REMARK"));
    entity.setValidDate(
        CostExcelSupport.readByHeader(
            row, headers, "有效期", "VALID TIME", "VALID DATE", "*VALID DATE", "*VALID TIME"));
    entity.setLogYardNameAddress(
        CostExcelSupport.readByHeader(
            row,
            headers,
            "堆场地址",
            "PICK UP ADDRESS",
            "PICK UP ADRRESS",
            "LOG YARD NAME & ADDRESS",
            "LOG YARD NAME / ADDRESS",
            "LOG YARD NAME &ADDRESS",
            "LOG YARD NAME",
            "LOG YARD"));
    Map<String, Object> extra =
        entity.getExtraFields() == null ? new HashMap<>() : new HashMap<>(entity.getExtraFields());
    putExtraDecimal(
        extra,
        "cf_road_yard_storage",
        CostExcelSupport.readDecimalByHeader(row, headers, "堆存费", "YARD STORAGE"));
    putExtraText(
        extra,
        "cf_road_yard_storage_unit",
        CostExcelSupport.readByHeader(
            row, headers, "堆存单位", "YARD STORAGE UNIT", "YARD STORAGE 单位"));
    putExtraDecimal(
        extra,
        "cf_road_extra_chassis",
        CostExcelSupport.readDecimalByHeader(row, headers, "额外车架费", "EXTRA CHASSIS"));
    putExtraText(
        extra,
        "cf_road_extra_chassis_unit",
        CostExcelSupport.readByHeader(
            row, headers, "额外车架单位", "EXTRA CHASSIS UNIT", "EXTRA CHASSIS 单位"));
    putExtraText(
        extra,
        "cf_road_waiting_unit",
        CostExcelSupport.readByHeader(
            row, headers, "待时单位", "WAITING UNIT", "WAITING 单位", "WAITING FEE UNIT"));
    String effective =
        CostExcelSupport.readByHeader(
            row, headers, "生效期", "EFFECTIVE TIME", "EFFECTIVE DATE");
    if (effective != null && !effective.isBlank()) {
      extra.put("cf_road_eff", effective.trim());
    }
    CostTemplateImportSupport.applyCustomFields("road", layout, row, headers, extra);
    entity.setExtraFields(extra);
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

  private static void putExtraDecimal(
      Map<String, Object> extra, String field, BigDecimal value) {
    if (value != null) {
      extra.put(field, value);
    }
  }

  private static void putExtraText(Map<String, Object> extra, String field, String value) {
    if (value != null && !value.isBlank()) {
      extra.put(field, value.trim().replaceFirst("^/+", ""));
    }
  }

  /**
   * 导入缺 ZIP、但有 City+State 时尝试主数据补全。唯一匹配写入 zip；多个邮编「待补录」；未找到「CITY、STATE有误」。
   */
  private String tryEnrichZipFromCityState(CostRoad entity) {
    if (!isBlank(entity.getZipCode())) {
      return null;
    }
    if (isBlank(entity.getCity()) || isBlank(entity.getState())) {
      return null;
    }
    DestZipResolveItemResponse resolved =
        destAddressService.resolveRouteFields(
            entity.getCity(), entity.getState(), entity.getZipCode());
    if (resolved.canonicalCity() != null && !resolved.canonicalCity().isBlank()) {
      entity.setCity(resolved.canonicalCity());
    }
    if ("unique".equals(resolved.status()) && !isBlank(resolved.zipCode())) {
      entity.setZipCode(resolved.zipCode());
      return null;
    }
    if ("ambiguous".equals(resolved.status())) {
      entity.setZipCode(CostRoadZipPlaceholder.PENDING);
      return null;
    }
    if ("notFound".equals(resolved.status())) {
      entity.setZipCode(CostRoadZipPlaceholder.CITY_STATE_INVALID);
      return null;
    }
    return null;
  }

  private String validateImportRow(CostRoad entity, CostTableTemplateLayout layout) {
    String required =
        CostTemplateImportSupport.validateRequired(
            "road", layout, (field) -> roadFieldValue(entity, field));
    if (required != null) {
      return required;
    }
    return masterRefValidator.validateRoad(entity);
  }

  private void validateEntityRequired(CostRoad entity, Long templateId) {
    CostTableTemplateLayout layout = templateService.resolveExportLayout("road", templateId);
    String error =
        CostTemplateImportSupport.validateRequired(
            "road", layout, (field) -> roadFieldValue(entity, field));
    if (error != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
    }
  }

  private void validateMasterRefs(CostRoad entity) {
    String error = masterRefValidator.validateRoad(entity);
    if (error != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
    }
  }

  private Object roadFieldValue(CostRoad entity, String field) {
    if (field != null && field.startsWith("cf_")) {
      return entity.getExtraFields() == null ? null : entity.getExtraFields().get(field);
    }
    return switch (field) {
      case "zipCode" -> entity.getZipCode();
      case "city" -> entity.getCity();
      case "state" -> entity.getState();
      case "por" -> entity.getPor();
      case "pol" -> entity.getPol();
      case "supplier" -> entity.getSupplier();
      case "baseFreight" -> entity.getBaseFreight();
      case "fsc" -> entity.getFsc();
      case "chassis" -> entity.getChassis();
      case "triTandemAxle" -> entity.getTriTandemAxle();
      case "split" -> entity.getSplit();
      case "stopOff" -> entity.getStopOff();
      case "allInNoFm" -> entity.getAllInNoFm();
      case "allInFmOneWay" -> entity.getAllInFmOneWay();
      case "allInFmRound" -> entity.getAllInFmRound();
      case "waitingFee" -> entity.getWaitingFee();
      case "redelivery" -> entity.getRedelivery();
      case "prepull" -> entity.getPrepull();
      case "nsLift" -> entity.getNsLift();
      case "otherFee" -> entity.getOtherFee();
      case "remark" -> entity.getRemark();
      case "validDate" -> entity.getValidDate();
      case "logYardNameAddress" -> entity.getLogYardNameAddress();
      default -> null;
    };
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
        .findByCategoryAndNameOrShortName("TRUCK", entity.getSupplier().trim())
        .map(
            supplier -> {
              entity.setSupplier(supplier.getName());
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
    // null 表示未传（保留旧值）；空 map 表示整包清空
    if (request.extraFields() != null) {
      entity.setExtraFields(new LinkedHashMap<>(request.extraFields()));
    }
  }

  private CostRoad requireEntity(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在"));
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
