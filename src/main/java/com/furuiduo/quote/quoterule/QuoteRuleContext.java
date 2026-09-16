package com.furuiduo.quote.quoterule;

import java.math.BigDecimal;

/** 报价单规则匹配上下文 */
public record QuoteRuleContext(
    boolean fumigationEnabled, boolean podChina, BigDecimal cifAmount, String por) {}
