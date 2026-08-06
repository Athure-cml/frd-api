package com.furuiduo.quote.shippingline.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.common.SearchText;
import com.furuiduo.quote.shippingline.dto.ShippingLineResponse;
import com.furuiduo.quote.shippingline.repository.ShippingLineRepository;

@Service
public class ShippingLineQueryService {

  private final ShippingLineRepository repository;

  public ShippingLineQueryService(ShippingLineRepository repository) {
    this.repository = repository;
  }

  public PageResult<ShippingLineResponse> list(
      int page, int pageSize, String code, String name, Integer status) {
    var pageable =
        PageRequest.of(
            Math.max(page - 1, 0),
            Math.min(Math.max(pageSize, 1), 200),
            Sort.by(Sort.Direction.DESC, "updatedAt"));
    var result =
        repository.search(SearchText.orEmpty(code), SearchText.orEmpty(name), status, pageable);
    return new PageResult<>(
        result.getContent().stream().map(ShippingLineResponse::from).toList(),
        result.getTotalElements());
  }
}
