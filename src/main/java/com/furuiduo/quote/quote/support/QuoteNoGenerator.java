package com.furuiduo.quote.quote.support;

import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.furuiduo.quote.quote.repository.QuoteOrderRepository;

@Component
public class QuoteNoGenerator {

  private final QuoteOrderRepository quoteOrderRepository;

  public QuoteNoGenerator(QuoteOrderRepository quoteOrderRepository) {
    this.quoteOrderRepository = quoteOrderRepository;
  }

  public String next() {
    int year = LocalDate.now().getYear();
    String prefix = "QT-" + year + "-";
    int seq =
        quoteOrderRepository
            .findQuoteNosByPrefix(prefix + "%", PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .map(no -> Integer.parseInt(no.substring(prefix.length())) + 1)
            .orElse(1);
    return prefix + String.format("%04d", seq);
  }
}
