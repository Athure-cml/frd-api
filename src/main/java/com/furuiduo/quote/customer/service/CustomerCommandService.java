package com.furuiduo.quote.customer.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.customer.dto.CustomerResponse;
import com.furuiduo.quote.customer.dto.CustomerSaveRequest;
import com.furuiduo.quote.customer.entity.Customer;
import com.furuiduo.quote.customer.repository.CustomerRepository;
import com.furuiduo.quote.customer.support.CustomerCodeGenerator;
import com.furuiduo.quote.quote.repository.QuoteOrderRepository;
import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class CustomerCommandService {

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
    validateSaveRequest(request);
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
    validateSaveRequest(request);
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

  private void validateSaveRequest(CustomerSaveRequest request) {
    if (request.name() == null || request.name().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "客户名称不能为空");
    }
    if (request.status() == null || (request.status() != 0 && request.status() != 1)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的客户状态");
    }
  }

  private void apply(Customer customer, CustomerSaveRequest request) {
    customer.setName(request.name().trim());
    customer.setContactName(trimToNull(request.contactName()));
    customer.setPhone(trimToNull(request.phone()));
    customer.setEmail(trimToNull(request.email()));
    customer.setAddress(trimToNull(request.address()));
    customer.setRemark(trimToNull(request.remark()));
    customer.setStatus(request.status());
    customer.setUpdatedAt(LocalDateTime.now());
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
