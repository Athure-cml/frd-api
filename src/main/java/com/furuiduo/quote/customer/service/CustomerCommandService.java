package com.furuiduo.quote.customer.service;

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

import com.furuiduo.quote.common.PartyMasterExcelSupport;
import com.furuiduo.quote.common.PartyMasterExcelSupport.StatusCell;
import com.furuiduo.quote.common.RequestIds;
import com.furuiduo.quote.common.SearchText;
import com.furuiduo.quote.cost.dto.CostImportResult;
import com.furuiduo.quote.cost.support.CostExcelSupport;
import com.furuiduo.quote.customer.dto.CustomerResponse;
import com.furuiduo.quote.customer.dto.CustomerSaveRequest;
import com.furuiduo.quote.customer.entity.Customer;
import com.furuiduo.quote.customer.repository.CustomerRepository;
import com.furuiduo.quote.customer.support.CustomerCodeGenerator;
import com.furuiduo.quote.quote.repository.QuoteOrderRepository;
import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class CustomerCommandService {

  private static final String[] EXPORT_HEADERS = {
    "编码", "名称", "联系人", "电话", "邮箱", "地址", "备注", "状态"
  };

  private final CustomerRepository customerRepository;
  private final QuoteOrderRepository quoteOrderRepository;
  private final CustomerCodeGenerator customerCodeGenerator;

  public CustomerCommandService(
      CustomerRepository customerRepository,
      QuoteOrderRepository quoteOrderRepository,
      CustomerCodeGenerator customerCodeGenerator) {
    this.customerRepository = customerRepository;
    this.quoteOrderRepository = quoteOrderRepository;
    this.customerCodeGenerator = customerCodeGenerator;
  }

  @Transactional
  public CustomerResponse create(SysUser user, CustomerSaveRequest request) {
    validateSaveRequest(request, null);
    Customer customer = new Customer();
    customer.setCode(customerCodeGenerator.next());
    customer.setCreatedBy(user.getId());
    customer.setCreatedByName(user.getRealName());
    customer.setDeptId(user.getDepartment() != null ? user.getDepartment().getId() : null);
    apply(customer, request);
    return CustomerResponse.from(customerRepository.save(customer));
  }

  @Transactional
  public CustomerResponse update(Long id, CustomerSaveRequest request) {
    validateSaveRequest(request, id);
    Customer customer =
        customerRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "客户不存在"));
    apply(customer, request);
    customer.setUpdatedAt(LocalDateTime.now());
    return CustomerResponse.from(customerRepository.save(customer));
  }

  @Transactional
  public void delete(Long id) {
    Customer customer =
        customerRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "客户不存在"));
    if (quoteOrderRepository.countByCustomerId(id) > 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该客户已关联报价单，无法删除");
    }
    customerRepository.delete(customer);
  }

  public CustomerResponse getById(Long id) {
    return customerRepository
        .findById(id)
        .map(CustomerResponse::from)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "客户不存在"));
  }

  public Customer requireEnabled(Long id) {
    Customer customer =
        customerRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "客户不存在"));
    if (customer.getStatus() == null || customer.getStatus() != 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "客户已停用，无法选用");
    }
    return customer;
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
    List<Customer> items;
    if (RequestIds.present(ids)) {
      items =
          customerRepository.findAllById(ids).stream()
              .sorted(
                  Comparator.comparing(
                      Customer::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
              .toList();
    } else {
      var pageable =
          PageRequest.of(0, 10_000, Sort.by(Sort.Direction.DESC, "updatedAt"));
      items =
          customerRepository
              .search(SearchText.orEmpty(code), SearchText.orEmpty(name), status, pageable)
              .getContent();
    }
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Customers");
      CostExcelSupport.writeHeaderRow(sheet, EXPORT_HEADERS);
      int rowIndex = 1;
      for (Customer item : items) {
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getCode()));
        row.createCell(1).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getName()));
        row.createCell(2).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getContactName()));
        row.createCell(3).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getPhone()));
        row.createCell(4).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getEmail()));
        row.createCell(5).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getAddress()));
        row.createCell(6).setCellValue(PartyMasterExcelSupport.nullToEmpty(item.getRemark()));
        row.createCell(7).setCellValue(PartyMasterExcelSupport.statusLabel(item.getStatus()));
      }
      return CostExcelSupport.writeWorkbook(workbook);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to export customers", ex);
    }
  }

  private void upsertImported(SysUser user, ImportRow row) {
    String name = PartyMasterExcelSupport.normalizeName(row.name());
    Customer customer = resolveForImport(row.code(), name);
    boolean creating = customer.getId() == null;
    if (creating) {
      assertNameAvailable(name, null);
      customer.setCode(customerCodeGenerator.next());
      customer.setCreatedBy(user.getId());
      customer.setCreatedByName(user.getRealName());
      customer.setDeptId(user.getDepartment() != null ? user.getDepartment().getId() : null);
    } else {
      assertNameAvailable(name, customer.getId());
    }
    customer.setName(name);
    customer.setContactName(PartyMasterExcelSupport.trimToNull(row.contactName()));
    customer.setPhone(PartyMasterExcelSupport.trimToNull(row.phone()));
    customer.setEmail(PartyMasterExcelSupport.trimToNull(row.email()));
    customer.setAddress(PartyMasterExcelSupport.trimToNull(row.address()));
    customer.setRemark(PartyMasterExcelSupport.trimToNull(row.remark()));
    customer.setStatus(
        PartyMasterExcelSupport.resolveStatus(
            row.status(), creating ? null : customer.getStatus()));
    customer.setUpdatedAt(LocalDateTime.now());
    customerRepository.save(customer);
  }

  /**
   * 仅当编码命中时更新；名称已存在且编码未命中则拒绝，避免重复导入被当成成功。
   */
  private Customer resolveForImport(String code, String name) {
    if (code != null && !code.isBlank()) {
      Customer byCode = customerRepository.findByCode(code.trim()).orElse(null);
      if (byCode != null) {
        return byCode;
      }
    }
    if (customerRepository.existsByNameNormalized(name, null)) {
      throw new IllegalArgumentException("客户名称已存在：" + name + "（如需更新请填写正确编码）");
    }
    return new Customer();
  }

  private ImportRow mapImportRow(Row row) {
    Map<String, Integer> headers = CostExcelSupport.readHeaderMap(row.getSheet().getRow(0));
    String code = CostExcelSupport.readByHeader(row, headers, "编码", "Code");
    String name = CostExcelSupport.readByHeader(row, headers, "名称", "Name", "客户名称");
    String contactName =
        CostExcelSupport.readByHeader(row, headers, "联系人", "Contact", "Contact Name");
    String phone = CostExcelSupport.readByHeader(row, headers, "电话", "Phone");
    String email = CostExcelSupport.readByHeader(row, headers, "邮箱", "Email");
    String address = CostExcelSupport.readByHeader(row, headers, "地址", "Address");
    String remark = CostExcelSupport.readByHeader(row, headers, "备注", "Remark");
    String statusRaw = CostExcelSupport.readByHeader(row, headers, "状态", "Status");
    if (code.isBlank()
        && name.isBlank()
        && contactName.isBlank()
        && phone.isBlank()
        && email.isBlank()
        && address.isBlank()
        && remark.isBlank()
        && statusRaw.isBlank()) {
      return null;
    }
    return new ImportRow(
        code, name, contactName, phone, email, address, remark, PartyMasterExcelSupport.parseStatusCell(statusRaw));
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

  private void validateSaveRequest(CustomerSaveRequest request, Long excludeId) {
    if (request.name() == null || request.name().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "客户名称不能为空");
    }
    if (request.status() == null || (request.status() != 0 && request.status() != 1)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的客户状态");
    }
    assertNameAvailable(PartyMasterExcelSupport.normalizeName(request.name()), excludeId);
  }

  private void assertNameAvailable(String name, Long excludeId) {
    if (customerRepository.existsByNameNormalized(name, excludeId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "客户名称已存在：" + name);
    }
  }

  private void apply(Customer customer, CustomerSaveRequest request) {
    customer.setName(PartyMasterExcelSupport.normalizeName(request.name()));
    customer.setContactName(PartyMasterExcelSupport.trimToNull(request.contactName()));
    customer.setPhone(PartyMasterExcelSupport.trimToNull(request.phone()));
    customer.setEmail(PartyMasterExcelSupport.trimToNull(request.email()));
    customer.setAddress(PartyMasterExcelSupport.trimToNull(request.address()));
    customer.setRemark(PartyMasterExcelSupport.trimToNull(request.remark()));
    customer.setStatus(request.status());
    customer.setUpdatedAt(LocalDateTime.now());
  }

  private record ImportRow(
      String code,
      String name,
      String contactName,
      String phone,
      String email,
      String address,
      String remark,
      StatusCell status) {}
}
