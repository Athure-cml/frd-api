package com.furuiduo.quote.masterdata.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.furuiduo.quote.masterdata.service.GlobalPortSyncService;

/** 每半年从 UNECE 官方数据包增量同步全球港口档案。 */
@Component
public class GlobalPortSyncScheduler {

  private static final Logger log = LoggerFactory.getLogger(GlobalPortSyncScheduler.class);

  private final GlobalPortSyncService syncService;

  @Value("${quote.masterdata.global-port.sync.enabled:true}")
  private boolean syncEnabled;

  public GlobalPortSyncScheduler(GlobalPortSyncService syncService) {
    this.syncService = syncService;
  }

  @Scheduled(cron = "${quote.masterdata.global-port.sync.cron:0 0 4 1 1,7 ?}")
  public void scheduledSync() {
    if (!syncEnabled) {
      return;
    }
    try {
      log.info("开始定时 UN/LOCODE 增量同步…");
      syncService.executeIncrementalSync();
    } catch (Exception ex) {
      log.error("定时 UN/LOCODE 同步失败：{}", ex.getMessage(), ex);
    }
  }
}
