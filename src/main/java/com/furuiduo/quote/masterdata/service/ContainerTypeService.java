package com.furuiduo.quote.masterdata.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.common.SearchText;
import com.furuiduo.quote.masterdata.dto.ContainerTypeResponse;
import com.furuiduo.quote.masterdata.dto.ContainerTypeSaveRequest;
import com.furuiduo.quote.masterdata.entity.MdContainerType;
import com.furuiduo.quote.masterdata.repository.MdContainerTypeRepository;

@Service
public class ContainerTypeService {

  private final MdContainerTypeRepository repository;

  public ContainerTypeService(MdContainerTypeRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<ContainerTypeResponse> list(String code, String name) {
    String normalizedCode = SearchText.orEmpty(code);
    String normalizedName = SearchText.orEmpty(name);
    if (normalizedCode.isEmpty() && normalizedName.isEmpty()) {
      return repository.findAll(Sort.by(Sort.Order.asc("sort"), Sort.Order.asc("code"))).stream()
          .map(ContainerTypeResponse::from)
          .toList();
    }
    return repository.search(normalizedCode, normalizedName).stream()
        .map(ContainerTypeResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ContainerTypeResponse> listEnabled() {
    return repository.findByStatusOrderBySortAscCodeAsc(1).stream()
        .map(ContainerTypeResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public ContainerTypeResponse getById(Long id) {
    return ContainerTypeResponse.from(requireEntity(id));
  }

  @Transactional
  public ContainerTypeResponse create(ContainerTypeSaveRequest request) {
    validateSave(request, null);
    MdContainerType entity = new MdContainerType();
    apply(entity, request);
    return ContainerTypeResponse.from(repository.save(entity));
  }

  @Transactional
  public ContainerTypeResponse update(Long id, ContainerTypeSaveRequest request) {
    MdContainerType entity = requireEntity(id);
    validateSave(request, entity);
    apply(entity, request);
    return ContainerTypeResponse.from(repository.save(entity));
  }

  @Transactional
  public void delete(Long id) {
    repository.delete(requireEntity(id));
  }

  private MdContainerType requireEntity(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "箱型不存在"));
  }

  private void validateSave(ContainerTypeSaveRequest request, MdContainerType existing) {
    if (request.code() == null || request.code().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "箱型代码不能为空");
    }
    if (request.name() == null || request.name().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "名称不能为空");
    }
    String code = normalizeCode(request.code());
    if (existing == null && repository.existsByCode(code)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "箱型代码已存在");
    }
    if (existing != null
        && !existing.getCode().equals(code)
        && repository.existsByCode(code)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "箱型代码已存在");
    }
  }

  private void apply(MdContainerType entity, ContainerTypeSaveRequest request) {
    entity.setCode(normalizeCode(request.code()));
    entity.setName(request.name().trim());
    entity.setSort(request.sort() == null ? 0 : request.sort());
    entity.setStatus(request.status() == null ? 1 : request.status());
    entity.setRemark(
        request.remark() == null || request.remark().isBlank() ? null : request.remark().trim());
  }

  private String normalizeCode(String code) {
    return code.trim().toUpperCase();
  }
}
