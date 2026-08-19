package com.furuiduo.quote.supplier.seed;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.supplier.entity.Supplier;
import com.furuiduo.quote.supplier.repository.SupplierRepository;
import com.furuiduo.quote.supplier.support.SupplierCategories;
import com.furuiduo.quote.supplier.support.SupplierCodeGenerator;

/** 将旧版全局序号 SUP-2026-0001 迁移为按分类独立编号 SUP-TRK-2026-0001。 */
@Component
@Order(101)
public class SupplierCodeMigrationRunner implements ApplicationRunner {

  private static final Pattern LEGACY_CODE = Pattern.compile("^SUP-\\d{4}-\\d{4}$");

  private final SupplierRepository supplierRepository;

  public SupplierCodeMigrationRunner(SupplierRepository supplierRepository) {
    this.supplierRepository = supplierRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    int year = java.time.LocalDate.now().getYear();
    for (String category :
        List.of(
            SupplierCategories.TRUCK,
            SupplierCategories.FUMIGATION,
            SupplierCategories.YARD,
            SupplierCategories.OTHER)) {
      migrateCategory(category, year);
    }
  }

  private void migrateCategory(String category, int year) {
    List<Supplier> legacy =
        supplierRepository.findAllByCategoryOrderByIdAsc(category).stream()
            .filter(supplier -> LEGACY_CODE.matcher(supplier.getCode()).matches())
            .sorted(Comparator.comparingInt(this::legacySequence))
            .toList();
    if (legacy.isEmpty()) {
      return;
    }
    int seq = 1;
    for (Supplier supplier : legacy) {
      supplier.setCode(SupplierCodeGenerator.format(category, year, seq++));
      supplierRepository.save(supplier);
    }
  }

  private int legacySequence(Supplier supplier) {
    String code = supplier.getCode();
    int lastDash = code.lastIndexOf('-');
    if (lastDash < 0 || lastDash >= code.length() - 1) {
      return supplier.getId().intValue();
    }
    try {
      return Integer.parseInt(code.substring(lastDash + 1));
    } catch (NumberFormatException ex) {
      return supplier.getId().intValue();
    }
  }
}
