package com.furuiduo.quote.quoterule;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** POR 列表匹配：remark 为「城市,州」成对逗号分隔，如 NEW YORK,NY,NORFOLK,VA */
public final class PorListMatcher {

  private PorListMatcher() {}

  public static boolean matches(String por, String remark) {
    if (por == null || por.isBlank() || remark == null || remark.isBlank()) {
      return false;
    }
    String normalizedPor = normalizePor(por);
    for (String entry : parsePorEntries(remark)) {
      if (entry.equals(normalizedPor)) {
        return true;
      }
    }
    return false;
  }

  static String normalizePor(String value) {
    if (value == null) {
      return "";
    }
    return value
        .trim()
        .toUpperCase(Locale.ROOT)
        .replaceAll("\\s+", " ")
        .replaceAll(",\\s*", ",");
  }

  static List<String> parsePorEntries(String remark) {
    String[] tokens =
        remark.trim().split("[,，\\n;]+");
    List<String> cleaned = new ArrayList<>();
    for (String token : tokens) {
      String part = token == null ? "" : token.trim();
      if (!part.isEmpty()) {
        cleaned.add(part);
      }
    }

    List<String> entries = new ArrayList<>();
    for (int i = 0; i + 1 < cleaned.size(); i += 2) {
      entries.add(normalizePor(cleaned.get(i) + "," + cleaned.get(i + 1)));
    }
    return entries;
  }
}
