package com.furuiduo.quote.masterdata.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.furuiduo.quote.masterdata.entity.MdGlobalPort;
import com.furuiduo.quote.masterdata.integration.unlocode.UnlocodeRecord;
import com.furuiduo.quote.masterdata.repository.MdGlobalPortRepository;

@Service
public class GlobalPortImportService {

  private final MdGlobalPortRepository portRepository;
  private final GlobalPortImportBatchService batchService;

  public GlobalPortImportService(
      MdGlobalPortRepository portRepository, GlobalPortImportBatchService batchService) {
    this.portRepository = portRepository;
    this.batchService = batchService;
  }

  public ImportResult upsertAll(
      List<UnlocodeRecord> records, String dataVersion, boolean insertOnly) {
    Set<String> existingAtStart = new HashSet<>(portRepository.findAllCodes());
    Map<String, UnlocodeRecord> recordByCode = new HashMap<>();
    int skipped = 0;

    for (UnlocodeRecord record : records) {
      if (!record.importable()) {
        skipped++;
        continue;
      }
      recordByCode.put(record.code(), record);
    }

    int inserted = 0;
    int updated = 0;
    List<MdGlobalPort> insertBatch = new ArrayList<>();
    int batchSize = GlobalPortImportBatchService.batchSize();

    for (UnlocodeRecord record : recordByCode.values()) {
      if (existingAtStart.contains(record.code())) {
        continue;
      }
      insertBatch.add(GlobalPortImportBatchService.toEntity(record, dataVersion));
      if (insertBatch.size() >= batchSize) {
        inserted += batchService.saveInsertBatch(insertBatch);
        insertBatch.clear();
      }
    }
    if (!insertBatch.isEmpty()) {
      inserted += batchService.saveInsertBatch(insertBatch);
    }

    if (!insertOnly) {
      List<String> updateCodes =
          recordByCode.keySet().stream().filter(existingAtStart::contains).toList();
      for (List<String> batch : GlobalPortImportBatchService.partition(updateCodes, batchSize)) {
        int batchUpdated = batchService.updateBatch(batch, recordByCode, dataVersion);
        updated += batchUpdated;
        skipped += batch.size() - batchUpdated;
      }
    } else {
      skipped +=
          recordByCode.keySet().stream().filter(existingAtStart::contains).count();
    }

    return new ImportResult(inserted, updated, skipped);
  }

  public record ImportResult(int inserted, int updated, int skipped) {}
}
