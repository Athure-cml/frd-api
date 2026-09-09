package com.furuiduo.quote.quoterule.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.quoterule.QuoteRuleContext;
import com.furuiduo.quote.quoterule.entity.MdQuoteRule;
import com.furuiduo.quote.quoterule.repository.MdQuoteRuleRepository;

@Service
public class QuoteRuleEngine {

  private final MdQuoteRuleRepository quoteRuleRepository;

  public QuoteRuleEngine(MdQuoteRuleRepository quoteRuleRepository) {
    this.quoteRuleRepository = quoteRuleRepository;
  }

  @Transactional(readOnly = true)
  public BigDecimal applyDecimalTarget(
      String targetField, BigDecimal base, QuoteRuleContext context) {
    if (base == null) {
      return null;
    }
    return findFirstMatching(loadActiveRules(), targetField, base, context)
        .map(rule -> calculateDecimal(rule, base, context))
        .orElse(base);
  }

  @Transactional(readOnly = true)
  public String formatDecimalTarget(
      String targetField, BigDecimal base, QuoteRuleContext context) {
    BigDecimal result = applyDecimalTarget(targetField, base, context);
    return formatUsd(result);
  }

  @Transactional(readOnly = true)
  public String applyDocFee(QuoteRuleContext context) {
    return findFirstMatching(loadActiveRules(), "DOC_FEE", null, context)
        .map(rule -> formatUsd(calculateDecimal(rule, null, context)))
        .orElse(null);
  }

  @Transactional(readOnly = true)
  public String applyCifTarget(String targetField, QuoteRuleContext context) {
    BigDecimal cif = context.cifAmount();
    return findFirstMatching(loadActiveRules(), targetField, null, context)
        .map(
            rule -> {
              if (cif == null || cif.compareTo(BigDecimal.ZERO) <= 0) {
                return buildCifPlaceholder(rule);
              }
              return formatUsd(calculateDecimal(rule, cif, context));
            })
        .orElse(null);
  }

  private List<MdQuoteRule> loadActiveRules() {
    return quoteRuleRepository.findByStatusOrderBySortOrderAscIdAsc(1);
  }

  private Optional<MdQuoteRule> findFirstMatching(
      List<MdQuoteRule> rules,
      String targetField,
      BigDecimal base,
      QuoteRuleContext context) {
    return rules.stream()
        .filter(rule -> targetField.equals(rule.getTargetField()))
        .filter(rule -> matchesCondition(rule, base, context))
        .findFirst();
  }

  private boolean matchesCondition(
      MdQuoteRule rule, BigDecimal base, QuoteRuleContext context) {
    String type = normalize(rule.getConditionType());
    return switch (type) {
      case "ALWAYS" -> true;
      case "FUMIGATION_ENABLED" -> context.fumigationEnabled();
      case "FUMIGATION_DISABLED" -> !context.fumigationEnabled();
      case "POD_CHINA" -> context.podChina();
      case "POD_NOT_CHINA" -> !context.podChina();
      case "BASE_GT" ->
          base != null
              && rule.getConditionAmount() != null
              && base.compareTo(rule.getConditionAmount()) > 0;
      case "BASE_LTE" ->
          base != null
              && rule.getConditionAmount() != null
              && base.compareTo(rule.getConditionAmount()) <= 0;
      default -> false;
    };
  }

  private BigDecimal calculateDecimal(
      MdQuoteRule rule, BigDecimal base, QuoteRuleContext context) {
    String calcType = normalize(rule.getCalcType());
    return switch (calcType) {
      case "COST_PLUS" -> {
        BigDecimal cost = base == null ? BigDecimal.ZERO : base;
        BigDecimal add = rule.getAddAmount() == null ? BigDecimal.ZERO : rule.getAddAmount();
        yield cost.add(add);
      }
      case "FIXED" ->
          rule.getFixedAmount() == null ? BigDecimal.ZERO : rule.getFixedAmount();
      case "CIF_MULTIPLY" -> {
        BigDecimal cif = context.cifAmount();
        if (cif == null) {
          yield BigDecimal.ZERO;
        }
        BigDecimal factor =
            rule.getCifFactor() == null ? BigDecimal.ONE : rule.getCifFactor();
        BigDecimal rate = rule.getCifRate() == null ? BigDecimal.ZERO : rule.getCifRate();
        yield cif.multiply(factor).multiply(rate);
      }
      case "CIF_PERCENT" -> {
        BigDecimal cif = context.cifAmount();
        if (cif == null) {
          yield BigDecimal.ZERO;
        }
        BigDecimal rate = rule.getCifRate() == null ? BigDecimal.ZERO : rule.getCifRate();
        yield cif.multiply(rate);
      }
      default -> base == null ? BigDecimal.ZERO : base;
    };
  }

  private String buildCifPlaceholder(MdQuoteRule rule) {
    String calcType = normalize(rule.getCalcType());
    if ("CIF_MULTIPLY".equals(calcType)) {
      BigDecimal factor = rule.getCifFactor() == null ? BigDecimal.ONE : rule.getCifFactor();
      BigDecimal rate = rule.getCifRate() == null ? BigDecimal.ZERO : rule.getCifRate();
      return "CIF*" + stripTrailingZeros(factor) + "*" + formatPercent(rate);
    }
    if ("CIF_PERCENT".equals(calcType)) {
      BigDecimal rate = rule.getCifRate() == null ? BigDecimal.ZERO : rule.getCifRate();
      return "CIF*" + formatPercent(rate);
    }
    return "CIF";
  }

  private String formatPercent(BigDecimal rate) {
    return rate.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%";
  }

  private String stripTrailingZeros(BigDecimal value) {
    return value.stripTrailingZeros().toPlainString();
  }

  private String formatUsd(BigDecimal amount) {
    if (amount == null) {
      return null;
    }
    return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toUpperCase();
  }
}
