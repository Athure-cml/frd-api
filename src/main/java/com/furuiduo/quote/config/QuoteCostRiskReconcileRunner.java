package com.furuiduo.quote.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.furuiduo.quote.quote.service.QuoteCostRiskService;

/**
 * 启动时纠偏历史成本异常：费用未变（如仅有效期续期）的误标清除，真实费用变更保留。
 */
@Component
public class QuoteCostRiskReconcileRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(QuoteCostRiskReconcileRunner.class);

  private final QuoteCostRiskService quoteCostRiskService;

  public QuoteCostRiskReconcileRunner(QuoteCostRiskService quoteCostRiskService) {
    this.quoteCostRiskService = quoteCostRiskService;
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      int cleared = quoteCostRiskService.reconcileFalsePositiveRisks();
      if (cleared > 0) {
        log.info("启动纠偏完成：已清除 {} 条非费用成本异常误标", cleared);
      }
    } catch (Exception ex) {
      log.warn("启动纠偏成本异常失败（不影响服务启动）: {}", ex.getMessage());
    }
  }
}
