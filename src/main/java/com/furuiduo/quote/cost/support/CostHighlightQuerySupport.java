package com.furuiduo.quote.cost.support;

import java.util.Set;

import com.furuiduo.quote.cost.entity.CostHighlightMode;
import com.furuiduo.quote.cost.service.CostDeptHighlightService;
import com.furuiduo.quote.sys.entity.SysUser;

import org.springframework.stereotype.Component;

@Component
public class CostHighlightQuerySupport {

  private final CostDeptHighlightService highlightService;

  public CostHighlightQuerySupport(CostDeptHighlightService highlightService) {
    this.highlightService = highlightService;
  }

  /** highlightOnly=true 时返回可见常用 ID 集合；否则 null 表示不限制 */
  public Set<Long> resolveRestrictIds(
      CostHighlightMode mode, SysUser user, Boolean highlightOnly) {
    if (!Boolean.TRUE.equals(highlightOnly)) {
      return null;
    }
    return highlightService.visibleHighlightedCostIds(mode, user);
  }
}
