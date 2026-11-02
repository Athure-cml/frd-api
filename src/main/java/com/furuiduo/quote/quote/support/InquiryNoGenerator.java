package com.furuiduo.quote.quote.support;

import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.furuiduo.quote.quote.repository.InquiryOrderRepository;

@Component
public class InquiryNoGenerator {

  private final InquiryOrderRepository inquiryOrderRepository;

  public InquiryNoGenerator(InquiryOrderRepository inquiryOrderRepository) {
    this.inquiryOrderRepository = inquiryOrderRepository;
  }

  public String next() {
    int year = LocalDate.now().getYear();
    String prefix = "IQ-" + year + "-";
    int seq =
        inquiryOrderRepository
            .findInquiryNosByPrefix(prefix + "%", PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .map(no -> Integer.parseInt(no.substring(prefix.length())) + 1)
            .orElse(1);
    return prefix + String.format("%04d", seq);
  }
}
