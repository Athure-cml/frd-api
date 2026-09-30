package com.furuiduo.quote.quote.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.quote.dto.QuoteApprovalHistoryItem;
import com.furuiduo.quote.quote.dto.QuoteApprovalLogResponse;
import com.furuiduo.quote.quote.entity.QuoteApprovalAction;
import com.furuiduo.quote.quote.entity.QuoteApprovalLog;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.entity.QuoteStatus;
import com.furuiduo.quote.quote.repository.QuoteApprovalLogRepository;
import com.furuiduo.quote.quote.support.QuoteApprovalSupport;
import com.furuiduo.quote.quote.support.QuoteDateTimes;
import com.furuiduo.quote.quote.support.QuoteStatusSupport;
import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class QuoteApprovalLogService {

  private final QuoteApprovalLogRepository quoteApprovalLogRepository;

  public QuoteApprovalLogService(QuoteApprovalLogRepository quoteApprovalLogRepository) {
    this.quoteApprovalLogRepository = quoteApprovalLogRepository;
  }

  @Transactional
  public QuoteApprovalLog append(
      Long quoteOrderId,
      QuoteApprovalAction action,
      QuoteStatus fromStatus,
      QuoteStatus toStatus,
      String comment,
      SysUser operator) {
    return append(quoteOrderId, action, fromStatus, toStatus, comment, null, null, operator);
  }

  @Transactional
  public QuoteApprovalLog append(
      Long quoteOrderId,
      QuoteApprovalAction action,
      QuoteStatus fromStatus,
      QuoteStatus toStatus,
      String comment,
      String nodeTitle,
      SysUser operator) {
    return append(quoteOrderId, action, fromStatus, toStatus, comment, nodeTitle, null, operator);
  }

  @Transactional
  public QuoteApprovalLog append(
      Long quoteOrderId,
      QuoteApprovalAction action,
      QuoteStatus fromStatus,
      QuoteStatus toStatus,
      String comment,
      String nodeTitle,
      String flowSnapshot,
      SysUser operator) {
    return append(
        quoteOrderId, action, fromStatus, toStatus, comment, nodeTitle, flowSnapshot, null, operator);
  }

  @Transactional
  public QuoteApprovalLog append(
      Long quoteOrderId,
      QuoteApprovalAction action,
      QuoteStatus fromStatus,
      QuoteStatus toStatus,
      String comment,
      String nodeTitle,
      String flowSnapshot,
      String printSnapshot,
      SysUser operator) {
    QuoteApprovalLog log = new QuoteApprovalLog();
    log.setQuoteOrderId(quoteOrderId);
    log.setAction(action);
    log.setFromStatus(fromStatus != null ? QuoteStatusSupport.displayStatus(fromStatus) : null);
    log.setToStatus(toStatus != null ? QuoteStatusSupport.displayStatus(toStatus) : null);
    log.setComment(comment);
    log.setNodeTitle(nodeTitle);
    log.setFlowSnapshot(flowSnapshot);
    log.setPrintSnapshot(printSnapshot);
    log.setOperatorId(operator.getId());
    log.setOperatorName(operator.getRealName());
    log.setCreatedAt(LocalDateTime.now());
    return quoteApprovalLogRepository.save(log);
  }

  public List<QuoteApprovalLogResponse> listByQuoteOrderId(Long quoteOrderId) {
    return listEntities(quoteOrderId).stream().map(QuoteApprovalLogResponse::from).toList();
  }

  public List<QuoteApprovalHistoryItem> listHistory(
      Long quoteOrderId, List<QuoteApprovalSupport.FlowNode> currentFlow) {
    List<QuoteApprovalLog> logs = listEntities(quoteOrderId);
    List<QuoteApprovalHistoryItem> items = new ArrayList<>();
    for (List<QuoteApprovalLog> cycle : QuoteApprovalSupport.splitCycles(logs)) {
      List<QuoteApprovalSupport.FlowNode> flow =
          QuoteApprovalSupport.resolveCycleFlow(cycle, currentFlow);
      int nodeIndex = 0;
      for (QuoteApprovalLog log : cycle) {
        QuoteApprovalAction action = log.getAction();
        String nodeTitle = log.getNodeTitle();
        if (nodeTitle == null || nodeTitle.isBlank()) {
          nodeTitle = QuoteApprovalSupport.nodeTitle(action, flow, nodeIndex);
        }
        items.add(
            new QuoteApprovalHistoryItem(
                log.getId(),
                nodeTitle,
                log.getOperatorName(),
                QuoteDateTimes.formatSeconds(log.getCreatedAt()),
                QuoteApprovalSupport.resultLabel(action),
                log.getComment(),
                action != null ? action.name() : null));
        if (action == QuoteApprovalAction.APPROVE) {
          nodeIndex++;
        }
      }
    }
    return items;
  }

  public List<QuoteApprovalLog> listEntities(Long quoteOrderId) {
    return quoteApprovalLogRepository.findByQuoteOrderIdOrderByCreatedAtAscIdAsc(quoteOrderId);
  }

  public boolean hasLogs(Long quoteOrderId) {
    return quoteApprovalLogRepository.existsByQuoteOrderId(quoteOrderId);
  }

  /** 为缺少打印快照的 SUBMIT 记录补齐（兼容旧数据删除前落库） */
  @Transactional
  public void ensurePrintSnapshots(
      QuoteOrder order, List<com.furuiduo.quote.quote.dto.QuoteCostMatchItemDto> costSnapshots) {
    if (order == null || order.getId() == null) {
      return;
    }
    String snapshot =
        com.furuiduo.quote.quote.support.QuoteApprovalPrintSnapshotSupport.serialize(
            order, costSnapshots);
    List<QuoteApprovalLog> logs =
        quoteApprovalLogRepository.findByQuoteOrderIdOrderByCreatedAtAscIdAsc(order.getId());
    for (QuoteApprovalLog log : logs) {
      if (log.getAction() == QuoteApprovalAction.SUBMIT
          && (log.getPrintSnapshot() == null || log.getPrintSnapshot().isBlank())) {
        log.setPrintSnapshot(snapshot);
        quoteApprovalLogRepository.save(log);
      }
    }
  }

  public String findLastComment(Long quoteOrderId) {
    List<QuoteApprovalLog> logs =
        quoteApprovalLogRepository.findByQuoteOrderIdOrderByCreatedAtAscIdAsc(quoteOrderId);
    if (logs.isEmpty()) {
      return null;
    }
    return logs.get(logs.size() - 1).getComment();
  }
}
