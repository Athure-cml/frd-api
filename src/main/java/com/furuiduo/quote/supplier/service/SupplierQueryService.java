package com.furuiduo.quote.supplier.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.common.SearchText;
import com.furuiduo.quote.supplier.dto.SupplierResponse;
import com.furuiduo.quote.supplier.repository.SupplierRepository;

@Service
public class SupplierQueryService {

  private final SupplierRepository supplierRepository;

  public SupplierQueryService(SupplierRepository supplierRepository) {
    this.supplierRepository = supplierRepository;
  }

  public PageResult<SupplierResponse> list(
      int page, int pageSize, String code, String name, Integer status) {
    var pageable =
        PageRequest.of(
            Math.max(page - 1, 0),
            Math.min(Math.max(pageSize, 1), 200),
            Sort.by(Sort.Direction.DESC, "updatedAt"));
    var result =
        supplierRepository.search(
            SearchText.orEmpty(code), SearchText.orEmpty(name), status, pageable);
    return new PageResult<>(
        result.getContent().stream().map(SupplierResponse::from).toList(),
        result.getTotalElements());
  }
}
