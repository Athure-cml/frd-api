package com.furuiduo.quote.common;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

  /** 去重并保留正整数 ID，保持首次出现顺序。 */
  public static List<Long> distinctPositive(List<Long> raw) {
    if (raw == null || raw.isEmpty()) {
      return List.of();
    }
    Set<Long> seen = new LinkedHashSet<>();
    for (Long id : raw) {
      if (id != null && id > 0) {
        seen.add(id);
      }
    }
    return List.copyOf(seen);
  }
}
