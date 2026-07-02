package com.furuiduo.quote.masterdata.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.common.SearchText;
import com.furuiduo.quote.masterdata.dto.UsStateResponse;
import com.furuiduo.quote.masterdata.dto.UsStateSaveRequest;
import com.furuiduo.quote.masterdata.entity.MdUsState;
import com.furuiduo.quote.masterdata.repository.MdDestCityRepository;
import com.furuiduo.quote.masterdata.repository.MdUsStateRepository;

@Service
public class UsStateService {

  private final MdUsStateRepository repository;
  private final MdDestCityRepository cityRepository;

  public UsStateService(MdUsStateRepository repository, MdDestCityRepository cityRepository) {
    this.repository = repository;
    this.cityRepository = cityRepository;
  }

  @Transactional(readOnly = true)
  public List<UsStateResponse> list(String code, String nameZh) {
    String normalizedCode = SearchText.orEmpty(code);
    String normalizedNameZh = SearchText.orEmpty(nameZh);
    if (normalizedCode.isEmpty() && normalizedNameZh.isEmpty()) {
      return repository.findAll(Sort.by("code")).stream().map(UsStateResponse::from).toList();
    }
    return repository.search(normalizedCode, normalizedNameZh).stream()
        .map(UsStateResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public UsStateResponse getById(Long id) {
    return UsStateResponse.from(requireEntity(id));
  }

  @Transactional
  public UsStateResponse create(UsStateSaveRequest request) {
    validateSave(request, null);
    MdUsState entity = new MdUsState();
    apply(entity, request);
    return UsStateResponse.from(repository.save(entity));
  }

  @Transactional
  public UsStateResponse update(Long id, UsStateSaveRequest request) {
    MdUsState entity = requireEntity(id);
    validateSave(request, entity);
    apply(entity, request);
    return UsStateResponse.from(repository.save(entity));
  }

  @Transactional
  public void delete(Long id) {
    MdUsState entity = requireEntity(id);
    if (cityRepository.existsByStateId(entity.getId())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "该州下存在城市，无法删除");
    }
    repository.delete(entity);
  }

  @Transactional(readOnly = true)
  public MdUsState requireByCode(String code) {
    return repository
        .findByCode(normalizeCode(code))
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "州代码不存在：" + code));
  }

  private MdUsState requireEntity(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "州不存在"));
  }

  private void validateSave(UsStateSaveRequest request, MdUsState existing) {
    if (request.code() == null || request.code().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "州代码不能为空");
    }
    if (request.nameZh() == null || request.nameZh().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "中文名称不能为空");
    }
    String code = normalizeCode(request.code());
    if (existing == null && repository.existsByCode(code)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "州代码已存在");
    }
    if (existing != null
        && !existing.getCode().equals(code)
        && repository.existsByCode(code)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "州代码已存在");
    }
  }

  private void apply(MdUsState entity, UsStateSaveRequest request) {
    entity.setCode(normalizeCode(request.code()));
    entity.setNameZh(request.nameZh().trim());
  }

  private String normalizeCode(String code) {
    return code.trim().toUpperCase();
  }
}
