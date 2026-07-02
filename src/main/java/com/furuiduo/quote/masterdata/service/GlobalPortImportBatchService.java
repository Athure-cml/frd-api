package com.furuiduo.quote.masterdata.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.masterdata.entity.MdGlobalPort;
import com.furuiduo.quote.masterdata.integration.unlocode.UnlocodeRecord;
import com.furuiduo.quote.masterdata.repository.MdGlobalPortRepository;
import com.furuiduo.quote.masterdata.seed.GlobalPortRouteResolver;

@Service
public class GlobalPortImportBatchService {

  private static final int BATCH_SIZE = 500;

  private final MdGlobalPortRepository portRepository;

  public GlobalPortImportBatchService(MdGlobalPortRepository portRepository) {
    this.portRepository = portRepository;
  }

  @Transactional
  public int saveInsertBatch(List<MdGlobalPort> batch) {
    portRepository.saveAll(batch);
    return batch.size();
  }

  @Transactional
  public int updateBatch(
      List<String> codes, Map<String, UnlocodeRecord> recordByCode, String dataVersion) {
    if (codes.isEmpty()) {
      return 0;
    }
    int updated = 0;
    List<MdGlobalPort> entities = portRepository.findByCodeIn(codes);
    for (MdGlobalPort entity : entities) {
      UnlocodeRecord record = recordByCode.get(entity.getCode().toUpperCase());
      if (record != null && applyChanges(entity, record, dataVersion)) {
        updated++;
      }
    }
    portRepository.saveAll(entities);
    return updated;
  }

  private boolean applyChanges(MdGlobalPort entity, UnlocodeRecord record, String dataVersion) {
    boolean changed = false;
    if (!Objects.equals(entity.getNameEn(), record.nameEn())) {
      entity.setNameEn(record.nameEn());
      changed = true;
    }
    String route = GlobalPortRouteResolver.resolveRoute(record.country());
    if (!Objects.equals(entity.getRoute(), route)) {
      entity.setRoute(route);
      changed = true;
    }
    String countryRegion = GlobalPortRouteResolver.resolveCountryRegion(record.country());
    if (!Objects.equals(entity.getCountryRegion(), countryRegion)) {
      entity.setCountryRegion(countryRegion);
      changed = true;
    }
    if (entity.getPortType() != record.portType()) {
      entity.setPortType(record.portType());
      changed = true;
    }
    if (!Objects.equals(entity.getFunctionCode(), record.functionCode())) {
      entity.setFunctionCode(record.functionCode());
      changed = true;
    }
    if (!Objects.equals(entity.getLocodeStatus(), record.locodeStatus())) {
      entity.setLocodeStatus(record.locodeStatus());
      changed = true;
    }
    if (dataVersion != null && !Objects.equals(entity.getDataVersion(), dataVersion)) {
      entity.setDataVersion(dataVersion);
      changed = true;
    }
    return changed;
  }

  static MdGlobalPort toEntity(UnlocodeRecord record, String dataVersion) {
    MdGlobalPort entity = new MdGlobalPort();
    entity.setCode(record.code());
    entity.setNameEn(record.nameEn());
    entity.setNameZh(null);
    entity.setRoute(GlobalPortRouteResolver.resolveRoute(record.country()));
    entity.setCountryRegion(GlobalPortRouteResolver.resolveCountryRegion(record.country()));
    entity.setPortType(record.portType());
    entity.setFunctionCode(record.functionCode());
    entity.setLocodeStatus(record.locodeStatus());
    entity.setDataVersion(dataVersion);
    return entity;
  }

  static List<List<String>> partition(List<String> items, int size) {
    List<List<String>> parts = new ArrayList<>();
    for (int i = 0; i < items.size(); i += size) {
      parts.add(items.subList(i, Math.min(i + size, items.size())));
    }
    return parts;
  }

  static int batchSize() {
    return BATCH_SIZE;
  }
}
