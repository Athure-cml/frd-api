package com.furuiduo.quote.cost.service;

import java.io.IOException;
import java.math.BigDecimal;
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
import com.furuiduo.quote.cost.dto.CostSeaBatchCopyRequest;
import com.furuiduo.quote.cost.dto.CostTableTemplateLayout;
import com.furuiduo.quote.cost.dto.FreightCostResponse;
import com.furuiduo.quote.cost.dto.FreightCostSaveRequest;
import com.furuiduo.quote.cost.entity.CostSea;
import com.furuiduo.quote.cost.entity.CostStatus;
import com.furuiduo.quote.cost.repository.CostSeaRepository;
import com.furuiduo.quote.cost.support.CostDataExcelExporter;
import com.furuiduo.quote.cost.support.CostExcelSupport;
import com.furuiduo.quote.cost.support.CostMasterRefValidator;
import com.furuiduo.quote.cost.support.CostTemplateImportSupport;
import com.furuiduo.quote.cost.support.CostValidityStatus;
import com.furuiduo.quote.cost.support.SeaAllInCalculator;

@Service
public class CostSeaService {

  private static final String CF_FREIGHT_EFF = "cf_sea_freight_eff";
  private static final String CF_BUNKER_EFF = "cf_sea_bunker_eff";
  private static final String CF_OTHERS_EFF = "cf_sea_others_eff";

  private static final String[] IMPORT_HEADERS = {
    "POR",
    "POL",
    "POD",
    "中文简称",
    "英文品名",
    "箱型",
    "运费",
    "有效期",
    "BUC",
    "BUC有效期",
    "EBS",
    "EBS有效期",
    "GRI",
    "GRI有效期",
    "OTHERS",
    "OTHERS有效期",
    "ALL IN (小计)",
    "SSL (船公司)",
    "AGENT (代理)",
    "REMARK 备注"
  };

  private final CostSeaRepository repository;
  private final CostGridTemplateService templateService;
  private final CostMasterRefValidator masterRefValidator;

  public CostSeaService(
      CostSeaRepository repository,
      CostGridTemplateService templateService,
      CostMasterRefValidator masterRefValidator) {
    this.repository = repository;
    this.templateService = templateService;
    this.masterRefValidator = masterRefValidator;
  }

  public PageResult<FreightCostResponse> list(
      int page,
      int pageSize,
      String por,
      String pol,
      String pod,
      String ssl,
      String containerType,
      String agent,
      String freightValidDate,
      String freightEffDate,
      String status) {
    int safePage = Math.max(page, 1);
    int safePageSize = Math.min(Math.max(pageSize, 1), 200);
    String p = SearchText.orEmpty(por);
    String pl = SearchText.orEmpty(pol);
    String pd = SearchText.orEmpty(pod);
    String s = SearchText.orEmpty(ssl);
    String ct = SearchText.orEmpty(containerType);
    String a = SearchText.orEmpty(agent);
    String fvd = SearchText.orEmpty(freightValidDate);
    String fed = SearchText.orEmpty(freightEffDate);
    String statusFilter = status;
    boolean filterStatus = statusFilter != null && !statusFilter.isBlank();
    boolean filterFreightEff = !fed.isEmpty();

    if (!filterStatus && !filterFreightEff) {
      var pageable =
          PageRequest.of(safePage - 1, safePageSize, Sort.by(Sort.Direction.DESC, "id"));
      Page<CostSea> result = repository.search(p, pl, pd, s, ct, a, fvd, pageable);
      return new PageResult<>(
          result.getContent().stream().map(FreightCostResponse::fromSea).toList(),
          result.getTotalElements());
    }

    var pageable = Pageable.unpaged(Sort.by(Sort.Direction.DESC, "id"));
    List<CostSea> filtered =
        repository.search(p, pl, pd, s, ct, a, fvd, pageable).getContent().stream()
            .filter(item -> matchesFreightEffDate(item, fed))
            .filter(
                item ->
                    CostValidityStatus.matchesFilter(
                        item.getStatus(), statusFilter, item.getFreightValidDate()))
            .toList();
    return paginate(filtered, safePage, safePageSize);
  }

  public FreightCostResponse getById(Long id) {
    return FreightCostResponse.fromSea(requireEntity(id));
  }

  @Transactional
  public FreightCostResponse create(FreightCostSaveRequest request) {
    CostSea entity = new CostSea();
    applySave(entity, request);
    validateEntityRequired(entity, null);
    validateMasterRefs(entity);
    entity.touch();
    return FreightCostResponse.fromSea(repository.save(entity));
  }

  @Transactional
  public FreightCostResponse update(Long id, FreightCostSaveRequest request) {
    CostSea entity = requireEntity(id);
    applySave(entity, request);
    validateEntityRequired(entity, null);
    validateMasterRefs(entity);
    entity.touch();
    return FreightCostResponse.fromSea(repository.save(entity));
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
  public int batchCopy(CostSeaBatchCopyRequest request) {
    if (request.ids() == null || request.ids().isEmpty()) {
      return 0;
    }
    boolean applyOverrides = Boolean.TRUE.equals(request.applyOverrides());
    if (applyOverrides && !hasAnySeaCopyOverride(request)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请至少填写一项要统一修改的字段");
    }
    int created = 0;
    for (Long id : request.ids()) {
      CostSea source = requireEntity(id);
      CostSea copy = copyOf(source);
      if (applyOverrides) {
        applySeaCopyOverrides(copy, request);
      }
      copy.touch();
      repository.save(copy);
      created++;
    }
    return created;
  }

  @Transactional
  public int batchUpdate(CostBatchUpdateRequest request) {
    if (request.ids() == null || request.ids().isEmpty()) {
      return 0;
    }
    Map<String, Object> fields = request.fields() == null ? Map.of() : request.fields();
    int updated = 0;
    for (Long id : request.ids()) {
      CostSea entity = requireEntity(id);

      if (fields.containsKey("freight")) {
        entity.setFreight(asDecimal(fields.get("freight")));
      }
      if (fields.containsKey("containerType")) {
        entity.setContainerType(asString(fields.get("containerType")));
      }
      if (fields.containsKey("freightValidDate")) {
        entity.setFreightValidDate(asString(fields.get("freightValidDate")));
      }
      if (fields.containsKey("buc")) {
        entity.setBuc(asDecimal(fields.get("buc")));
      }
      if (fields.containsKey("bucValidDate")) {
        entity.setBucValidDate(asString(fields.get("bucValidDate")));
      }
      if (fields.containsKey("others")) {
        entity.setOthers(asDecimal(fields.get("others")));
      }
      if (fields.containsKey("othersValidDate")) {
        entity.setOthersValidDate(asString(fields.get("othersValidDate")));
      }

      Map<String, Object> extra =
          entity.getExtraFields() == null
              ? new HashMap<>()
              : new HashMap<>(entity.getExtraFields());
      boolean extraChanged = false;
      if (fields.containsKey("freightEffDate")) {
        extra.put(CF_FREIGHT_EFF, asString(fields.get("freightEffDate")));
        extraChanged = true;
      }
      if (fields.containsKey("bucEffDate")) {
        extra.put(CF_BUNKER_EFF, asString(fields.get("bucEffDate")));
        extraChanged = true;
      }
      if (fields.containsKey("othersEffDate")) {
        extra.put(CF_OTHERS_EFF, asString(fields.get("othersEffDate")));
        extraChanged = true;
      }
      if (extraChanged) {
        entity.setExtraFields(extra);
      }

      // 兼容旧批量字段
      if (fields.containsKey("agent")) {
        entity.setAgent(asString(fields.get("agent")));
      }
      if (fields.containsKey("remark")) {
        entity.setRemark(asString(fields.get("remark")));
      }

      // ALL IN 始终按公式重算，不接受手工覆盖
      entity.setAllIn(SeaAllInCalculator.compute(entity));
      if (fields.containsKey("freightValidDate")) {
        entity.setStatus(
            CostValidityStatus.resolve(CostStatus.active, entity.getFreightValidDate()));
      }
      if (fields.containsKey("containerType") || fields.containsKey("agent")) {
        validateMasterRefs(entity);
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
    CostTableTemplateLayout layout = templateService.resolveExportLayout("sea", templateId);
    return CostExcelSupport.importRows(
        file,
        IMPORT_HEADERS,
        (row) -> mapImportRow(row, layout),
        (entity) -> {
          String required =
              CostTemplateImportSupport.validateRequired(
                  "sea", layout, (field) -> seaFieldValue(entity, field));
          if (required != null) {
            return required;
          }
          return masterRefValidator.validateSea(entity);
        },
        (rowNum, entity) -> {
          entity.setAllIn(SeaAllInCalculator.compute(entity));
          entity.touch();
          repository.save(entity);
        },
        dryRun);
  }

  public byte[] exportExcel(
      String por,
      String pol,
      String pod,
      String ssl,
      String containerType,
      String agent,
      String freightValidDate,
      String freightEffDate,
      String status,
      Long templateId,
      List<Long> ids) {
    List<CostSea> items;
    if (RequestIds.present(ids)) {
      items =
          repository.findAllById(ids).stream()
              .sorted(Comparator.comparing(CostSea::getId))
              .toList();
    } else {
      String statusFilter = status;
      boolean filterStatus = statusFilter != null && !statusFilter.isBlank();
      String fed = SearchText.orEmpty(freightEffDate);
      boolean filterFreightEff = !fed.isEmpty();
      var pageable = Pageable.unpaged(Sort.by(Sort.Direction.ASC, "id"));
      items =
          repository
              .search(
                  SearchText.orEmpty(por),
                  SearchText.orEmpty(pol),
                  SearchText.orEmpty(pod),
                  SearchText.orEmpty(ssl),
                  SearchText.orEmpty(containerType),
                  SearchText.orEmpty(agent),
                  SearchText.orEmpty(freightValidDate),
                  pageable)
              .getContent();
      if (filterStatus || filterFreightEff) {
        items =
            items.stream()
                .filter(item -> matchesFreightEffDate(item, fed))
                .filter(
                    item ->
                        CostValidityStatus.matchesFilter(
                            item.getStatus(), statusFilter, item.getFreightValidDate()))
                .sorted(Comparator.comparing(CostSea::getId))
                .toList();
      }
    }

    var layout = templateService.resolveExportLayout("sea", templateId);
    return CostDataExcelExporter.exportSea(items, layout);
  }

  private boolean matchesFreightEffDate(CostSea item, String freightEffDate) {
    if (freightEffDate == null || freightEffDate.isBlank()) {
      return true;
    }
    Object value = null;
    if (item.getExtraFields() != null) {
      value = item.getExtraFields().get(CF_FREIGHT_EFF);
      if (value == null) {
        value = item.getExtraFields().get("cf_seaFreightEff");
      }
    }
    if (value == null) {
      return false;
    }
    return String.valueOf(value)
        .toLowerCase()
        .contains(freightEffDate.trim().toLowerCase());
  }

  private PageResult<FreightCostResponse> paginate(List<CostSea> filtered, int page, int pageSize) {
    int total = filtered.size();
    int fromIndex = (page - 1) * pageSize;
    if (fromIndex >= total) {
      return new PageResult<>(List.of(), total);
    }
    int toIndex = Math.min(fromIndex + pageSize, total);
    List<FreightCostResponse> items =
        filtered.subList(fromIndex, toIndex).stream().map(FreightCostResponse::fromSea).toList();
    return new PageResult<>(items, total);
  }

  private CostSea mapImportRow(Row row, CostTableTemplateLayout layout) {
    Sheet sheet = row.getSheet();
    Map<String, Integer> headers = CostExcelSupport.readHeaderMap(sheet.getRow(0));
    boolean nested =
        headers.containsKey(CostExcelSupport.normalizeHeader("附加费"));

    CostSea entity;
    if (nested) {
      entity = mapNestedImportRow(row);
    } else {
      String pol = readHeader(row, headers, "POL", "起运港", "ORIGIN");
      String pod = readHeader(row, headers, "POD", "目的港", "DESTINATION");
      if (pol.isBlank() && pod.isBlank()) {
        return null;
      }
      entity = new CostSea();
      entity.setPor(readHeader(row, headers, "POR"));
      entity.setPol(pol);
      entity.setPod(pod);
      entity.setCnShortName(readHeader(row, headers, "中文简称"));
      entity.setEnProductName(readHeader(row, headers, "英文品名"));
      entity.setContainerType(readHeader(row, headers, "箱型", "SPEC"));
      entity.setFreight(
          CostExcelSupport.readDecimalByHeader(row, headers, "运费", "O/F RATE (USD)", "UNIT PRICE"));
      entity.setFreightValidDate(readHeader(row, headers, "有效期", "FREIGHT VALID DATE", "VALID DATE"));
      entity.setBuc(CostExcelSupport.readDecimalByHeader(row, headers, "BUC", "燃油附加费"));
      entity.setBucValidDate(readHeader(row, headers, "BUC有效期", "附加费有效期"));
      entity.setEbs(CostExcelSupport.readDecimalByHeader(row, headers, "EBS"));
      entity.setEbsValidDate(readHeader(row, headers, "EBS有效期"));
      entity.setGri(CostExcelSupport.readDecimalByHeader(row, headers, "GRI"));
      entity.setGriValidDate(readHeader(row, headers, "GRI有效期"));
      entity.setOthers(CostExcelSupport.readDecimalByHeader(row, headers, "OTHERS"));
      entity.setOthersValidDate(readHeader(row, headers, "OTHERS有效期"));
      entity.setAllIn(
          CostExcelSupport.readDecimalByHeader(row, headers, "ALL IN (小计)", "ALL IN"));
      entity.setSsl(readHeader(row, headers, "SSL (船公司)", "SSL", "承运商", "CARRIER"));
      entity.setAgent(readHeader(row, headers, "AGENT (代理)", "AGENT"));
      entity.setRemark(readHeader(row, headers, "REMARK 备注", "备注", "REMARK"));
      entity.setStatus(CostValidityStatus.resolve(CostStatus.active, entity.getFreightValidDate()));
    }
    if (entity == null) {
      return null;
    }
    Map<String, Object> extra =
        entity.getExtraFields() == null ? new HashMap<>() : new HashMap<>(entity.getExtraFields());
    CostTemplateImportSupport.applyCustomFields("sea", layout, row, headers, extra);
    entity.setExtraFields(extra);
    return entity;
  }

  /** 双行表头：… | 附加费(BUC/有效期/EBS/有效期/GRI/有效期/OTHERS/有效期) | … */
  private CostSea mapNestedImportRow(Row row) {
    if (row.getRowNum() <= 1) {
      return null;
    }
    String pol = CostExcelSupport.cellString(row.getCell(1));
    String pod = CostExcelSupport.cellString(row.getCell(2));
    if (pol.isBlank() && pod.isBlank()) {
      return null;
    }
    CostSea entity = new CostSea();
    entity.setPor(CostExcelSupport.cellString(row.getCell(0)));
    entity.setPol(pol);
    entity.setPod(pod);
    entity.setCnShortName(CostExcelSupport.cellString(row.getCell(3)));
    entity.setEnProductName(CostExcelSupport.cellString(row.getCell(4)));
    entity.setContainerType(CostExcelSupport.cellString(row.getCell(5)));
    entity.setFreight(CostExcelSupport.cellDecimal(row.getCell(6)));
    entity.setFreightValidDate(CostExcelSupport.cellString(row.getCell(7)));
    entity.setBuc(CostExcelSupport.cellDecimal(row.getCell(8)));
    entity.setBucValidDate(CostExcelSupport.cellString(row.getCell(9)));
    entity.setEbs(CostExcelSupport.cellDecimal(row.getCell(10)));
    entity.setEbsValidDate(CostExcelSupport.cellString(row.getCell(11)));
    entity.setGri(CostExcelSupport.cellDecimal(row.getCell(12)));
    entity.setGriValidDate(CostExcelSupport.cellString(row.getCell(13)));
    entity.setOthers(CostExcelSupport.cellDecimal(row.getCell(14)));
    entity.setOthersValidDate(CostExcelSupport.cellString(row.getCell(15)));
    entity.setAllIn(CostExcelSupport.cellDecimal(row.getCell(16)));
    entity.setSsl(CostExcelSupport.cellString(row.getCell(17)));
    entity.setAgent(CostExcelSupport.cellString(row.getCell(18)));
    entity.setRemark(CostExcelSupport.cellString(row.getCell(19)));
    entity.setStatus(CostValidityStatus.resolve(CostStatus.active, entity.getFreightValidDate()));
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

  private void applySave(CostSea entity, FreightCostSaveRequest request) {
    entity.setPor(request.por());
    entity.setPol(request.pol());
    entity.setPod(request.pod());
    entity.setCnShortName(request.cnShortName());
    entity.setEnProductName(request.enProductName());
    entity.setContainerType(request.containerType());
    entity.setFreight(request.freight());
    entity.setFreightValidDate(request.freightValidDate());
    entity.setBuc(request.buc());
    entity.setBucValidDate(request.bucValidDate());
    entity.setEbs(request.ebs());
    entity.setEbsValidDate(request.ebsValidDate());
    entity.setGri(request.gri());
    entity.setGriValidDate(request.griValidDate());
    entity.setOthers(request.others());
    entity.setOthersValidDate(request.othersValidDate());
    entity.setSsl(request.ssl());
    entity.setAgent(request.agent());
    entity.setRemark(request.remark());
    if (request.allIn() != null) {
      entity.setAllIn(request.allIn());
    } else {
      entity.setAllIn(SeaAllInCalculator.compute(entity));
    }
    entity.setStatus(CostValidityStatus.resolve(CostStatus.active, request.freightValidDate()));
    if (request.extraFields() != null) {
      entity.setExtraFields(new LinkedHashMap<>(request.extraFields()));
    }
  }

  private CostSea copyOf(CostSea source) {
    CostSea target = new CostSea();
    target.setPor(source.getPor());
    target.setPol(source.getPol());
    target.setPod(source.getPod());
    target.setCnShortName(source.getCnShortName());
    target.setEnProductName(source.getEnProductName());
    target.setContainerType(source.getContainerType());
    target.setFreight(source.getFreight());
    target.setFreightValidDate(source.getFreightValidDate());
    target.setBuc(source.getBuc());
    target.setBucValidDate(source.getBucValidDate());
    target.setEbs(source.getEbs());
    target.setEbsValidDate(source.getEbsValidDate());
    target.setGri(source.getGri());
    target.setGriValidDate(source.getGriValidDate());
    target.setOthers(source.getOthers());
    target.setOthersValidDate(source.getOthersValidDate());
    target.setAllIn(source.getAllIn());
    target.setSsl(source.getSsl());
    target.setAgent(source.getAgent());
    target.setStatus(source.getStatus());
    target.setRemark(source.getRemark());
    if (source.getExtraFields() != null) {
      target.setExtraFields(new HashMap<>(source.getExtraFields()));
    }
    return target;
  }

  private void applySeaCopyOverrides(CostSea copy, CostSeaBatchCopyRequest request) {
    if (request.freight() != null) {
      copy.setFreight(request.freight());
    }
    if (request.containerType() != null && !request.containerType().isBlank()) {
      copy.setContainerType(request.containerType().trim());
    }
    if (request.freightValidDate() != null && !request.freightValidDate().isBlank()) {
      copy.setFreightValidDate(request.freightValidDate().trim());
    }
    if (request.buc() != null) {
      copy.setBuc(request.buc());
    }
    if (request.bucValidDate() != null && !request.bucValidDate().isBlank()) {
      copy.setBucValidDate(request.bucValidDate().trim());
    }
    if (request.others() != null) {
      copy.setOthers(request.others());
    }
    if (request.othersValidDate() != null && !request.othersValidDate().isBlank()) {
      copy.setOthersValidDate(request.othersValidDate().trim());
    }

    Map<String, Object> extra =
        copy.getExtraFields() == null ? new HashMap<>() : new HashMap<>(copy.getExtraFields());
    boolean extraChanged = false;
    if (request.freightEffDate() != null && !request.freightEffDate().isBlank()) {
      extra.put(CF_FREIGHT_EFF, request.freightEffDate().trim());
      extraChanged = true;
    }
    if (request.bucEffDate() != null && !request.bucEffDate().isBlank()) {
      extra.put(CF_BUNKER_EFF, request.bucEffDate().trim());
      extraChanged = true;
    }
    if (request.othersEffDate() != null && !request.othersEffDate().isBlank()) {
      extra.put(CF_OTHERS_EFF, request.othersEffDate().trim());
      extraChanged = true;
    }
    if (extraChanged) {
      copy.setExtraFields(extra);
    }

    // ALL IN 始终按公式重算，不接受手工覆盖
    copy.setAllIn(SeaAllInCalculator.compute(copy));
    copy.setStatus(CostValidityStatus.resolve(CostStatus.active, copy.getFreightValidDate()));
  }

  private boolean hasAnySeaCopyOverride(CostSeaBatchCopyRequest request) {
    return request.freight() != null
        || notBlank(request.containerType())
        || notBlank(request.freightEffDate())
        || notBlank(request.freightValidDate())
        || request.buc() != null
        || notBlank(request.bucEffDate())
        || notBlank(request.bucValidDate())
        || request.others() != null
        || notBlank(request.othersEffDate())
        || notBlank(request.othersValidDate());
  }

  private boolean notBlank(String value) {
    return value != null && !value.isBlank();
  }

  private Object seaFieldValue(CostSea entity, String field) {
    if (field != null && field.startsWith("cf_")) {
      return entity.getExtraFields() == null ? null : entity.getExtraFields().get(field);
    }
    return switch (field) {
      case "por" -> entity.getPor();
      case "pol" -> entity.getPol();
      case "pod" -> entity.getPod();
      case "cnShortName" -> entity.getCnShortName();
      case "enProductName" -> entity.getEnProductName();
      case "containerType" -> entity.getContainerType();
      case "freight" -> entity.getFreight();
      case "freightValidDate" -> entity.getFreightValidDate();
      case "buc" -> entity.getBuc();
      case "bucValidDate" -> entity.getBucValidDate();
      case "ebs" -> entity.getEbs();
      case "ebsValidDate" -> entity.getEbsValidDate();
      case "gri" -> entity.getGri();
      case "griValidDate" -> entity.getGriValidDate();
      case "others" -> entity.getOthers();
      case "othersValidDate" -> entity.getOthersValidDate();
      case "allIn" -> entity.getAllIn();
      case "ssl" -> entity.getSsl();
      case "agent" -> entity.getAgent();
      case "remark" -> entity.getRemark();
      default -> null;
    };
  }

  private void validateEntityRequired(CostSea entity, Long templateId) {
    CostTableTemplateLayout layout = templateService.resolveExportLayout("sea", templateId);
    String error =
        CostTemplateImportSupport.validateRequired(
            "sea", layout, (field) -> seaFieldValue(entity, field));
    if (error != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
    }
  }

  private void validateMasterRefs(CostSea entity) {
    String error = masterRefValidator.validateSea(entity);
    if (error != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
    }
  }

  private CostSea requireEntity(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在"));
  }

  private BigDecimal asDecimal(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    String text = String.valueOf(value).trim();
    if (text.isBlank()) {
      return null;
    }
    return new BigDecimal(text);
  }

  private String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }
}
