package com.furuiduo.quote.supplier.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.common.PartyMasterExcelSupport;
import com.furuiduo.quote.supplier.dto.SupplierTypeResponse;
import com.furuiduo.quote.supplier.dto.SupplierTypeSaveRequest;
import com.furuiduo.quote.supplier.entity.SupplierType;
import com.furuiduo.quote.supplier.repository.SupplierRepository;
import com.furuiduo.quote.supplier.repository.SupplierTypeRepository;
import com.furuiduo.quote.supplier.support.SupplierOtherTypes;

@Service
public class SupplierTypeService {

  private final SupplierTypeRepository supplierTypeRepository;
  private final SupplierRepository supplierRepository;

  public SupplierTypeService(
      SupplierTypeRepository supplierTypeRepository, SupplierRepository supplierRepository) {
    this.supplierTypeRepository = supplierTypeRepository;
    this.supplierRepository = supplierRepository;
  }

  @Transactional(readOnly = true)
  public List<SupplierTypeResponse> list(boolean enabledOnly) {
    List<SupplierType> items =
        enabledOnly
            ? supplierTypeRepository.findByStatusOrderBySortOrderAscIdAsc(1)
            : supplierTypeRepository.findAllByOrderBySortOrderAscIdAsc();
    return items.stream().map(this::toResponse).toList();
  }

  @Transactional
  public SupplierTypeResponse create(SupplierTypeSaveRequest request) {
    String name = requireName(request);
    assertNameAvailable(name, null);
    SupplierType type = new SupplierType();
    type.setName(name);
    type.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    type.setStatus(resolveStatus(request.status(), 1));
    type.setCreatedAt(LocalDateTime.now());
    type.setUpdatedAt(LocalDateTime.now());
    return toResponse(supplierTypeRepository.save(type));
  }

  @Transactional
  public SupplierTypeResponse update(Long id, SupplierTypeSaveRequest request) {
    SupplierType type =
        supplierTypeRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "类型不存在"));
    String name = requireName(request);
    assertNameAvailable(name, id);
    type.setName(name);
    if (request.sortOrder() != null) {
      type.setSortOrder(request.sortOrder());
    }
    if (request.status() != null) {
      type.setStatus(resolveStatus(request.status(), type.getStatus()));
    }
    type.setUpdatedAt(LocalDateTime.now());
    return toResponse(supplierTypeRepository.save(type));
  }

  @Transactional
  public void delete(Long id) {
    SupplierType type =
        supplierTypeRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "类型不存在"));
    if (isInUse(id)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该类型已被使用，无法删除");
    }
    supplierTypeRepository.delete(type);
  }

  private SupplierTypeResponse toResponse(SupplierType type) {
    return SupplierTypeResponse.from(type, isInUse(type.getId()));
  }

  private boolean isInUse(Long typeId) {
    return supplierRepository.existsOtherSupplierUsingType(SupplierOtherTypes.usageJson(typeId));
  }

  private String requireName(SupplierTypeSaveRequest request) {
    if (request == null || request.name() == null || request.name().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "类型名称不能为空");
    }
    return PartyMasterExcelSupport.normalizeName(request.name());
  }

  private void assertNameAvailable(String name, Long excludeId) {
    if (supplierTypeRepository.existsByNameNormalized(name, excludeId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "类型名称已存在：" + name);
    }
  }

  private int resolveStatus(Integer status, int fallback) {
    if (status == null) {
      return fallback;
    }
    if (status != 0 && status != 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的类型状态");
    }
    return status;
  }
}
