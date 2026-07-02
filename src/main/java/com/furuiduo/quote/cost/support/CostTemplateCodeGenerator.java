package com.furuiduo.quote.cost.support;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.furuiduo.quote.cost.repository.CostGridTemplateRepository;

@Component
public class CostTemplateCodeGenerator {

  private final CostGridTemplateRepository repository;

  public CostTemplateCodeGenerator(CostGridTemplateRepository repository) {
    this.repository = repository;
  }

  public String next(String mode) {
    String prefix = mode + "_tpl_";
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
