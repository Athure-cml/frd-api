package com.furuiduo.quote.agent.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.furuiduo.quote.agent.dto.AgentResponse;
import com.furuiduo.quote.agent.repository.AgentRepository;
import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.common.SearchText;

@Service
public class AgentQueryService {

  private final AgentRepository repository;

  public AgentQueryService(AgentRepository repository) {
    this.repository = repository;
  }

  public PageResult<AgentResponse> list(
      int page, int pageSize, String code, String name, Integer status) {
    var pageable =
        PageRequest.of(
            Math.max(page - 1, 0),
            Math.min(Math.max(pageSize, 1), 200),
            Sort.by(Sort.Direction.DESC, "updatedAt"));
    var result =
        repository.search(SearchText.orEmpty(code), SearchText.orEmpty(name), status, pageable);
    return new PageResult<>(
        result.getContent().stream().map(AgentResponse::from).toList(), result.getTotalElements());
  }
}
