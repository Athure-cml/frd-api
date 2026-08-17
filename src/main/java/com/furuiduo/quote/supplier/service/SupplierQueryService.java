package com.furuiduo.quote.supplier.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.common.SearchText;
import com.furuiduo.quote.supplier.dto.SupplierResponse;
import com.furuiduo.quote.supplier.repository.SupplierRepository;
import com.furuiduo.quote.supplier.support.SupplierCategories;

@Service
public class SupplierQueryService {

  private final SupplierRepository supplierRepository;

  public SupplierQueryService(SupplierRepository supplierRepository) {
    this.supplierRepository = supplierRepository;
  }

  public PageResult<SupplierResponse> list(
      int page,
      int pageSize,
      String category,
      String code,
      String name,
      Integer status,
      String typeId) {
    String normalizedCategory = SupplierCategories.normalize(category);
    // 排序写在 SupplierRepository.search 的原生 SQL 中
    var pageable =
        PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(pageSize, 1), 200));
    var result =
        supplierRepository.search(
            normalizedCategory,
            SearchText.orEmpty(code),
            SearchText.orEmpty(name),
            status,
            SearchText.orEmpty(typeId),
            pageable);
    return new PageResult<>(
        result.getContent().stream().map(SupplierResponse::from).toList(),
        result.getTotalElements());
  }
}
