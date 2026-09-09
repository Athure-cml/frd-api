package com.furuiduo.quote.cost.support;

import java.math.BigDecimal;
import java.util.Map;

/** 批量「按搜索条件全选」时，从 JSON Map 解析列表搜索参数。 */
public final class CostBatchCriteriaMaps {

  private CostBatchCriteriaMaps() {}

  public static String string(Map<String, Object> criteria, String key) {
    if (criteria == null) {
      return null;
    }
    Object value = criteria.get(key);
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value).trim();
    return text.isEmpty() ? null : text;
  }

  public static BigDecimal decimal(Map<String, Object> criteria, String key) {
    if (criteria == null) {
      return null;
    }
    Object value = criteria.get(key);
    if (value == null || "".equals(value)) {
      return null;
    }
    if (value instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    try {
      return new BigDecimal(String.valueOf(value).trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
