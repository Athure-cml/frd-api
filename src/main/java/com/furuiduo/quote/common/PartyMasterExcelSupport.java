package com.furuiduo.quote.common;

import java.util.Locale;

/** Shared helpers for customer / supplier / shipping-line / agent Excel import-export. */
public final class PartyMasterExcelSupport {

  private PartyMasterExcelSupport() {}

  public static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  public static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  /** Trim and collapse internal whitespace. */
  public static String normalizeName(String name) {
    if (name == null) {
      return "";
    }
    return name.trim().replaceAll("\\s+", " ");
  }

  /** Case-insensitive key for duplicate checks. */
  public static String nameKey(String name) {
    return normalizeName(name).toLowerCase(Locale.ROOT);
  }

  public static String statusLabel(Integer status) {
    if (status != null && status == 0) {
      return "停用";
    }
    return "启用";
  }

  /**
   * Parses status cell.
   *
   * <ul>
   *   <li>blank → missing (keep existing on update / default enabled on create)
   *   <li>valid → enabled(1) / disabled(0)
   *   <li>unrecognized text → {@link StatusCell#unrecognized()}
   * </ul>
   */
  public static StatusCell parseStatusCell(String raw) {
    if (raw == null || raw.isBlank()) {
      return StatusCell.missing();
    }
    String trimmed = raw.trim();
    String value = trimmed.toLowerCase(Locale.ROOT);
    if ("1".equals(value)
        || "启用".equals(trimmed)
        || "enabled".equals(value)
        || "active".equals(value)
        || "true".equals(value)
        || "yes".equals(value)) {
      return StatusCell.of(1);
    }
    if ("0".equals(value)
        || "停用".equals(trimmed)
        || "disabled".equals(value)
        || "inactive".equals(value)
        || "false".equals(value)
        || "no".equals(value)) {
      return StatusCell.of(0);
    }
    return StatusCell.unrecognized();
  }

  /** Resolve import status: missing keeps existing (or defaults to enabled). */
  public static int resolveStatus(StatusCell cell, Integer existingStatus) {
    if (cell == null || cell.isMissing()) {
      return existingStatus != null ? existingStatus : 1;
    }
    if (cell.isUnrecognized() || cell.value() == null) {
      throw new IllegalArgumentException("状态无效（请填启用/停用或 1/0）");
    }
    return cell.value();
  }

  /** Import status cell; factories must not share names with record accessors. */
  public record StatusCell(Kind kind, Integer value) {
    public enum Kind {
      MISSING,
      UNRECOGNIZED,
      PRESENT
    }

    public static StatusCell missing() {
      return new StatusCell(Kind.MISSING, null);
    }

    public static StatusCell unrecognized() {
      return new StatusCell(Kind.UNRECOGNIZED, null);
    }

    public static StatusCell of(int status) {
      return new StatusCell(Kind.PRESENT, status);
    }

    public boolean isMissing() {
      return kind == Kind.MISSING;
    }

    public boolean isUnrecognized() {
      return kind == Kind.UNRECOGNIZED;
    }
  }
}
