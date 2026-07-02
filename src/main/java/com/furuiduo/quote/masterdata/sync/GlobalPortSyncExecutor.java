package com.furuiduo.quote.masterdata.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.furuiduo.quote.masterdata.service.GlobalPortSyncService;

@Component
public class GlobalPortSyncExecutor {

  private static final Logger log = LoggerFactory.getLogger(GlobalPortSyncExecutor.class);

  private final GlobalPortSyncService syncService;
  private final GlobalPortSyncTaskHolder taskHolder;

  public GlobalPortSyncExecutor(
      GlobalPortSyncService syncService, GlobalPortSyncTaskHolder taskHolder) {
    this.syncService = syncService;
    this.taskHolder = taskHolder;
  }

  @Async
  public void runIncrementalSync() {
    try {
      syncService.executeIncrementalSync();
    } catch (Exception ex) {
      log.error("UN/LOCODE 异步同步失败：{}", ex.getMessage(), ex);
      taskHolder.fail(ex.getMessage() == null ? "同步失败" : ex.getMessage());
    }
  }
}
