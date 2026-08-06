package com.furuiduo.quote.supplier.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 供应商类型（多选）编码与中文标签。 */
public final class SupplierTypes {

  public static final String BOOKING_AGENT = "BOOKING_AGENT";
  public static final String FLEET = "FLEET";
  public static final String CUSTOMS_BROKER = "CUSTOMS_BROKER";
  public static final String WAREHOUSE = "WAREHOUSE";
  public static final String DEDICATED_LINE = "DEDICATED_LINE";
  public static final String CONTAINER_LEASING = "CONTAINER_LEASING";
  public static final String OTHER = "OTHER";

  private static final Map<String, String> CODE_TO_LABEL = new LinkedHashMap<>();
  private static final Map<String, String> LABEL_TO_CODE = new LinkedHashMap<>();

  static {
    put(BOOKING_AGENT, "订舱代理");
    put(FLEET, "车队");
    put(CUSTOMS_BROKER, "报关行");
    put(WAREHOUSE, "仓库");
    put(DEDICATED_LINE, "专线");
    put(CONTAINER_LEASING, "租箱公司");
    put(OTHER, "其他");
  }

  private SupplierTypes() {}

  private static void put(String code, String label) {
    CODE_TO_LABEL.put(code, label);
    LABEL_TO_CODE.put(label, code);
    LABEL_TO_CODE.put(label.toLowerCase(Locale.ROOT), code);
    LABEL_TO_CODE.put(code, code);
    LABEL_TO_CODE.put(code.toLowerCase(Locale.ROOT), code);
  }

  public static boolean isValid(String code) {
    return code != null && CODE_TO_LABEL.containsKey(code);
  }

  /** 规范化并去重；非法编码抛 IllegalArgumentException。 */
  public static List<String> normalize(List<String> raw) {
    if (raw == null || raw.isEmpty()) {
      return List.of();
    }
    Set<String> ordered = new LinkedHashSet<>();
    for (String item : raw) {
      if (item == null || item.isBlank()) {
        continue;
      }
      String code = item.trim();
      if (!isValid(code)) {
        throw new IllegalArgumentException("无效的供应商类型: " + code);
      }
      ordered.add(code);
    }
    return new ArrayList<>(ordered);
  }

  public static String toExcelValue(List<String> types) {
    if (types == null || types.isEmpty()) {
      return "";
    }
    List<String> labels = new ArrayList<>();
    for (String code : types) {
      String label = CODE_TO_LABEL.get(code);
      if (label != null) {
        labels.add(label);
      }
    }
    return String.join(",", labels);
  }

  /** 解析 Excel 单元格：支持中文标签或编码，逗号/顿号/分号分隔。 */
  public static List<String> parseExcelValue(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    String[] parts = raw.split("[,，;；、|/]");
    Set<String> ordered = new LinkedHashSet<>();
    for (String part : parts) {
      if (part == null || part.isBlank()) {
        continue;
      }
      String token = part.trim();
      String code = LABEL_TO_CODE.get(token);
      if (code == null) {
        code = LABEL_TO_CODE.get(token.toLowerCase(Locale.ROOT));
      }
      if (code == null) {
        throw new IllegalArgumentException(
            "无效的供应商类型「" + token + "」，可选：订舱代理、车队、报关行、仓库、专线、租箱公司、其他");
      }
      ordered.add(code);
    }
    return new ArrayList<>(ordered);
  }

  public static String labelOf(String code) {
    return CODE_TO_LABEL.getOrDefault(code, code);
  }
}
