package com.furuiduo.quote.approval.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.approval.dto.ApprovalConfigFlowStepRequest;
import com.furuiduo.quote.approval.dto.ApprovalConfigResponse;
import com.furuiduo.quote.approval.dto.ApprovalConfigSaveRequest;
import com.furuiduo.quote.approval.entity.ApprovalConfig;
import com.furuiduo.quote.approval.entity.ApprovalConfigStep;
import com.furuiduo.quote.approval.repository.ApprovalConfigRepository;
import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.common.SearchText;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.repository.SysUserRepository;

@Service
public class ApprovalConfigService {

  private static final String OBJECT_QUOTE = "QUOTE";

  private final ApprovalConfigRepository approvalConfigRepository;
  private final SysUserRepository userRepository;

  public ApprovalConfigService(
      ApprovalConfigRepository approvalConfigRepository, SysUserRepository userRepository) {
    this.approvalConfigRepository = approvalConfigRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public PageResult<ApprovalConfigResponse> list(
      int page, int pageSize, String configNo, String configObject) {
    var result =
        approvalConfigRepository.search(
            SearchText.orEmpty(configNo),
            SearchText.orEmpty(configObject),
            PageRequest.of(
                Math.max(page - 1, 0),
                Math.max(pageSize, 1),
                Sort.by(Sort.Direction.DESC, "id")));
    List<ApprovalConfigResponse> items =
        result.getContent().stream().map(ApprovalConfigResponse::from).toList();
    return new PageResult<>(items, result.getTotalElements());
  }

  @Transactional
  public ApprovalConfigResponse create(ApprovalConfigSaveRequest request) {
    List<ResolvedStep> steps = validateAndResolveSteps(request);
    ApprovalConfig config = new ApprovalConfig();
    config.setConfigObject(normalizeObject(request.configObject()));
    config.setConfigNo("AC-TMP-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10));
    applySteps(config, steps);
    ApprovalConfig saved = approvalConfigRepository.save(config);
    saved.setConfigNo(buildConfigNo(saved.getId()));
    return ApprovalConfigResponse.from(approvalConfigRepository.save(saved));
  }

  @Transactional
  public ApprovalConfigResponse update(Long id, ApprovalConfigSaveRequest request) {
    List<ResolvedStep> steps = validateAndResolveSteps(request);
    ApprovalConfig config =
        approvalConfigRepository
            .findWithStepsById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "审批配置不存在"));
    config.setConfigObject(normalizeObject(request.configObject()));
    config.setUpdatedAt(LocalDateTime.now());
    applySteps(config, steps);
    return ApprovalConfigResponse.from(approvalConfigRepository.save(config));
  }

  @Transactional(readOnly = true)
  public List<ApprovalConfigResponse.FlowStep> findQuoteFlowSteps() {
    List<ApprovalConfig> configs =
        approvalConfigRepository.findByConfigObjectOrderByUpdatedAtDescIdDesc(OBJECT_QUOTE);
    if (configs.isEmpty()) {
      return List.of();
    }
    return configs.get(0).getSteps().stream()
        .map(ApprovalConfigResponse.FlowStep::from)
        .toList();
  }

  @Transactional
  public void delete(Long id) {
    ApprovalConfig config =
        approvalConfigRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "审批配置不存在"));
    approvalConfigRepository.delete(config);
  }

  private void applySteps(ApprovalConfig config, List<ResolvedStep> steps) {
    config.getSteps().clear();
    int order = 1;
    for (ResolvedStep resolved : steps) {
      ApprovalConfigStep step = new ApprovalConfigStep();
      step.setConfig(config);
      step.setSortOrder(order++);
      step.setApproverId(resolved.approverId());
      step.setApproverName(resolved.approverName());
      config.getSteps().add(step);
    }
  }

  private List<ResolvedStep> validateAndResolveSteps(ApprovalConfigSaveRequest request) {
    if (request == null || request.flowSteps() == null || request.flowSteps().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请至少配置一个审批节点");
    }
    Set<Long> seen = new HashSet<>();
    List<ResolvedStep> resolved = new java.util.ArrayList<>();
    for (ApprovalConfigFlowStepRequest step : request.flowSteps()) {
      if (step == null || step.approverId() == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择审批人");
      }
      if (!seen.add(step.approverId())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "同一审批人不能重复出现");
      }
      SysUser user =
          userRepository
              .findById(step.approverId())
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "审批人不存在"));
      String name =
          user.getRealName() == null || user.getRealName().isBlank()
              ? user.getUsername()
              : user.getRealName();
      resolved.add(new ResolvedStep(user.getId(), name));
    }
    return resolved;
  }

  private String normalizeObject(String configObject) {
    String value = configObject == null || configObject.isBlank() ? OBJECT_QUOTE : configObject;
    if (!OBJECT_QUOTE.equals(value)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "暂不支持该配置对象");
    }
    return value;
  }

  private static String buildConfigNo(Long id) {
    return "AC-" + String.format("%06d", id);
  }

  private record ResolvedStep(Long approverId, String approverName) {}
}
