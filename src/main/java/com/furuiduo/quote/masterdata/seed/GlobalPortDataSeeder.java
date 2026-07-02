package com.furuiduo.quote.masterdata.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.furuiduo.quote.masterdata.repository.MdGlobalPortRepository;
import com.furuiduo.quote.masterdata.service.GlobalPortSyncService;

/**
 * 启动时从 UNECE UN/LOCODE 离线数据包全量初始化全球港口档案（仅空库时执行）。
 *
 * <p>不再调用 FreightUtils 批量 API，避免限流。增量更新由 {@link
 * com.furuiduo.quote.masterdata.sync.GlobalPortSyncScheduler} 或管理端手动触发。
 */
@Component
@Order(105)
public class GlobalPortDataSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(GlobalPortDataSeeder.class);

  private final MdGlobalPortRepository portRepository;
  private final GlobalPortSyncService syncService;

  @Value("${quote.masterdata.global-port.enabled:true}")
  private boolean enabled;

  @Value("${quote.masterdata.global-port.skip-if-exists:true}")
  private boolean skipIfExists;

  public GlobalPortDataSeeder(
      MdGlobalPortRepository portRepository, GlobalPortSyncService syncService) {
    this.portRepository = portRepository;
    this.syncService = syncService;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!enabled) {
      return;
    }
    if (skipIfExists && portRepository.count() > 0) {
      log.info("全球港口档案已存在，跳过 UN/LOCODE 全量初始化");
      return;
    }
    try {
      var result = syncService.initializeIfEmpty();
      if (result == null) {
        log.info("全球港口档案非空，跳过初始化");
        return;
      }
      log.info(
          "全球港口档案初始化完成：版本 {}，新增 {} 条",
          result.dataVersion(),
          result.inserted());
    } catch (Exception ex) {
      log.error("全球港口 UN/LOCODE 初始化失败：{}", ex.getMessage(), ex);
    }
  }
}
