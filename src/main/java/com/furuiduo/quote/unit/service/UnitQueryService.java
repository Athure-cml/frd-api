package com.furuiduo.quote.unit.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.furuiduo.quote.unit.dto.UnitResponse;
import com.furuiduo.quote.unit.repository.UnitRepository;

@Service
public class UnitQueryService {

  private final UnitRepository unitRepository;

  public UnitQueryService(UnitRepository unitRepository) {
    this.unitRepository = unitRepository;
  }

  public List<UnitResponse> list(String code, String name, Integer status) {
    String normalizedCode = code == null ? "" : code.trim();
    String normalizedName = name == null ? "" : name.trim();
    if (normalizedCode.isEmpty() && normalizedName.isEmpty() && status == null) {
      return unitRepository.findAll(Sort.by("sort").ascending().and(Sort.by("code"))).stream()
          .map(UnitResponse::from)
          .toList();
    }
    if (normalizedCode.isEmpty() && normalizedName.isEmpty() && status != null) {
      return unitRepository.findByStatusOrderBySortAscCodeAsc(status).stream()
          .map(UnitResponse::from)
          .toList();
    }
    return unitRepository.search(normalizedCode, normalizedName, status).stream()
        .map(UnitResponse::from)
        .toList();
  }
}
