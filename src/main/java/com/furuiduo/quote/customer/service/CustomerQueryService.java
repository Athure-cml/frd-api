package com.furuiduo.quote.customer.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.customer.dto.CustomerResponse;
import com.furuiduo.quote.customer.repository.CustomerRepository;

@Service
public class CustomerQueryService {

  private final CustomerRepository customerRepository;

  public CustomerQueryService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  public PageResult<CustomerResponse> list(
      int page, int pageSize, String code, String name, Integer status) {
    var pageable =
        PageRequest.of(
            Math.max(page - 1, 0),
            Math.min(Math.max(pageSize, 1), 200),
            Sort.by(Sort.Direction.DESC, "updatedAt"));
    var result = customerRepository.search(code, name, status, pageable);
    return new PageResult<>(
        result.getContent().stream().map(CustomerResponse::from).toList(),
        result.getTotalElements());
  }
}
