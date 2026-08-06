package com.furuiduo.quote.cost.service;

import java.io.IOException;
import java.math.BigDecimal;
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
import com.furuiduo.quote.cost.dto.FreightCostResponse;
import com.furuiduo.quote.cost.dto.FreightCostSaveRequest;
import com.furuiduo.quote.cost.entity.CostSea;
import com.furuiduo.quote.cost.entity.CostStatus;
import com.furuiduo.quote.cost.repository.CostSeaRepository;
import com.furuiduo.quote.cost.support.CostExcelSupport;
import com.furuiduo.quote.cost.support.CostValidityStatus;
import com.furuiduo.quote.cost.support.SeaAllInCalculator;
import com.furuiduo.quote.cost.support.SeaCostExcelExporter;

@Service
public class CostSeaService {

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

  public CostSeaService(
      CostSeaRepository repository, CostGridTemplateService templateService) {
    this.repository = repository;
    this.templateService = templateService;
  }

  public PageResult<FreightCostResponse> list(
      int page, int pageSize, String por, String pol, String pod, String ssl, String status) {
    int safePage = Math.max(page, 1);
    int safePageSize = Math.min(Math.max(pageSize, 1), 200);

    List<CostSea> filtered =
        repository.findAll().stream()
            .filter(item -> contains(item.getPor(), por))
            .filter(item -> contains(item.getPol(), pol))
            .filter(item -> contains(item.getPod(), pod))
            .filter(item -> contains(item.getSsl(), ssl))
            .filter(
                item ->
                    CostValidityStatus.matchesFilter(
                        item.getStatus(), status, item.getFreightValidDate()))
            .sorted(Comparator.comparing(CostSea::getId).reversed())
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
    entity.touch();
    return FreightCostResponse.fromSea(repository.save(entity));
  }

  @Transactional
  public FreightCostResponse update(Long id, FreightCostSaveRequest request) {
    CostSea entity = requireEntity(id);
    applySave(entity, request);
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
  public int batchUpdate(CostBatchUpdateRequest request) {
    if (request.ids() == null || request.ids().isEmpty()) {
      return 0;
    }
    Map<String, Object> fields = request.fields() == null ? Map.of() : request.fields();
    int updated = 0;
    for (Long id : request.ids()) {
      CostSea entity = requireEntity(id);
      if (fields.containsKey("freightValidDate")) {
        entity.setFreightValidDate(asString(fields.get("freightValidDate")));
      }
      if (fields.containsKey("buc")) {
        entity.setBuc(asDecimal(fields.get("buc")));
      }
      if (fields.containsKey("bucValidDate")) {
        entity.setBucValidDate(asString(fields.get("bucValidDate")));
      }
      if (fields.containsKey("agent")) {
        entity.setAgent(asString(fields.get("agent")));
      }
      if (fields.containsKey("remark")) {
        entity.setRemark(asString(fields.get("remark")));
      }
      entity.setAllIn(SeaAllInCalculator.compute(entity));
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
        entity -> null,
        (rowNum, entity) -> {
          entity.setAllIn(SeaAllInCalculator.compute(entity));
          entity.touch();
          repository.save(entity);
        });
  }

  public byte[] exportExcel(
      String por, String pol, String pod, String ssl, String status, Long templateId, List<Long> ids) {
    List<CostSea> items =
        repository.findAll().stream()
            .filter(item -> !RequestIds.present(ids) || ids.contains(item.getId()))
            .filter(item -> RequestIds.present(ids) || contains(item.getPor(), por))
            .filter(item -> RequestIds.present(ids) || contains(item.getPol(), pol))
            .filter(item -> RequestIds.present(ids) || contains(item.getPod(), pod))
            .filter(item -> RequestIds.present(ids) || contains(item.getSsl(), ssl))
            .filter(
                item ->
                    RequestIds.present(ids)
                        || CostValidityStatus.matchesFilter(
                            item.getStatus(), status, item.getFreightValidDate()))
            .sorted(Comparator.comparing(CostSea::getId))
            .toList();

    templateService.resolveExportLayout("sea", templateId);
    return SeaCostExcelExporter.export(items);
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

  private CostSea mapImportRow(Row row) {
    Sheet sheet = row.getSheet();
    Map<String, Integer> headers = CostExcelSupport.readHeaderMap(sheet.getRow(0));
    boolean nested =
        headers.containsKey(CostExcelSupport.normalizeHeader("附加费"));

    if (nested) {
      return mapNestedImportRow(row);
    }

    String pol = readHeader(row, headers, "POL", "起运港", "ORIGIN");
    String pod = readHeader(row, headers, "POD", "目的港", "DESTINATION");
    if (pol.isBlank() && pod.isBlank()) {
      return null;
    }
    CostSea entity = new CostSea();
    entity.setPor(readHeader(row, headers, "POR"));
    entity.setPol(pol);
    entity.setPod(pod);
    entity.setCnShortName(readHeader(row, headers, "中文简称"));
    entity.setEnProductName(readHeader(row, headers, "英文品名"));
    entity.setContainerType(readHeader(row, headers, "箱型", "SPEC"));
    entity.setFreight(
        CostExcelSupport.readDecimalByHeader(row, headers, "运费", "O/F RATE (USD)", "UNIT PRICE"));
    entity.setFreightValidDate(readHeader(row, headers, "有效期", "FREIGHT VALID DATE", "VALID DATE"));
    entity.setBuc(CostExcelSupport.readDecimalByHeader(row, headers, "BUC"));
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
      entity.setExtraFields(request.extraFields());
    }
  }

  private CostSea requireEntity(Long id) {
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
