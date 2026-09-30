package com.furuiduo.quote.quote.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.furuiduo.quote.approval.dto.ApprovalConfigResponse;
import com.furuiduo.quote.approval.service.ApprovalConfigService;
import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.common.SearchText;
import com.furuiduo.quote.quote.dto.QuoteApprovalDetailResponse;
import com.furuiduo.quote.quote.dto.QuoteApprovalListItem;
import com.furuiduo.quote.quote.dto.QuoteApprovalLogResponse;
import com.furuiduo.quote.quote.dto.QuoteApprovalWorkflowStepResponse;
import com.furuiduo.quote.quote.dto.QuoteDetailResponse;
import com.furuiduo.quote.quote.entity.QuoteApprovalAction;
import com.furuiduo.quote.quote.entity.QuoteApprovalLog;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.entity.QuoteStatus;
import com.furuiduo.quote.quote.repository.QuoteApprovalLogRepository;
import com.furuiduo.quote.quote.repository.QuoteOrderRepository;
import com.furuiduo.quote.quote.support.QuoteApprovalPrintSnapshotSupport;
import com.furuiduo.quote.quote.support.QuoteApprovalSupport;
import com.furuiduo.quote.quote.support.QuoteStatusSupport;
import com.furuiduo.quote.sys.entity.DataScope;
import com.furuiduo.quote.sys.entity.SysDepartment;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.repository.SysDepartmentRepository;
import com.furuiduo.quote.sys.service.PermissionService;

@Service
public class QuoteApprovalService {

  private final QuoteOrderRepository quoteOrderRepository;
  private final QuoteApprovalLogRepository quoteApprovalLogRepository;
  private final QuoteApprovalLogService quoteApprovalLogService;
  private final QuoteQueryService quoteQueryService;
  private final PermissionService permissionService;
  private final ApprovalConfigService approvalConfigService;
  private final SysDepartmentRepository sysDepartmentRepository;

  public QuoteApprovalService(
      QuoteOrderRepository quoteOrderRepository,
      QuoteApprovalLogRepository quoteApprovalLogRepository,
      QuoteApprovalLogService quoteApprovalLogService,
      QuoteQueryService quoteQueryService,
      PermissionService permissionService,
      ApprovalConfigService approvalConfigService,
      SysDepartmentRepository sysDepartmentRepository) {
    this.quoteOrderRepository = quoteOrderRepository;
    this.quoteApprovalLogRepository = quoteApprovalLogRepository;
    this.quoteApprovalLogService = quoteApprovalLogService;
    this.quoteQueryService = quoteQueryService;
    this.permissionService = permissionService;
    this.approvalConfigService = approvalConfigService;
    this.sysDepartmentRepository = sysDepartmentRepository;
  }

  public PageResult<QuoteApprovalListItem> list(
      SysUser user, int page, int pageSize, String approvalNo, String status) {
    DataScope dataScope = permissionService.getEffectiveDataScope(user);
    Long deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;
    Pageable pageable =
        PageRequest.of(
            Math.max(page - 1, 0),
            Math.max(pageSize, 1),
            Sort.by(Sort.Direction.DESC, "submittedAt")
                .and(Sort.by(Sort.Direction.DESC, "updatedAt")));

    var result =
        quoteOrderRepository.searchApprovalList(
            SearchText.orEmpty(approvalNo),
            normalizeStatusFilter(status),
            dataScope == DataScope.ALL,
            dataScope == DataScope.DEPT,
            dataScope == DataScope.SELF,
            deptId,
            user.getId(),
            PageRequest.of(0, 2000, pageable.getSort()));

    List<Long> ids = result.getContent().stream().map(QuoteOrder::getId).toList();
    Map<Long, List<QuoteApprovalLog>> logsByOrderId = loadLogs(ids);
    Map<Long, String> deptNames = loadDeptNames(result.getContent());
    List<QuoteApprovalSupport.FlowNode> currentFlow = toFlowNodes();

    List<QuoteApprovalListItem> items = new ArrayList<>();
    String statusFilter = normalizeStatusFilter(status);
    for (QuoteOrder order : result.getContent()) {
      List<QuoteApprovalLog> logs = logsByOrderId.getOrDefault(order.getId(), List.of());
      String deptName =
          order.getDeptId() == null ? "" : deptNames.getOrDefault(order.getDeptId(), "");
      List<List<QuoteApprovalLog>> cycles = QuoteApprovalSupport.splitCycles(logs);
      if (cycles.isEmpty()) {
        continue;
      }
      for (int i = cycles.size() - 1; i >= 0; i--) {
        List<QuoteApprovalLog> cycle = cycles.get(i);
        boolean latest = i == cycles.size() - 1;
        List<QuoteApprovalSupport.FlowNode> cycleFlow =
            QuoteApprovalSupport.resolveCycleFlow(cycle, currentFlow);
        QuoteApprovalSupport.ResolvedApproval overall =
            QuoteApprovalSupport.resolveCycle(order, cycle, cycleFlow, latest);
        boolean pendingForMe =
            latest
                && QuoteApprovalSupport.STATUS_PENDING.equals(overall.status())
                && QuoteApprovalSupport.isCurrentApprover(user.getId(), logs, cycleFlow);
        boolean iApproved =
            QuoteApprovalSupport.hasOperatorAction(
                cycle, QuoteApprovalAction.APPROVE, user.getId());
        boolean iRejected =
            QuoteApprovalSupport.hasOperatorAction(
                cycle, QuoteApprovalAction.REJECT, user.getId());
        boolean iWithdrawn =
            QuoteApprovalSupport.hasOperatorAction(
                cycle, QuoteApprovalAction.ROLLBACK, user.getId());
        boolean finished =
            QuoteApprovalSupport.STATUS_APPROVED.equals(overall.status())
                || QuoteApprovalSupport.STATUS_REJECTED.equals(overall.status())
                || QuoteApprovalSupport.STATUS_WITHDRAWN.equals(overall.status());
        if (!pendingForMe && !iApproved && !iRejected && !iWithdrawn && !finished) {
          continue;
        }
        if (!statusFilter.isEmpty()) {
          if (QuoteApprovalSupport.STATUS_PENDING.equals(statusFilter)) {
            if (!pendingForMe) {
              continue;
            }
          } else if (!statusFilter.equals(overall.status())) {
            continue;
          }
        }
        items.add(
            QuoteApprovalListItem.fromCycle(
                order, cycle, i + 1, latest, cycleFlow, deptName));
      }
    }

    int from = Math.min((pageable.getPageNumber()) * pageable.getPageSize(), items.size());
    int to = Math.min(from + pageable.getPageSize(), items.size());
    return new PageResult<>(items.subList(from, to), items.size());
  }

  public QuoteApprovalDetailResponse getDetail(SysUser user, Long id, Integer cycleIndex) {
    QuoteDetailResponse quote = quoteQueryService.getById(user, id);
    List<QuoteApprovalLogResponse> logs = quoteApprovalLogService.listByQuoteOrderId(id);
    QuoteStatus status = QuoteStatusSupport.parseDisplayStatus(quote.status());
    List<ApprovalConfigResponse.FlowStep> flow = approvalConfigService.findQuoteFlowSteps();
    List<QuoteApprovalSupport.FlowNode> flowNodes = toFlowNodes(flow);
    List<QuoteApprovalLog> entityLogs = quoteApprovalLogService.listEntities(id);
    List<List<QuoteApprovalLog>> entityCycles = QuoteApprovalSupport.splitCycles(entityLogs);
    List<List<QuoteApprovalLogResponse>> cycles = splitCycles(logs);
    int latestIndex = cycles.size();
    int selected =
        cycleIndex == null || cycleIndex < 1 || cycleIndex > latestIndex
            ? latestIndex
            : cycleIndex;
    boolean latest = selected == latestIndex;
    boolean pending = status == QuoteStatus.PENDING_APPROVAL && latest;
    List<QuoteApprovalLog> entityCycle =
        entityCycles.isEmpty() ? List.of() : entityCycles.get(selected - 1);
    List<QuoteApprovalSupport.FlowNode> cycleFlow =
        QuoteApprovalSupport.resolveCycleFlow(entityCycle, flowNodes);
    boolean pendingApproval =
        pending && QuoteApprovalSupport.isCurrentApprover(user.getId(), entityLogs, cycleFlow);
    List<QuoteApprovalLogResponse> cycle =
        cycles.isEmpty() ? List.of() : cycles.get(selected - 1);
    List<QuoteApprovalWorkflowStepResponse> steps =
        buildWorkflowSteps(quote, cycle, pending, toFlowSteps(cycleFlow));
    String printSnapshot =
        QuoteApprovalPrintSnapshotSupport.findCyclePrintSnapshot(entityCycle);
    QuoteDetailResponse quoteForView =
        QuoteApprovalPrintSnapshotSupport.applyToDetail(quote, printSnapshot);
    return new QuoteApprovalDetailResponse(quoteForView, pendingApproval, logs, steps);
  }

  private List<QuoteApprovalSupport.FlowNode> toFlowNodes() {
    return toFlowNodes(approvalConfigService.findQuoteFlowSteps());
  }

  private List<QuoteApprovalSupport.FlowNode> toFlowNodes(
      List<ApprovalConfigResponse.FlowStep> flow) {
    return flow.stream()
        .map(step -> new QuoteApprovalSupport.FlowNode(step.approverId(), step.approverName()))
        .toList();
  }

  private List<ApprovalConfigResponse.FlowStep> toFlowSteps(
      List<QuoteApprovalSupport.FlowNode> flow) {
    return flow.stream()
        .map(step -> new ApprovalConfigResponse.FlowStep(step.approverId(), step.approverName()))
        .toList();
  }

  private Map<Long, String> loadDeptNames(List<QuoteOrder> orders) {
    Set<Long> deptIds =
        orders.stream().map(QuoteOrder::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
    if (deptIds.isEmpty()) {
      return Map.of();
    }
    return sysDepartmentRepository.findAllById(deptIds).stream()
        .collect(Collectors.toMap(SysDepartment::getId, SysDepartment::getName, (a, b) -> a));
  }

  private Map<Long, List<QuoteApprovalLog>> loadLogs(List<Long> quoteOrderIds) {
    if (quoteOrderIds.isEmpty()) {
      return Map.of();
    }
    return quoteApprovalLogRepository
        .findByQuoteOrderIdInOrderByCreatedAtAscIdAsc(quoteOrderIds)
        .stream()
        .collect(Collectors.groupingBy(QuoteApprovalLog::getQuoteOrderId));
  }

  private String normalizeStatusFilter(String status) {
    if (status == null || status.isBlank()) {
      return "";
    }
    return switch (status.toUpperCase()) {
      case "PENDING", "APPROVED", "REJECTED", "WITHDRAWN" -> status.toUpperCase();
      default -> "";
    };
  }

  private List<QuoteApprovalWorkflowStepResponse> buildWorkflowSteps(
      QuoteDetailResponse quote,
      List<QuoteApprovalLogResponse> logs,
      boolean pending,
      List<ApprovalConfigResponse.FlowStep> flow) {
    List<QuoteApprovalWorkflowStepResponse> steps = new ArrayList<>();
    List<QuoteApprovalLogResponse> cycle = currentCycle(logs);
    List<QuoteApprovalLogResponse> cycleApproves = cycleActions(cycle, "APPROVE");
    QuoteApprovalLogResponse submitLog = findFirst(cycle, "SUBMIT");
    QuoteApprovalLogResponse rejectLog = findLast(cycle, "REJECT");
    QuoteApprovalLogResponse withdrawLog = findLast(cycle, "ROLLBACK");

    boolean cyclePending = pending && withdrawLog == null && rejectLog == null;

    steps.add(
        new QuoteApprovalWorkflowStepResponse(
            "draft",
            "创建草稿",
            "finish",
            quote.createdByName(),
            quote.createdAt(),
            null));

    if (submitLog != null) {
      String submitComment = submitLog.comment();
      if ((submitComment == null || submitComment.isBlank())
          && quote.changeReason() != null
          && !quote.changeReason().isBlank()
          && quote.revisionNo() != null
          && quote.revisionNo() > 0) {
        submitComment = "变更原因：" + quote.changeReason().trim();
      }
      steps.add(
          new QuoteApprovalWorkflowStepResponse(
              "submit",
              "提交审批",
              "finish",
              submitLog.operatorName(),
              submitLog.createdAt(),
              submitComment));
    } else if (quote.submittedAt() != null) {
      String submitComment = null;
      if (quote.changeReason() != null
          && !quote.changeReason().isBlank()
          && quote.revisionNo() != null
          && quote.revisionNo() > 0) {
        submitComment = "变更原因：" + quote.changeReason().trim();
      }
      steps.add(
          new QuoteApprovalWorkflowStepResponse(
              "submit",
              "提交审批",
              "finish",
              quote.createdByName(),
              quote.submittedAt(),
              submitComment));
    } else {
      steps.add(
          new QuoteApprovalWorkflowStepResponse(
              "submit", "提交审批", pending ? "process" : "wait", null, null, null));
    }

    List<ApprovalConfigResponse.FlowStep> nodes =
        flow.isEmpty()
            ? List.of(new ApprovalConfigResponse.FlowStep(null, null))
            : flow;
    int currentIndex = cycleApproves.size();
    boolean rejected = rejectLog != null;
    boolean withdrawn = withdrawLog != null;
    QuoteApprovalLogResponse stopLog = withdrawn ? withdrawLog : rejectLog;
    String stopTitle = withdrawn ? "已撤回" : "已驳回";

    for (int i = 0; i < nodes.size(); i++) {
      ApprovalConfigResponse.FlowStep node = nodes.get(i);
      String key = "node-" + (i + 1);
      if (i < cycleApproves.size()) {
        QuoteApprovalLogResponse approveLog = cycleApproves.get(i);
        steps.add(
            new QuoteApprovalWorkflowStepResponse(
                key,
                "已通过",
                "finish",
                approveLog.operatorName(),
                approveLog.createdAt(),
                approveLog.comment()));
        continue;
      }
      if ((rejected || withdrawn) && i == currentIndex) {
        steps.add(
            new QuoteApprovalWorkflowStepResponse(
                key,
                stopTitle,
                "error",
                stopLog.operatorName(),
                stopLog.createdAt(),
                stopLog.comment()));
        continue;
      }
      if (cyclePending && i == currentIndex) {
        steps.add(
            new QuoteApprovalWorkflowStepResponse(
                key, "待审批", "process", node.approverName(), null, null));
        continue;
      }
      steps.add(
          new QuoteApprovalWorkflowStepResponse(
              key, "下一节点", "wait", node.approverName(), null, null));
    }

    return steps;
  }

  private List<List<QuoteApprovalLogResponse>> splitCycles(List<QuoteApprovalLogResponse> logs) {
    List<List<QuoteApprovalLogResponse>> cycles = new ArrayList<>();
    int from = -1;
    for (int i = 0; i < logs.size(); i++) {
      if ("SUBMIT".equals(logs.get(i).action())) {
        if (from >= 0) {
          cycles.add(List.copyOf(logs.subList(from, i)));
        }
        from = i;
      }
    }
    if (from >= 0) {
      cycles.add(List.copyOf(logs.subList(from, logs.size())));
    }
    return cycles;
  }

  private List<QuoteApprovalLogResponse> currentCycle(List<QuoteApprovalLogResponse> logs) {
    int from = -1;
    for (int i = 0; i < logs.size(); i++) {
      if ("SUBMIT".equals(logs.get(i).action())) {
        from = i;
      }
    }
    if (from < 0) {
      return List.of();
    }
    return logs.subList(from, logs.size());
  }

  private List<QuoteApprovalLogResponse> cycleActions(
      List<QuoteApprovalLogResponse> cycle, String action) {
    return cycle.stream().filter(log -> action.equals(log.action())).toList();
  }

  private QuoteApprovalLogResponse findFirst(List<QuoteApprovalLogResponse> logs, String action) {
    for (QuoteApprovalLogResponse log : logs) {
      if (action.equals(log.action())) {
        return log;
      }
    }
    return null;
  }

  private QuoteApprovalLogResponse findLast(List<QuoteApprovalLogResponse> logs, String action) {
    QuoteApprovalLogResponse last = null;
    for (QuoteApprovalLogResponse log : logs) {
      if (action.equals(log.action())) {
        last = log;
      }
    }
    return last;
  }
}
