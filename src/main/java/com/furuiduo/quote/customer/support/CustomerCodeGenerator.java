package com.furuiduo.quote.customer.support;

import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.furuiduo.quote.customer.repository.CustomerRepository;

@Component
public class CustomerCodeGenerator {

  private final CustomerRepository customerRepository;

  public CustomerCodeGenerator(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  public String next() {
    int year = LocalDate.now().getYear();
    String prefix = "CUS-" + year + "-";
    int seq =
        customerRepository
            .findCustomerCodesByPrefix(prefix + "%", PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .map(code -> Integer.parseInt(code.substring(prefix.length())) + 1)
            .orElse(1);
    return prefix + String.format("%04d", seq);
  }
}
