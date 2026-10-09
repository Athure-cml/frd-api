package com.furuiduo.quote.common;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** JPQL 可选文本筛选：PostgreSQL 18 下 null 字符串参数可能绑定为 bytea，统一用空串表示“不筛选”。 */
public final class SearchText {

  /** 供应商等多选分隔符：名称常含逗号（如 Inc.），不可再用 CSV。 */
  public static final String MULTI_VALUE_DELIMITER = "|";

  private SearchText() {}

  public static String orEmpty(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return value.trim();
  }

  /** 逗号分隔的多值（如 CITY 多选），去空白、去重、转大写。 */
  public static List<String> parseCsvUpper(String value) {
    return parseDelimitedUpper(value, ",");
  }

  /**
   * 管道符分隔的多值（如 SUPPLIER 多选）。供应商全称常含逗号，必须用本方法，勿用 {@link
   * #parseCsvUpper}。
   */
  public static List<String> parsePipeUpper(String value) {
    return parseDelimitedUpper(value, MULTI_VALUE_DELIMITER);
  }

  public static List<String> parseDelimitedUpper(String value, String delimiter) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    String delim = delimiter == null || delimiter.isEmpty() ? "," : delimiter;
    return Arrays.stream(value.split(Pattern.quote(delim), -1))
        .map(String::trim)
        .filter(part -> !part.isEmpty())
        .map(part -> part.toUpperCase(Locale.ROOT))
        .distinct()
        .toList();
  }
}
