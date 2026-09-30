package com.furuiduo.quote.quote.support;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.furuiduo.quote.quote.entity.QuoteApprovalAction;
import com.furuiduo.quote.quote.entity.QuoteApprovalLog;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.entity.QuoteStatus;

public final class QuoteApprovalSupport {

  public static final String TYPE_QUOTE = "QUOTE";
  public static final String TYPE_QUOTE_REVISION = "QUOTE_REVISION";

  public static String resolveApprovalType(QuoteOrder order) {
    if (order != null
        && order.getRevisionNo() != null
        && order.getRevisionNo() > 0) {
      return TYPE_QUOTE_REVISION;
    }
    return TYPE_QUOTE;
  }
  public static final String STATUS_PENDING = "PENDING";
  public static final String STATUS_APPROVED = "APPROVED";
  public static final String STATUS_REJECTED = "REJECTED";
  public static final String STATUS_WITHDRAWN = "WITHDRAWN";

  private static final ObjectMapper FLOW_JSON = new ObjectMapper();

  private QuoteApprovalSupport() {}

  public static String buildApprovalNo(QuoteOrder order) {
    return buildApprovalNo(order, 1);
  }

  public static String buildApprovalNo(QuoteOrder order, int cycleIndex) {
    String base = "AP-" + order.getQuoteNo();
    if (cycleIndex <= 1) {
      return base;
    }
    return base + "-" + String.format("%02d", cycleIndex);
  }

  public record FlowNode(Long approverId, String approverName) {}

  /** 本轮提交时的流程优先；没有快照的历史已完成轮次按实际同意记录还原，避免被当前配置改写。 */
  public static List<FlowNode> resolveCycleFlow(List<QuoteApprovalLog> cycle, List<FlowNode> current) {
    List<FlowNode> snapshot = parseFlowSnapshot(findSubmitSnapshot(cycle));
    if (!snapshot.isEmpty()) {
      return snapshot;
    }
    List<FlowNode> reconstructed = reconstructFlowFromApproves(cycle);
    if (isCycleFinished(cycle)) {
      if (hasAction(cycle, QuoteApprovalAction.REJECT)
          || hasAction(cycle, QuoteApprovalAction.ROLLBACK)) {
        List<FlowNode> nodes = new ArrayList<>(reconstructed);
        QuoteApprovalLog stop = lastAction(cycle, QuoteApprovalAction.ROLLBACK);
        if (stop == null) {
          stop = lastAction(cycle, QuoteApprovalAction.REJECT);
        }
        if (stop != null) {
          nodes.add(new FlowNode(stop.getOperatorId(), stop.getOperatorName()));
        }
        if (!nodes.isEmpty()) {
          return nodes;
        }
      } else if (!reconstructed.isEmpty()) {
        return reconstructed;
      }
    }
    int currentSize = current == null ? 0 : current.size();
    if (reconstructed.size() > currentSize) {
      return reconstructed;
    }
    if (current == null || current.isEmpty()) {
      return reconstructed;
    }
    return current;
  }

  public static String serializeFlow(List<FlowNode> flow) {
    ArrayNode array = FLOW_JSON.createArrayNode();
    if (flow != null) {
      for (FlowNode node : flow) {
        ObjectNode item = FLOW_JSON.createObjectNode();
        if (node.approverId() == null) {
          item.putNull("approverId");
        } else {
          item.put("approverId", node.approverId());
        }
        item.put("approverName", node.approverName() == null ? "" : node.approverName());
        array.add(item);
      }
    }
    return array.toString();
  }

  public static List<FlowNode> parseFlowSnapshot(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      JsonNode root = FLOW_JSON.readTree(json);
      if (root == null || !root.isArray()) {
        return List.of();
      }
      List<FlowNode> nodes = new ArrayList<>();
      for (JsonNode item : root) {
        Long approverId =
            item.hasNonNull("approverId") ? item.get("approverId").asLong() : null;
        String approverName = item.path("approverName").asText("");
        nodes.add(new FlowNode(approverId, approverName));
      }
      return List.copyOf(nodes);
    } catch (Exception ex) {
      return List.of();
    }
  }

  private static String findSubmitSnapshot(List<QuoteApprovalLog> cycle) {
    if (cycle == null) {
      return null;
    }
    for (QuoteApprovalLog log : cycle) {
      if (log.getAction() == QuoteApprovalAction.SUBMIT) {
        return log.getFlowSnapshot();
      }
    }
    return null;
  }

  private static List<FlowNode> reconstructFlowFromApproves(List<QuoteApprovalLog> cycle) {
    List<FlowNode> nodes = new ArrayList<>();
    if (cycle == null) {
      return nodes;
    }
    for (QuoteApprovalLog log : cycle) {
      if (log.getAction() == QuoteApprovalAction.APPROVE) {
        nodes.add(new FlowNode(log.getOperatorId(), log.getOperatorName()));
      }
    }
    return nodes;
  }

  public static boolean isCycleFinished(List<QuoteApprovalLog> cycle) {
    if (hasAction(cycle, QuoteApprovalAction.ROLLBACK)
        || hasAction(cycle, QuoteApprovalAction.REJECT)) {
      return true;
    }
    for (QuoteApprovalLog log : cycle) {
      if (log.getAction() == QuoteApprovalAction.APPROVE
          && QuoteStatus.SENT.name().equals(log.getToStatus())) {
        return true;
      }
    }
    return false;
  }

  public static ResolvedApproval resolve(QuoteOrder order, List<QuoteApprovalLog> logs) {
    return resolve(order, logs, List.of());
  }

  public static ResolvedApproval resolve(
      QuoteOrder order, List<QuoteApprovalLog> logs, List<FlowNode> flow) {
    QuoteStatus normalized = QuoteStatusSupport.normalize(order.getStatus());
    if (normalized == QuoteStatus.PENDING_APPROVAL) {
      FlowNode node = currentFlowNode(logs, flow);
      String label =
          node != null && node.approverName() != null && !node.approverName().isBlank()
              ? node.approverName()
              : "待审批";
      return new ResolvedApproval(STATUS_PENDING, label);
    }
    List<QuoteApprovalLog> cycle = currentCycle(logs);
    if (hasAction(cycle, QuoteApprovalAction.ROLLBACK)) {
      return new ResolvedApproval(STATUS_WITHDRAWN, "已撤回");
    }
    if (hasAction(cycle, QuoteApprovalAction.REJECT)) {
      return new ResolvedApproval(STATUS_REJECTED, "已驳回");
    }
    if (order.getApprovedAt() != null) {
      return new ResolvedApproval(STATUS_APPROVED, "已通过");
    }
    if (hasAction(logs, QuoteApprovalAction.ROLLBACK)) {
      return new ResolvedApproval(STATUS_WITHDRAWN, "已撤回");
    }
    if (hasAction(logs, QuoteApprovalAction.REJECT)) {
      return new ResolvedApproval(STATUS_REJECTED, "已驳回");
    }
    return new ResolvedApproval(STATUS_PENDING, "待审批");
  }

  public static ResolvedApproval resolveCycle(
      QuoteOrder order, List<QuoteApprovalLog> cycle, List<FlowNode> flow, boolean latest) {
    if (hasAction(cycle, QuoteApprovalAction.ROLLBACK)) {
      return new ResolvedApproval(STATUS_WITHDRAWN, "已撤回");
    }
    if (hasAction(cycle, QuoteApprovalAction.REJECT)) {
      return new ResolvedApproval(STATUS_REJECTED, "已驳回");
    }
    if (isCycleApproved(order, cycle, flow, latest)) {
      return new ResolvedApproval(STATUS_APPROVED, "已通过");
    }
    if (latest && QuoteStatusSupport.normalize(order.getStatus()) == QuoteStatus.PENDING_APPROVAL) {
      FlowNode node = flowNodeAt(cycle, flow);
      String label =
          node != null && node.approverName() != null && !node.approverName().isBlank()
              ? node.approverName()
              : "审批中";
      return new ResolvedApproval(STATUS_PENDING, label);
    }
    return new ResolvedApproval(STATUS_PENDING, "审批中");
  }

  private static boolean isCycleApproved(
      QuoteOrder order, List<QuoteApprovalLog> cycle, List<FlowNode> flow, boolean latest) {
    for (QuoteApprovalLog log : cycle) {
      if (log.getAction() == QuoteApprovalAction.APPROVE
          && QuoteStatus.SENT.name().equals(log.getToStatus())) {
        return true;
      }
    }
    if (flow != null && !flow.isEmpty() && countCycleApproves(cycle) >= flow.size()) {
      return true;
    }
    return latest
        && order.getApprovedAt() != null
        && !hasAction(cycle, QuoteApprovalAction.REJECT)
        && !hasAction(cycle, QuoteApprovalAction.ROLLBACK);
  }

  public static FlowNode currentFlowNode(List<QuoteApprovalLog> logs, List<FlowNode> flow) {
    return flowNodeAt(currentCycle(logs), flow);
  }

  public static FlowNode flowNodeAt(List<QuoteApprovalLog> cycle, List<FlowNode> flow) {
    if (flow == null || flow.isEmpty()) {
      return null;
    }
    int index = countCycleApproves(cycle);
    if (index < 0 || index >= flow.size()) {
      return null;
    }
    return flow.get(index);
  }

  public static boolean isCurrentApprover(
      Long userId, List<QuoteApprovalLog> logs, List<FlowNode> flow) {
    if (userId == null) {
      return false;
    }
    if (flow == null || flow.isEmpty()) {
      return true;
    }
    FlowNode node = currentFlowNode(logs, flow);
    return node != null && userId.equals(node.approverId());
  }

  public static List<QuoteApprovalLog> currentCycle(List<QuoteApprovalLog> logs) {
    List<List<QuoteApprovalLog>> cycles = splitCycles(logs);
    if (cycles.isEmpty()) {
      return List.of();
    }
    return cycles.get(cycles.size() - 1);
  }

  public static List<List<QuoteApprovalLog>> splitCycles(List<QuoteApprovalLog> logs) {
    List<List<QuoteApprovalLog>> cycles = new ArrayList<>();
    if (logs == null || logs.isEmpty()) {
      return cycles;
    }
    int from = -1;
    for (int i = 0; i < logs.size(); i++) {
      if (logs.get(i).getAction() == QuoteApprovalAction.SUBMIT) {
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

  public static int countCycleApproves(List<QuoteApprovalLog> cycle) {
    int count = 0;
    for (QuoteApprovalLog log : cycle) {
      if (log.getAction() == QuoteApprovalAction.APPROVE) {
        count++;
      }
    }
    return count;
  }

  public static String resolveInitiatorName(QuoteOrder order, List<QuoteApprovalLog> logs) {
    for (QuoteApprovalLog log : logs) {
      if (log.getAction() == QuoteApprovalAction.SUBMIT) {
        return log.getOperatorName();
      }
    }
    return order.getCreatedByName();
  }

  public static boolean hasOperatorAction(
      List<QuoteApprovalLog> logs, QuoteApprovalAction action, Long userId) {
    if (userId == null) {
      return false;
    }
    for (QuoteApprovalLog log : logs) {
      if (log.getAction() == action && userId.equals(log.getOperatorId())) {
        return true;
      }
    }
    return false;
  }

  public static String inboxStatus(
      QuoteOrder order, List<QuoteApprovalLog> logs, List<FlowNode> flow, Long userId) {
    QuoteStatus normalized = QuoteStatusSupport.normalize(order.getStatus());
    if (normalized == QuoteStatus.PENDING_APPROVAL && isCurrentApprover(userId, logs, flow)) {
      return STATUS_PENDING;
    }
    return resolve(order, logs, flow).status();
  }

  public static LocalDateTime resolveCompletedAt(
      QuoteOrder order, List<QuoteApprovalLog> logs) {
    return cycleCompletedAt(currentCycle(logs), order, true);
  }

  public static LocalDateTime cycleSubmitAt(List<QuoteApprovalLog> cycle) {
    for (QuoteApprovalLog log : cycle) {
      if (log.getAction() == QuoteApprovalAction.SUBMIT) {
        return log.getCreatedAt();
      }
    }
    return null;
  }

  public static LocalDateTime cycleCompletedAt(
      List<QuoteApprovalLog> cycle, QuoteOrder order, boolean latest) {
    QuoteApprovalLog lastStop = null;
    QuoteApprovalLog lastApprove = null;
    for (QuoteApprovalLog log : cycle) {
      if (log.getAction() == QuoteApprovalAction.REJECT
          || log.getAction() == QuoteApprovalAction.ROLLBACK) {
        lastStop = log;
      }
      if (log.getAction() == QuoteApprovalAction.APPROVE) {
        lastApprove = log;
      }
    }
    if (lastStop != null) {
      return lastStop.getCreatedAt();
    }
    if (lastApprove != null && QuoteStatus.SENT.name().equals(lastApprove.getToStatus())) {
      return lastApprove.getCreatedAt();
    }
    if (latest && order.getApprovedAt() != null) {
      return order.getApprovedAt();
    }
    return lastApprove != null && isCycleApproved(order, cycle, List.of(), latest)
        ? lastApprove.getCreatedAt()
        : null;
  }

  public static String formatDuration(QuoteOrder order, List<QuoteApprovalLog> logs) {
    List<QuoteApprovalLog> cycle = currentCycle(logs);
    return formatDuration(cycleSubmitAt(cycle), cycleCompletedAt(cycle, order, true));
  }

  public static String formatDuration(LocalDateTime start, LocalDateTime end) {
    if (start == null) {
      return "";
    }
    LocalDateTime stop = end == null ? LocalDateTime.now() : end;
    Duration duration = Duration.between(start, stop);
    if (duration.isNegative()) {
      duration = Duration.ZERO;
    }
    long totalSeconds = duration.getSeconds();
    long totalMinutes = totalSeconds / 60;
    if (totalMinutes < 60) {
      return totalMinutes + "分钟";
    }
    long totalHours = totalMinutes / 60;
    if (totalHours < 24) {
      long minutes = totalMinutes % 60;
      if (minutes == 0) {
        return totalHours + "小时";
      }
      return totalHours + "小时" + minutes + "分钟";
    }
    long days = totalHours / 24;
    long hours = totalHours % 24;
    if (hours == 0) {
      return days + "天";
    }
    return days + "天" + hours + "小时";
  }

  public static String nodeTitle(QuoteApprovalAction action, List<FlowNode> flow, int nodeIndex) {
    if (action == QuoteApprovalAction.SUBMIT) {
      return "提交审批";
    }
    if (action == QuoteApprovalAction.ROLLBACK) {
      return "撤回审批";
    }
    if (flow != null && nodeIndex >= 0 && nodeIndex < flow.size()) {
      FlowNode node = flow.get(nodeIndex);
      if (node != null && node.approverName() != null && !node.approverName().isBlank()) {
        return node.approverName() + "审批";
      }
    }
    return "节点" + (nodeIndex + 1);
  }

  public static String resultLabel(QuoteApprovalAction action) {
    return switch (action) {
      case SUBMIT -> "提交";
      case APPROVE -> "同意";
      case REJECT -> "驳回";
      case ROLLBACK -> "撤回";
    };
  }

  private static QuoteApprovalLog lastAction(
      List<QuoteApprovalLog> logs, QuoteApprovalAction action) {
    QuoteApprovalLog last = null;
    if (logs == null) {
      return null;
    }
    for (QuoteApprovalLog log : logs) {
      if (log.getAction() == action) {
        last = log;
      }
    }
    return last;
  }

  private static boolean hasAction(List<QuoteApprovalLog> logs, QuoteApprovalAction action) {
    if (logs == null) {
      return false;
    }
    for (QuoteApprovalLog log : logs) {
      if (log.getAction() == action) {
        return true;
      }
    }
    return false;
  }

  public record ResolvedApproval(String status, String currentNode) {}
}
