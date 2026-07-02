package com.furuiduo.quote.masterdata.service;

import java.io.IOException;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.masterdata.dto.GlobalPortSyncResult;
import com.furuiduo.quote.masterdata.dto.GlobalPortSyncStatus;
import com.furuiduo.quote.masterdata.entity.MdDataSyncMeta;
import com.furuiduo.quote.masterdata.integration.unlocode.UnlocodeDataReader;
import com.furuiduo.quote.masterdata.integration.unlocode.UnlocodeDataset;
import com.furuiduo.quote.masterdata.repository.MdDataSyncMetaRepository;
import com.furuiduo.quote.masterdata.repository.MdGlobalPortRepository;
import com.furuiduo.quote.masterdata.service.GlobalPortImportService.ImportResult;
import com.furuiduo.quote.masterdata.sync.GlobalPortSyncTaskHolder;

@Service
public class GlobalPortSyncService {

  private static final Logger log = LoggerFactory.getLogger(GlobalPortSyncService.class);
  static final String SYNC_KEY = "global_port_unlocode";

  private final UnlocodeDataReader dataReader;
  private final GlobalPortImportService importService;
  private final MdGlobalPortRepository portRepository;
  private final MdDataSyncMetaRepository syncMetaRepository;
  private final GlobalPortSyncTaskHolder taskHolder;

  public GlobalPortSyncService(
      UnlocodeDataReader dataReader,
      GlobalPortImportService importService,
      MdGlobalPortRepository portRepository,
      MdDataSyncMetaRepository syncMetaRepository,
      GlobalPortSyncTaskHolder taskHolder) {
    this.dataReader = dataReader;
    this.importService = importService;
    this.portRepository = portRepository;
    this.syncMetaRepository = syncMetaRepository;
    this.taskHolder = taskHolder;
  }

  public GlobalPortSyncStatus getStatus() {
    return taskHolder.current();
  }

  public synchronized GlobalPortSyncStatus prepareAsyncSync() {
    if (!taskHolder.tryStart()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "同步正在进行中，请稍后再试");
    }
    return taskHolder.current();
  }

  public void resetSyncState() {
    taskHolder.forceReset();
  }

  public void executeIncrementalSync() throws IOException {
    taskHolder.markPhase("LOADING");
    UnlocodeDataset dataset = dataReader.load();
    taskHolder.markPhase("IMPORTING");
    ImportResult result = importService.upsertAll(dataset.records(), dataset.dataVersion(), false);
    LocalDateTime syncedAt = LocalDateTime.now();
    saveSyncMeta(dataset, result, syncedAt);
    GlobalPortSyncResult syncResult =
        new GlobalPortSyncResult(
            dataset.dataVersion(),
            dataset.records().size(),
            result.inserted(),
            result.updated(),
            result.skipped(),
            syncedAt);
    taskHolder.complete(syncResult);
    log.info(
        "全球港口 UN/LOCODE 增量同步完成：解析 {} 条，新增 {}，更新 {}，跳过 {}",
        dataset.records().size(),
        result.inserted(),
        result.updated(),
        result.skipped());
  }

  @Transactional
  public GlobalPortSyncResult initializeIfEmpty() throws IOException {
    if (portRepository.count() > 0) {
      return null;
    }
    UnlocodeDataset dataset = dataReader.load();
    ImportResult result = importService.upsertAll(dataset.records(), dataset.dataVersion(), true);
    LocalDateTime syncedAt = LocalDateTime.now();
    saveSyncMeta(dataset, result, syncedAt);
    log.info(
        "全球港口 UN/LOCODE 全量初始化完成：解析 {} 条，新增 {}",
        dataset.records().size(),
        result.inserted());
    return new GlobalPortSyncResult(
        dataset.dataVersion(),
        dataset.records().size(),
        result.inserted(),
        result.updated(),
        result.skipped(),
        syncedAt);
  }

  @Transactional
  public void saveSyncMeta(UnlocodeDataset dataset, ImportResult result, LocalDateTime syncedAt) {
    MdDataSyncMeta meta =
        syncMetaRepository
            .findBySyncKey(SYNC_KEY)
            .orElseGet(
                () -> {
                  MdDataSyncMeta created = new MdDataSyncMeta();
                  created.setSyncKey(SYNC_KEY);
                  return created;
                });
    meta.setDataVersion(dataset.dataVersion());
    meta.setLastSyncAt(syncedAt);
    meta.setTotalRecords(dataset.records().size());
    meta.setInsertedCount(result.inserted());
    meta.setUpdatedCount(result.updated());
    meta.setRemark("skipped=" + result.skipped());
    syncMetaRepository.save(meta);
  }
}
