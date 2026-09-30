package com.furuiduo.quote.quote.support;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.furuiduo.quote.quote.entity.QuoteServiceType;

/** 报价单服务类型多选：存储枚举名列表，展示用中文名。 */
public final class QuoteServiceTypesSupport {

  private QuoteServiceTypesSupport() {}

  public static List<String> normalize(List<String> raw) {
    if (raw == null || raw.isEmpty()) {
      return List.of();
    }
    LinkedHashSet<String> unique = new LinkedHashSet<>();
    for (String item : raw) {
      if (item == null || item.isBlank()) {
        continue;
      }
      QuoteServiceType type = QuoteServiceType.valueOf(item.trim().toUpperCase());
      unique.add(type.name());
    }
    return new ArrayList<>(unique);
  }

  public static String joinNames(List<String> raw) {
    if (raw == null || raw.isEmpty()) {
      return "";
    }
    return raw.stream()
        .filter(item -> item != null && !item.isBlank())
        .map(
            item -> {
              try {
                return QuoteServiceType.valueOf(item.trim().toUpperCase()).displayName();
              } catch (IllegalArgumentException ex) {
                return item.trim();
              }
            })
        .collect(Collectors.joining("、"));
  }

  public static List<String> copyOf(List<String> raw) {
    if (raw == null || raw.isEmpty()) {
      return new ArrayList<>();
    }
    return new ArrayList<>(raw);
  }
}
