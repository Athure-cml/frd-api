package com.furuiduo.quote.supplier.support;

import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.furuiduo.quote.supplier.repository.SupplierRepository;

@Component
public class SupplierCodeGenerator {

  private final SupplierRepository supplierRepository;

  public SupplierCodeGenerator(SupplierRepository supplierRepository) {
    this.supplierRepository = supplierRepository;
  }

  public String next() {
    int year = LocalDate.now().getYear();
    String prefix = "SUP-" + year + "-";
    int seq =
        supplierRepository
            .findSupplierCodesByPrefix(prefix + "%", PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .map(code -> Integer.parseInt(code.substring(prefix.length())) + 1)
            .orElse(1);
    return prefix + String.format("%04d", seq);
  }
}
