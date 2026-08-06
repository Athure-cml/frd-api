package com.furuiduo.quote.shippingline.support;

import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.furuiduo.quote.shippingline.repository.ShippingLineRepository;

@Component
public class ShippingLineCodeGenerator {

  private final ShippingLineRepository repository;

  public ShippingLineCodeGenerator(ShippingLineRepository repository) {
    this.repository = repository;
  }

  public String next() {
    int year = LocalDate.now().getYear();
    String prefix = "SSL-" + year + "-";
    int seq =
        repository
            .findCodesByPrefix(prefix + "%", PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .map(code -> Integer.parseInt(code.substring(prefix.length())) + 1)
            .orElse(1);
    return prefix + String.format("%04d", seq);
  }
}
