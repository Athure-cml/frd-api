package com.furuiduo.quote.cost.seed;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.entity.CostSea;
import com.furuiduo.quote.cost.repository.CostFumigationRepository;
import com.furuiduo.quote.cost.repository.CostRoadRepository;
import com.furuiduo.quote.cost.repository.CostSeaRepository;

@Component
@Order(20)
public class CostDataSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(CostDataSeeder.class);

  private final CostRoadRepository roadRepository;
  private final CostSeaRepository seaRepository;
  private final CostFumigationRepository fumigationRepository;
  private final boolean resetCostData;

  public CostDataSeeder(
      CostRoadRepository roadRepository,
      CostSeaRepository seaRepository,
      CostFumigationRepository fumigationRepository,
      @Value("${quote.cost.reset:false}") boolean resetCostData) {
    this.roadRepository = roadRepository;
    this.seaRepository = seaRepository;
    this.fumigationRepository = fumigationRepository;
    this.resetCostData = resetCostData;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (resetCostData) {
      resetAllCostData();
      seedAll();
      log.warn(
          "QUOTE_COST_RESET=true：三个成本库已清空并各写入 {} 条测试数据。"
              + "请立即删除该环境变量并重新部署，避免每次启动都清库",
          CostSampleData.SAMPLE_SIZE);
      return;
    }

    seedIfEmpty(
        roadRepository.count(),
        CostSampleData.roadSamples(),
        roadRepository::saveAll,
        "卡车");
    seedIfEmpty(
        seaRepository.count(),
        CostSampleData.seaSamples(),
        seaRepository::saveAll,
        "海运");
    seedIfEmpty(
        fumigationRepository.count(),
        CostSampleData.fumigationSamples(),
        fumigationRepository::saveAll,
        "熏蒸");
  }

  private void resetAllCostData() {
    log.warn("正在清空 cost_road / cost_sea / cost_fumigation …");
    fumigationRepository.deleteAllInBatch();
    seaRepository.deleteAllInBatch();
    roadRepository.deleteAllInBatch();
  }

  private void seedAll() {
    roadRepository.saveAll(CostSampleData.roadSamples());
    seaRepository.saveAll(CostSampleData.seaSamples());
    fumigationRepository.saveAll(CostSampleData.fumigationSamples());
    log.info(
        "成本库测试数据已重建：卡车 {} 条、海运 {} 条、熏蒸 {} 条",
        CostSampleData.SAMPLE_SIZE,
        CostSampleData.SAMPLE_SIZE,
        CostSampleData.SAMPLE_SIZE);
  }

  private static <T> void seedIfEmpty(
      long count, List<T> samples, java.util.function.Consumer<List<T>> saver, String label) {
    if (count > 0) {
      return;
    }
    saver.accept(samples);
    log.info("成本库-{}：已写入 {} 条测试数据", label, samples.size());
  }
}
