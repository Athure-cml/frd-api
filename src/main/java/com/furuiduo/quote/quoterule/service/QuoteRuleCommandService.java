package com.furuiduo.quote.quoterule.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.quoterule.dto.QuoteRuleResponse;
import com.furuiduo.quote.quoterule.dto.QuoteRuleSaveRequest;
import com.furuiduo.quote.quoterule.entity.MdQuoteRule;
import com.furuiduo.quote.quoterule.repository.MdQuoteRuleRepository;

@Service
public class QuoteRuleCommandService {

  private static final Set<String> TARGET_FIELDS =
      Set.of(
          "OCEAN_FREIGHT",
          "TRUCKING_FEE",
          "FM_NON_OAK",
          "FM_OAK",
          "DOC_FEE",
          "CARGO_INSURANCE",
          "CARGO_AGENT");

  private static final Set<String> CONDITION_TYPES =
      Set.of(
          "ALWAYS",
          "FUMIGATION_ENABLED",
          "FUMIGATION_DISABLED",
          "POD_CHINA",
          "POD_NOT_CHINA",
          "BASE_GT",
          "BASE_LTE",
          "POR_IN");

  private static final Set<String> CALC_TYPES =
      Set.of("COST_PLUS", "FIXED", "CIF_MULTIPLY", "CIF_PERCENT");

  private final MdQuoteRuleRepository quoteRuleRepository;

  public QuoteRuleCommandService(MdQuoteRuleRepository quoteRuleRepository) {
    this.quoteRuleRepository = quoteRuleRepository;
  }

  @Transactional(readOnly = true)
  public QuoteRuleResponse getById(Long id) {
    return QuoteRuleResponse.from(requireEntity(id));
  }

  @Transactional
  public QuoteRuleResponse create(QuoteRuleSaveRequest request) {
    validateSaveRequest(request);
    MdQuoteRule rule = new MdQuoteRule();
    apply(rule, request);
    return QuoteRuleResponse.from(quoteRuleRepository.save(rule));
  }

  @Transactional
  public QuoteRuleResponse update(Long id, QuoteRuleSaveRequest request) {
    validateSaveRequest(request);
    MdQuoteRule rule = requireEntity(id);
    apply(rule, request);
    rule.setUpdatedAt(LocalDateTime.now());
    return QuoteRuleResponse.from(quoteRuleRepository.save(rule));
  }

  @Transactional
  public void delete(Long id) {
    quoteRuleRepository.delete(requireEntity(id));
  }

  @Transactional
  public void batchDelete(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    for (Long id : ids) {
      delete(id);
    }
  }

  private MdQuoteRule requireEntity(Long id) {
    return quoteRuleRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "规则不存在"));
  }

  private void validateSaveRequest(QuoteRuleSaveRequest request) {
    if (request.name() == null || request.name().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "规则名称不能为空");
    }
    String targetField = normalizeEnum(request.targetField());
    if (!TARGET_FIELDS.contains(targetField)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "目标费用字段无效");
    }
    String conditionType = normalizeEnum(request.conditionType());
    if (!CONDITION_TYPES.contains(conditionType)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "条件类型无效");
    }
    if (("BASE_GT".equals(conditionType) || "BASE_LTE".equals(conditionType))
        && request.conditionAmount() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分档条件需填写阈值金额");
    }
    if ("POR_IN".equals(conditionType)
        && (request.remark() == null || request.remark().isBlank())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "POR 列表条件须在备注中填写起运港");
    }
    String calcType = normalizeEnum(request.calcType());
    if (!CALC_TYPES.contains(calcType)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "计算类型无效");
    }
    switch (calcType) {
      case "COST_PLUS" -> {
        if (request.addAmount() == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "加价金额不能为空");
        }
      }
      case "FIXED" -> {
        if (request.fixedAmount() == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "固定金额不能为空");
        }
      }
      case "CIF_MULTIPLY" -> {
        if (request.cifFactor() == null || request.cifRate() == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CIF 公式需填写系数与费率");
        }
      }
      case "CIF_PERCENT" -> {
        if (request.cifRate() == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CIF 比例需填写费率");
        }
      }
      default -> {
        // validated above
      }
    }
  }

  private void apply(MdQuoteRule rule, QuoteRuleSaveRequest request) {
    rule.setName(request.name().trim());
    rule.setTargetField(normalizeEnum(request.targetField()));
    rule.setConditionType(normalizeEnum(request.conditionType()));
    rule.setConditionAmount(request.conditionAmount());
    rule.setCalcType(normalizeEnum(request.calcType()));
    rule.setAddAmount(request.addAmount());
    rule.setFixedAmount(request.fixedAmount());
    rule.setCifFactor(request.cifFactor());
    rule.setCifRate(request.cifRate());
    rule.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    rule.setStatus(request.status() == null ? 1 : request.status());
    rule.setRemark(trimToNull(request.remark()));
  }

  private String normalizeEnum(String value) {
    if (value == null) {
      return "";
    }
    return value.trim().toUpperCase();
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
