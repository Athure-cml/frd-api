package com.furuiduo.quote.sys.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.sys.dto.OperationLogResponse;
import com.furuiduo.quote.sys.entity.OperationAction;
import com.furuiduo.quote.sys.entity.SysOperationLog;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.repository.SysOperationLogRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class OperationLogService {

  private final SysOperationLogRepository operationLogRepository;

  public OperationLogService(SysOperationLogRepository operationLogRepository) {
    this.operationLogRepository = operationLogRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordSuccess(
      SysUser user,
      String module,
      OperationAction action,
      String resourceType,
      String resourceId,
      String summary,
      String requestMethod,
      String requestUri,
      String requestBody,
      String ipAddress) {
    SysOperationLog log = baseLog(user, module, action, resourceType, resourceId, summary);
    log.setRequestMethod(requestMethod);
    log.setRequestUri(requestUri);
    log.setRequestBody(requestBody);
    log.setIpAddress(ipAddress);
    log.setSuccess(true);
    operationLogRepository.save(log);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordFailure(
      SysUser user,
      String module,
      OperationAction action,
      String resourceType,
      String resourceId,
      String summary,
      String requestMethod,
      String requestUri,
      String requestBody,
      String ipAddress,
      String errorMessage) {
    SysOperationLog log = baseLog(user, module, action, resourceType, resourceId, summary);
    log.setRequestMethod(requestMethod);
    log.setRequestUri(requestUri);
    log.setRequestBody(requestBody);
    log.setIpAddress(ipAddress);
    log.setSuccess(false);
    log.setErrorMessage(truncate(errorMessage, 512));
    operationLogRepository.save(log);
  }

  public PageResult<OperationLogResponse> list(
      int page,
      int pageSize,
      String module,
      OperationAction action,
      String username,
      String keyword,
      LocalDateTime startAt,
      LocalDateTime endAt) {
    int safePage = Math.max(page, 1);
    int safeSize = Math.min(Math.max(pageSize, 1), 100);
    PageRequest pageable =
        PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

    Specification<SysOperationLog> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          if (module != null && !module.isBlank()) {
            predicates.add(cb.equal(root.get("module"), module.trim()));
          }
          if (action != null) {
            predicates.add(cb.equal(root.get("action"), action));
          }
          if (username != null && !username.isBlank()) {
            predicates.add(
                cb.like(cb.upper(root.get("username")), "%" + username.trim().toLowerCase() + "%"));
          }
          if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim().toLowerCase() + "%";
            predicates.add(
                cb.or(
                    cb.like(cb.upper(root.get("summary")), like),
                    cb.like(cb.upper(root.get("requestUri")), like),
                    cb.like(cb.upper(root.get("realName")), like)));
          }
          if (startAt != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startAt));
          }
          if (endAt != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endAt));
          }
          return cb.and(predicates.toArray(Predicate[]::new));
        };

    Page<SysOperationLog> result = operationLogRepository.findAll(spec, pageable);
    List<OperationLogResponse> items = result.getContent().stream().map(OperationLogResponse::from).toList();
    return new PageResult<>(items, result.getTotalElements());
  }

  public OperationLogResponse getById(Long id) {
    SysOperationLog log =
        operationLogRepository
            .findById(id)
            .orElseThrow(
                () -> new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Operation log not found"));
    return OperationLogResponse.from(log);
  }

  private SysOperationLog baseLog(
      SysUser user,
      String module,
      OperationAction action,
      String resourceType,
      String resourceId,
      String summary) {
    SysOperationLog log = new SysOperationLog();
    if (user != null) {
      log.setUserId(user.getId());
      log.setUsername(user.getUsername());
      log.setRealName(user.getRealName());
    }
    log.setModule(module);
    log.setAction(action);
    log.setResourceType(resourceType);
    log.setResourceId(resourceId);
    log.setSummary(summary);
    log.setCreatedAt(LocalDateTime.now());
    return log;
  }

  private String truncate(String value, int max) {
    if (value == null) {
      return null;
    }
    if (value.length() <= max) {
      return value;
    }
    return value.substring(0, max);
  }
}
