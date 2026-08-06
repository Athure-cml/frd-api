package com.furuiduo.quote.common;

import java.util.Arrays;
import java.util.List;

/** 解析导出等接口中的逗号分隔 id 参数。 */
public final class RequestIds {

  private RequestIds() {}

  public static List<Long> parse(String ids) {
    if (ids == null || ids.isBlank()) {
      return List.of();
    }
    return Arrays.stream(ids.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(Long::valueOf)
        .toList();
  }

  public static boolean present(List<Long> ids) {
    return ids != null && !ids.isEmpty();
  }
}
