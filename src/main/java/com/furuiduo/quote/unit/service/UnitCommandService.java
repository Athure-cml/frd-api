package com.furuiduo.quote.unit.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.unit.dto.UnitResponse;
import com.furuiduo.quote.unit.dto.UnitSaveRequest;
import com.furuiduo.quote.unit.entity.Unit;
import com.furuiduo.quote.unit.repository.UnitRepository;

@Service
public class UnitCommandService {

  private final UnitRepository unitRepository;

  public UnitCommandService(UnitRepository unitRepository) {
    this.unitRepository = unitRepository;
  }

  public UnitResponse getById(Long id) {
    return UnitResponse.from(requireEntity(id));
  }

  @Transactional
  public UnitResponse create(UnitSaveRequest request) {
    validateSaveRequest(request, null);
    Unit unit = new Unit();
    apply(unit, request);
    return UnitResponse.from(unitRepository.save(unit));
  }

  @Transactional
  public UnitResponse update(Long id, UnitSaveRequest request) {
    Unit unit = requireEntity(id);
    validateSaveRequest(request, unit.getId());
    apply(unit, request);
    unit.setUpdatedAt(LocalDateTime.now());
    return UnitResponse.from(unitRepository.save(unit));
  }

  @Transactional
  public void delete(Long id) {
    Unit unit = requireEntity(id);
    unitRepository.delete(unit);
  }

  private Unit requireEntity(Long id) {
    return unitRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "单位不存在"));
  }

  private void validateSaveRequest(UnitSaveRequest request, Long excludeId) {
    if (request.code() == null || request.code().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "单位编码不能为空");
    }
    if (request.name() == null || request.name().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "单位名称不能为空");
    }
    if (request.status() != null && request.status() != 0 && request.status() != 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的单位状态");
    }
    String code = normalizeCode(request.code());
    if (unitRepository.existsByCodeNormalized(code, excludeId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "单位编码已存在：" + code);
    }
  }

  private void apply(Unit unit, UnitSaveRequest request) {
    unit.setCode(normalizeCode(request.code()));
    unit.setName(request.name().trim());
    unit.setRemark(trimToNull(request.remark()));
    unit.setSort(request.sort() == null ? 0 : request.sort());
    unit.setStatus(request.status() == null ? 1 : request.status());
    unit.setUpdatedAt(LocalDateTime.now());
  }

  private static String normalizeCode(String code) {
    return code.trim();
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
