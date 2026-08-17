package com.furuiduo.quote.common;

import org.springframework.data.domain.Sort;

/**
 * 客商主数据列表排序：先置顶分组，再自定义 sortOrder，最后更新时间。
 *
 * <p>注意：不能用 pinnedAt DESC，否则同组置顶项会按置顶时间排序，拖拽 sortOrder 不生效。
 */
public final class PartySort {

  private PartySort() {}

  public static Sort list() {
    return Sort.by(
        Sort.Order.asc("pinRank"),
        Sort.Order.asc("sortOrder"),
        Sort.Order.desc("updatedAt"));
  }
}
