package com.furuiduo.quote.common;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** JPQL 可选文本筛选：PostgreSQL 18 下 null 字符串参数可能绑定为 bytea，统一用空串表示“不筛选”。 */
public final class SearchText {

  private SearchText() {}

  public static String orEmpty(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return value.trim();
  }

  /** 逗号分隔的多值（如 CITY 多选），去空白、去重、转大写。 */
  public static List<String> parseCsvUpper(String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(part -> !part.isEmpty())
        .map(part -> part.toUpperCase(Locale.ROOT))
        .distinct()
        .toList();
  }
}
