package com.furuiduo.quote.cost.support;

import java.util.Set;

import com.furuiduo.quote.cost.entity.CostHighlightMode;
import com.furuiduo.quote.cost.service.CostDeptHighlightService;
import com.furuiduo.quote.quote.service.QuoteLibraryEntryService;
import com.furuiduo.quote.sys.entity.SysUser;

import org.springframework.stereotype.Component;

@Component
public class CostHighlightQuerySupport {

  private final CostDeptHighlightService highlightService;
  private final QuoteLibraryEntryService quoteLibraryEntryService;

  public CostHighlightQuerySupport(
      CostDeptHighlightService highlightService,
      QuoteLibraryEntryService quoteLibraryEntryService) {
    this.highlightService = highlightService;
    this.quoteLibraryEntryService = quoteLibraryEntryService;
  }

  /** highlightOnly=true 时返回可见常用 ID 集合；否则 null 表示不限制 */
  public Set<Long> resolveRestrictIds(
      CostHighlightMode mode, SysUser user, Boolean highlightOnly) {
    if (!Boolean.TRUE.equals(highlightOnly)) {
      return null;
    }
    return highlightService.visibleHighlightedCostIds(mode, user);
  }

  /** 叠加「仅看常用」与「是否生成报价」后的成本 ID 限制。 */
  public Set<Long> resolveRestrictIds(
      CostHighlightMode mode, SysUser user, Boolean highlightOnly, Boolean inQuoteLibrary) {
    return quoteLibraryEntryService.resolveInQuoteLibraryRestrictIds(
        mode, resolveRestrictIds(mode, user, highlightOnly), inQuoteLibrary);
  }
}
