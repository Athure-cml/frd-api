package com.furuiduo.quote.common;

/** 客商导入编码校验辅助。 */
public final class PartyImportSupport {

  private PartyImportSupport() {}

  public static String validateImportCode(
      PartyImportCodeKind kind,
      String rawCode,
      PartyImportContext ctx,
      String supplierCategory) {
    String normalized = kind.normalize(rawCode);
    if (normalized == null) {
      return null;
    }
    if (!kind.matches(normalized)) {
      return kind.invalidFormatMessage();
    }
    if (kind == PartyImportCodeKind.SUPPLIER && supplierCategory != null) {
      String categoryError = kind.validateSupplierCategory(normalized, supplierCategory);
      if (categoryError != null) {
        return categoryError;
      }
    }
    return ctx.reserveFileCode(normalized);
  }

  public static String normalizeImportCode(PartyImportCodeKind kind, String rawCode) {
    String normalized = kind.normalize(rawCode);
    if (normalized == null || !kind.matches(normalized)) {
      return null;
    }
    return normalized;
  }
}
