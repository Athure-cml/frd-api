package com.furuiduo.quote.cost.support;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** 批量操作 ID 解析：ids 优先，否则按 searchCriteria 展开，再剔除 excludeIds。 */
public final class CostBatchIdResolver {

  private CostBatchIdResolver() {}

  public static List<Long> resolve(
      List<Long> ids,
      Map<String, Object> searchCriteria,
      List<Long> excludeIds,
      Supplier<List<Long>> idsFromCriteria) {
    List<Long> resolved;
    if (ids != null && !ids.isEmpty()) {
      resolved = ids;
    } else if (searchCriteria != null && !searchCriteria.isEmpty()) {
      resolved = idsFromCriteria.get();
    } else {
      return List.of();
    }
    if (excludeIds == null || excludeIds.isEmpty()) {
      return resolved;
    }
    Set<Long> exclude = new HashSet<>(excludeIds);
    return resolved.stream().filter(id -> !exclude.contains(id)).toList();
  }
}
