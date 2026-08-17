package com.furuiduo.quote.supplier.support;

import java.util.Locale;
import java.util.Set;

/** 供应商大类：卡车 / 熏蒸 / 仓库堆场 / 其他。 */
public final class SupplierCategories {

  public static final String TRUCK = "TRUCK";
  public static final String FUMIGATION = "FUMIGATION";
  public static final String YARD = "YARD";
  public static final String OTHER = "OTHER";

  private static final Set<String> ALL = Set.of(TRUCK, FUMIGATION, YARD, OTHER);

  private SupplierCategories() {}

  public static boolean isValid(String category) {
    return category != null && ALL.contains(category.trim().toUpperCase(Locale.ROOT));
  }

  public static String normalize(String category) {
    if (category == null || category.isBlank()) {
      return TRUCK;
    }
    String code = category.trim().toUpperCase(Locale.ROOT);
    if (!ALL.contains(code)) {
      throw new IllegalArgumentException("无效的供应商分类: " + category);
    }
    return code;
  }

  public static boolean supportsTypes(String category) {
    return OTHER.equals(normalize(category));
  }

  public static boolean supportsFormulas(String category) {
    return TRUCK.equals(normalize(category));
  }

  public static String exportFilename(String category) {
    return switch (normalize(category)) {
      case FUMIGATION -> "熏蒸供应商.xlsx";
      case YARD -> "仓库堆场.xlsx";
      case OTHER -> "其他供应商.xlsx";
      default -> "卡车供应商.xlsx";
    };
  }

  public static String displayName(String category) {
    return switch (normalize(category)) {
      case FUMIGATION -> "熏蒸供应商";
      case YARD -> "仓库堆场";
      case OTHER -> "其他供应商";
      default -> "卡车供应商";
    };
  }
}
