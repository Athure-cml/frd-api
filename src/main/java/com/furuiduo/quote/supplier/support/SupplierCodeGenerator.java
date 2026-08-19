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

  public String next(String category) {
    String normalized = SupplierCategories.normalize(category);
    int year = LocalDate.now().getYear();
    String prefix = prefix(normalized, year);
    int seq =
        supplierRepository
            .findSupplierCodesByCategoryAndPrefix(
                normalized, prefix + "%", PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .map(code -> Integer.parseInt(code.substring(prefix.length())) + 1)
            .orElse(1);
    return format(normalized, year, seq);
  }

  public static String format(String category, int year, int seq) {
    return prefix(SupplierCategories.normalize(category), year) + String.format("%04d", seq);
  }

  public static String prefix(String category, int year) {
    return "SUP-" + categoryTag(category) + "-" + year + "-";
  }

  private static String categoryTag(String category) {
    return switch (SupplierCategories.normalize(category)) {
      case SupplierCategories.FUMIGATION -> "FUM";
      case SupplierCategories.YARD -> "YRD";
      case SupplierCategories.OTHER -> "OTH";
      default -> "TRK";
    };
  }
}
