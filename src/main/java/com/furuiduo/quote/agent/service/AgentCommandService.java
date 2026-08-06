package com.furuiduo.quote.agent.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.agent.dto.AgentResponse;
import com.furuiduo.quote.agent.dto.AgentSaveRequest;
import com.furuiduo.quote.agent.entity.Agent;
import com.furuiduo.quote.agent.repository.AgentRepository;
import com.furuiduo.quote.agent.support.AgentCodeGenerator;
import com.furuiduo.quote.common.PartyMasterExcelSupport;
import com.furuiduo.quote.common.PartyMasterExcelSupport.StatusCell;
import com.furuiduo.quote.common.RequestIds;
import com.furuiduo.quote.common.SearchText;
import com.furuiduo.quote.cost.dto.CostImportResult;
import com.furuiduo.quote.cost.support.CostExcelSupport;
import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class AgentCommandService {

  private static final String[] EXPORT_HEADERS = {"编码", "名称", "邮箱", "备注", "状态"};

  private final AgentRepository repository;
  private final AgentCodeGenerator codeGenerator;

  public AgentCommandService(AgentRepository repository, AgentCodeGenerator codeGenerator) {
    this.repository = repository;
    this.codeGenerator = codeGenerator;
  }

  @Transactional
  public AgentResponse create(SysUser user, AgentSaveRequest request) {
    validateSaveRequest(request, null);
    Agent entity = new Agent();
    entity.setCode(codeGenerator.next());
    entity.setCreatedBy(user.getId());
    entity.setCreatedByName(user.getRealName());
    entity.setDeptId(user.getDepartment() != null ? user.getDepartment().getId() : null);
    apply(entity, request);
    return AgentResponse.from(repository.save(entity));
  }

  @Transactional
  public AgentResponse update(Long id, AgentSaveRequest request) {
    validateSaveRequest(request, id);
    Agent entity =
        repository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "代理商不存在"));
    apply(entity, request);
    entity.setUpdatedAt(LocalDateTime.now());
    return AgentResponse.from(repository.save(entity));
  }

  @Transactional
  public void delete(Long id) {
    Agent entity =
        repository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "代理商不存在"));
    repository.delete(entity);
  }

  public AgentResponse getById(Long id) {
    return repository
        .findById(id)
        .map(AgentResponse::from)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "代理商不存在"));
  }

  @Transactional
  public CostImportResult importExcel(SysUser user, MultipartFile file) throws IOException {
    Set<String> seenNames = new HashSet<>();
    return CostExcelSupport.importRows(
        file,
        EXPORT_HEADERS,
        this::mapImportRow,
        (row) -> validateImportRow(row, seenNames),
        (rowNum, row) -> upsertImported(user, row));
  }

  @Transactional(readOnly = true)
  public byte[] exportExcel(String code, String name, Integer status, List<Long> ids) {
    List<Agent> items;
    if (RequestIds.present(ids)) {
      items =
          repository.findAllById(ids).stream()
              .sorted(
                  Comparator.comparing(
                      Agent::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
              .toList();
    } else {
      var pageable =
          PageRequest.of(0, 10_000, Sort.by(Sort.Direction.DESC, "updatedAt"));
      items =
          repository
              .search(SearchText.orEmpty(code), SearchText.orEmpty(name), status, pageable)
              .getContent();
    }
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Agents");
      CostExcelSupport.writeHeaderRow(sheet, EXPORT_HEADERS);
      int rowIndex = 1;
      for (Agent item : items) {
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getCode()));
        row.createCell(1).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getName()));
        row.createCell(2).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getEmail()));
        row.createCell(3).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getRemark()));
        row.createCell(4).setCellValue(PartyMasterExcelSupport.statusLabel(item.getStatus()));
      }
      return CostExcelSupport.writeWorkbook(workbook);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to export agents", ex);
    }
  }

  private void upsertImported(SysUser user, ImportRow row) {
    String name = PartyMasterExcelSupport.normalizeName(row.name());
    Agent entity = resolveForImport(row.code(), name);
    boolean creating = entity.getId() == null;
    if (creating) {
      assertNameAvailable(name, null);
      entity.setCode(codeGenerator.next());
      entity.setCreatedBy(user.getId());
      entity.setCreatedByName(user.getRealName());
      entity.setDeptId(user.getDepartment() != null ? user.getDepartment().getId() : null);
    } else {
      assertNameAvailable(name, entity.getId());
    }
    entity.setName(name);
    entity.setEmail(PartyMasterExcelSupport.trimToNull(row.email()));
    entity.setRemark(PartyMasterExcelSupport.trimToNull(row.remark()));
    entity.setStatus(
        PartyMasterExcelSupport.resolveStatus(row.status(), creating ? null : entity.getStatus()));
    entity.setUpdatedAt(LocalDateTime.now());
    repository.save(entity);
  }

  /** 仅当编码命中时更新；名称已存在且编码未命中则拒绝。 */
  private Agent resolveForImport(String code, String name) {
    if (code != null && !code.isBlank()) {
      Agent byCode = repository.findByCode(code.trim()).orElse(null);
      if (byCode != null) {
        return byCode;
      }
    }
    if (repository.existsByNameNormalized(name, null)) {
      throw new IllegalArgumentException("代理商名称已存在：" + name + "（如需更新请填写正确编码）");
    }
    return new Agent();
  }

  private ImportRow mapImportRow(Row row) {
    Map<String, Integer> headers = CostExcelSupport.readHeaderMap(row.getSheet().getRow(0));
    String code = CostExcelSupport.readByHeader(row, headers, "编码", "Code");
    String name = CostExcelSupport.readByHeader(row, headers, "名称", "Name", "代理商名称");
    String email = CostExcelSupport.readByHeader(row, headers, "邮箱", "Email");
    String remark = CostExcelSupport.readByHeader(row, headers, "备注", "Remark");
    String statusRaw = CostExcelSupport.readByHeader(row, headers, "状态", "Status");
    if (code.isBlank()
        && name.isBlank()
        && email.isBlank()
        && remark.isBlank()
        && statusRaw.isBlank()) {
      return null;
    }
    return new ImportRow(
        code, name, email, remark, PartyMasterExcelSupport.parseStatusCell(statusRaw));
  }

  private String validateImportRow(ImportRow row, Set<String> seenNames) {
    if (row.name() == null || row.name().isBlank()) {
      return "名称不能为空";
    }
    if (row.status() != null && row.status().isUnrecognized()) {
      return "状态无效（请填启用/停用或 1/0）";
    }
    String key = PartyMasterExcelSupport.nameKey(row.name());
    if (!seenNames.add(key)) {
      return "名称与文件中其他行重复";
    }
    return null;
  }

  private void validateSaveRequest(AgentSaveRequest request, Long excludeId) {
    if (request.name() == null || request.name().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "代理商名称不能为空");
    }
    if (request.status() == null || (request.status() != 0 && request.status() != 1)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的代理商状态");
    }
    assertNameAvailable(PartyMasterExcelSupport.normalizeName(request.name()), excludeId);
  }

  private void assertNameAvailable(String name, Long excludeId) {
    if (repository.existsByNameNormalized(name, excludeId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "代理商名称已存在：" + name);
    }
  }

  private void apply(Agent entity, AgentSaveRequest request) {
    entity.setName(PartyMasterExcelSupport.normalizeName(request.name()));
    entity.setEmail(PartyMasterExcelSupport.trimToNull(request.email()));
    entity.setRemark(PartyMasterExcelSupport.trimToNull(request.remark()));
    entity.setStatus(request.status());
    entity.setUpdatedAt(LocalDateTime.now());
  }

  private record ImportRow(
      String code, String name, String email, String remark, StatusCell status) {}
}
