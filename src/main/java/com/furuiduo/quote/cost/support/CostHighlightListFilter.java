package com.furuiduo.quote.cost.support;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class CostHighlightListFilter {

  private CostHighlightListFilter() {}

  public static <T> List<T> filterByIds(List<T> items, Set<Long> restrictToIds, IdGetter<T> getter) {
    if (restrictToIds == null) {
      return items;
    }
    return items.stream().filter(item -> restrictToIds.contains(getter.getId(item))).toList();
  }

  public static boolean isEmptyRestriction(Set<Long> restrictToIds) {
    return restrictToIds != null && restrictToIds.isEmpty();
  }

  @FunctionalInterface
  public interface IdGetter<T> {
    Long getId(T item);
  }
}
