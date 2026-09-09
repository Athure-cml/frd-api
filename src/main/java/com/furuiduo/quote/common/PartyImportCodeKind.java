package com.furuiduo.quote.common;

import java.util.Locale;
import java.util.regex.Pattern;

/** 客商四类主数据各自的导入编码规则（前缀/格式互不混用）。 */
public enum PartyImportCodeKind {
  CUSTOMER(Pattern.compile("^CUS-\\d{4}-\\d{4}$"), "客户", "CUS-YYYY-NNNN"),
  AGENT(Pattern.compile("^AGT-\\d{4}-\\d{4}$"), "代理商", "AGT-YYYY-NNNN"),
  SHIPPING_LINE(Pattern.compile("^SSL-\\d{4}-\\d{4}$"), "船公司", "SSL-YYYY-NNNN"),
  SUPPLIER(
      Pattern.compile("^SUP-(TRK|FUM|YRD|OTH)-\\d{4}-\\d{4}$"), "供应商", "SUP-XXX-YYYY-NNNN");

  private final Pattern pattern;
  private final String moduleLabel;
  private final String formatHint;

  PartyImportCodeKind(Pattern pattern, String moduleLabel, String formatHint) {
    this.pattern = pattern;
    this.moduleLabel = moduleLabel;
    this.formatHint = formatHint;
  }

  public String normalize(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    return raw.trim().toUpperCase(Locale.ROOT);
  }

  public boolean matches(String normalized) {
    return normalized != null && pattern.matcher(normalized).matches();
  }

  public String invalidFormatMessage() {
    return moduleLabel + "编码格式无效，应为 " + formatHint;
  }

  /** 供应商编码需与当前分类前缀一致（如 SUP-TRK-… 仅用于卡车供应商）。 */
  public String validateSupplierCategory(String normalized, String supplierCategory) {
    if (this != SUPPLIER || normalized == null) {
      return null;
    }
    String expected =
        switch (com.furuiduo.quote.supplier.support.SupplierCategories.normalize(
            supplierCategory)) {
          case com.furuiduo.quote.supplier.support.SupplierCategories.FUMIGATION -> "FUM";
          case com.furuiduo.quote.supplier.support.SupplierCategories.YARD -> "YRD";
          case com.furuiduo.quote.supplier.support.SupplierCategories.OTHER -> "OTH";
          default -> "TRK";
        };
    String tag = extractSupplierTag(normalized);
    if (tag != null && !tag.equals(expected)) {
      return "编码 "
          + normalized
          + " 与当前供应商分类不匹配（应为 SUP-"
          + expected
          + "-YYYY-NNNN）";
    }
    return null;
  }

  private static String extractSupplierTag(String code) {
    if (code == null || !code.startsWith("SUP-")) {
      return null;
    }
    int secondDash = code.indexOf('-', 4);
    if (secondDash < 0) {
      return null;
    }
    return code.substring(4, secondDash);
  }
}
