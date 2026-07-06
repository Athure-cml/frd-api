package com.furuiduo.quote.sys.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.sys.entity.OperationAction;

@Component
public class OperationLogSupport {

  private static final Pattern SENSITIVE_KEY =
      Pattern.compile("password|passwd|secret|token", Pattern.CASE_INSENSITIVE);
  private static final int MAX_BODY_LENGTH = 4000;
  private static final int MAX_SUMMARY_LENGTH = 256;

  private final ObjectMapper objectMapper;

  public OperationLogSupport(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public OperationAction resolveAction(String httpMethod, String requestUri) {
    String path = normalizePath(requestUri);
    if (path.endsWith("/set-default") || path.endsWith("/import")) {
      return OperationAction.UPDATE;
    }
    if (path.endsWith("/batch-delete")) {
      return OperationAction.DELETE;
    }
    if ("POST".equalsIgnoreCase(httpMethod) && "/cost-library/templates/export".equals(path)) {
      return OperationAction.UPDATE;
    }
    return switch (httpMethod.toUpperCase()) {
      case "POST" -> OperationAction.CREATE;
      case "PUT", "PATCH" -> OperationAction.UPDATE;
      case "DELETE" -> OperationAction.DELETE;
      default -> OperationAction.UPDATE;
    };
  }

  public String resolveModule(String requestUri) {
    if (requestUri == null || requestUri.isBlank()) {
      return "unknown";
    }
    String path = requestUri.split("\\?")[0];
    if (path.startsWith("/sys/departments")) {
      return "sys:dept";
    }
    if (path.startsWith("/sys/users")) {
      return "sys:user";
    }
    if (path.startsWith("/sys/roles")) {
      return "sys:role";
    }
    if (path.startsWith("/customers")) {
      return "customer";
    }
    if (path.startsWith("/quotes")) {
      return "quote";
    }
    if (path.startsWith("/cost-library/road")) {
      return "cost:road";
    }
    if (path.startsWith("/cost-library/sea")) {
      return "cost:sea";
    }
    if (path.startsWith("/cost-library/fumigation")) {
      return "cost:fumigation";
    }
    if (path.startsWith("/cost-library/rail")) {
      return "cost:rail";
    }
    if (path.startsWith("/cost-library/templates")) {
      return "cost:template";
    }
    if (path.startsWith("/currencies")) {
      return "currency";
    }
    if (path.startsWith("/exchange-rates")) {
      return "exchange_rate";
    }
    return "unknown";
  }

  public String resolveResourceType(String controllerName) {
    if (controllerName == null || controllerName.isBlank()) {
      return null;
    }
    String simple = controllerName;
    if (simple.endsWith("Controller")) {
      simple = simple.substring(0, simple.length() - "Controller".length());
    }
    return simple;
  }

  public String resolveResourceId(String requestUri) {
    if (requestUri == null) {
      return null;
    }
    String path = requestUri.split("\\?")[0];
    if (path.startsWith("/quotes/")) {
      String quoteId = extractSegmentAfter(path, "quotes");
      if (quoteId != null) {
        return quoteId;
      }
    }
    String[] segments = path.split("/");
    for (int i = segments.length - 1; i >= 0; i--) {
      String segment = segments[i];
      if (!segment.isBlank() && segment.matches("\\d+")) {
        return segment;
      }
    }
    return null;
  }

  public String buildSummary(
      OperationAction action,
      String module,
      String resourceId,
      Object requestBody,
      Object result,
      String requestUri) {
    JsonNode bodyNode = toNode(requestBody);
    JsonNode resultNode = unwrapApiResponse(result);
    String path = normalizePath(requestUri);
    if (path.endsWith("/set-default") && "cost:template".equals(module)) {
      return buildTemplateApplySummary(resourceId, resultNode);
    }
    if ("quote".equals(module) && path.contains("/follow-ups")) {
      return buildQuoteFollowUpSummary(action, bodyNode);
    }
    if (path.endsWith("/batch-delete") && module.startsWith("cost:")) {
      return "批量删除" + moduleLabel(module);
    }
    if ("/cost-library/templates/export".equals(path)) {
      return buildTemplateExportPreviewSummary(bodyNode, resultNode);
    }
    String actionLabel =
        switch (action) {
          case CREATE -> "新建";
          case UPDATE -> "更新";
          case DELETE -> "删除";
        };
    String entityLabel = moduleLabel(module);
    StringBuilder summary = new StringBuilder(actionLabel).append(entityLabel);

    if ("cost:template".equals(module)) {
      if (action == OperationAction.DELETE) {
        return buildTemplateDeleteSummary(resourceId, resultNode);
      }
      appendNamedSummary(summary, resourceId, bodyNode, resultNode, "name", "code");
      return truncate(summary.toString(), MAX_SUMMARY_LENGTH);
    }
    if ("quote".equals(module)) {
      appendQuoteSummary(summary, resourceId, bodyNode, resultNode);
      return truncate(summary.toString(), MAX_SUMMARY_LENGTH);
    }
    if ("customer".equals(module)) {
      appendNamedSummary(summary, resourceId, bodyNode, resultNode, "name", "code");
      return truncate(summary.toString(), MAX_SUMMARY_LENGTH);
    }
    if ("sys:dept".equals(module)) {
      appendNamedSummary(summary, resourceId, bodyNode, resultNode, "name", "code");
      return truncate(summary.toString(), MAX_SUMMARY_LENGTH);
    }
    if ("sys:user".equals(module)) {
      appendNamedSummary(summary, resourceId, bodyNode, resultNode, "realName", "username");
      return truncate(summary.toString(), MAX_SUMMARY_LENGTH);
    }
    if ("sys:role".equals(module)) {
      appendNamedSummary(summary, resourceId, bodyNode, resultNode, "name", "code");
      return truncate(summary.toString(), MAX_SUMMARY_LENGTH);
    }
    if ("currency".equals(module)) {
      appendNamedSummary(summary, resourceId, bodyNode, resultNode, "name", "code");
      return truncate(summary.toString(), MAX_SUMMARY_LENGTH);
    }
    if ("exchange_rate".equals(module)) {
      appendExchangeRateSummary(summary, bodyNode, resultNode);
      return truncate(summary.toString(), MAX_SUMMARY_LENGTH);
    }
    if (module.startsWith("cost:")) {
      appendCostSummary(summary, resourceId, bodyNode, resultNode);
      return truncate(summary.toString(), MAX_SUMMARY_LENGTH);
    }

    appendResourceId(summary, resourceId);
    return truncate(summary.toString(), MAX_SUMMARY_LENGTH);
  }

  public String serializeBody(Object body) {
    if (body == null) {
      return null;
    }
    try {
      JsonNode node = objectMapper.valueToTree(body);
      maskSensitive(node);
      String json = objectMapper.writeValueAsString(node);
      if (json.length() <= MAX_BODY_LENGTH) {
        return json;
      }
      return json.substring(0, MAX_BODY_LENGTH) + "…";
    } catch (JsonProcessingException ex) {
      String text = String.valueOf(body);
      if (text.length() <= MAX_BODY_LENGTH) {
        return text;
      }
      return text.substring(0, MAX_BODY_LENGTH) + "…";
    }
  }

  private String buildQuoteFollowUpSummary(OperationAction action, JsonNode bodyNode) {
    String actionLabel =
        switch (action) {
          case CREATE -> "新增";
          case UPDATE -> "更新";
          case DELETE -> "删除";
        };
    StringBuilder summary = new StringBuilder(actionLabel).append("跟进记录");
    String content = text(bodyNode, "content");
    if (content != null) {
      summary.append('：').append(truncate(content, 80));
    }
    return truncate(summary.toString(), MAX_SUMMARY_LENGTH);
  }

  private String extractSegmentAfter(String path, String marker) {
    String[] segments = path.split("/");
    for (int i = 0; i < segments.length - 1; i++) {
      if (marker.equals(segments[i])) {
        String next = segments[i + 1];
        if (next != null && !next.isBlank() && next.matches("\\d+")) {
          return next;
        }
      }
    }
    return null;
  }

  private void appendQuoteSummary(
      StringBuilder summary, String resourceId, JsonNode bodyNode, JsonNode resultNode) {
    String quoteNo = firstNonBlank(text(resultNode, "quoteNo"), text(bodyNode, "quoteNo"));
    if (quoteNo != null) {
      summary.append(' ').append(quoteNo);
    } else {
      appendResourceId(summary, resourceId);
    }

    List<String> details = new ArrayList<>();
    String customer =
        firstNonBlank(text(bodyNode, "customerName"), text(resultNode, "customerName"));
    if (customer != null) {
      details.add("客户「" + customer + "」");
    }
    String route =
        firstNonBlank(text(bodyNode, "routeSummary"), text(resultNode, "routeSummary"));
    if (route != null) {
      details.add("线路「" + route + "」");
    }
    int lineCount = arraySize(bodyNode, "lines");
    if (lineCount <= 0) {
      lineCount = arraySize(resultNode, "lines");
    }
    if (lineCount > 0) {
      details.add(lineCount + " 条明细");
    }
    if (!details.isEmpty()) {
      summary.append('：').append(String.join("，", details));
    }
  }

  private void appendNamedSummary(
      StringBuilder summary,
      String resourceId,
      JsonNode bodyNode,
      JsonNode resultNode,
      String primaryField,
      String secondaryField) {
    String name =
        firstNonBlank(
            text(bodyNode, primaryField),
            text(resultNode, primaryField),
            text(bodyNode, secondaryField),
            text(resultNode, secondaryField));
    if (name != null) {
      summary.append('「').append(name).append('』');
      return;
    }
    appendResourceId(summary, resourceId);
  }

  private void appendExchangeRateSummary(
      StringBuilder summary, JsonNode bodyNode, JsonNode resultNode) {
    String from = firstNonBlank(text(bodyNode, "fromCurrency"), text(resultNode, "fromCurrency"));
    String to = firstNonBlank(text(bodyNode, "toCurrency"), text(resultNode, "toCurrency"));
    if (from != null && to != null) {
      summary.append('：').append(from).append(" → ").append(to);
    }
  }

  private void appendCostSummary(
      StringBuilder summary, String resourceId, JsonNode bodyNode, JsonNode resultNode) {
    String label =
        firstNonBlank(
            text(bodyNode, "routeName"),
            text(resultNode, "routeName"),
            text(bodyNode, "destination"),
            text(resultNode, "destination"),
            text(bodyNode, "city"),
            text(resultNode, "city"),
            text(bodyNode, "name"),
            text(resultNode, "name"));
    if (label != null) {
      summary.append('「').append(label).append('』');
      return;
    }
    appendResourceId(summary, resourceId);
  }

  private String buildTemplateDeleteSummary(String resourceId, JsonNode resultNode) {
    StringBuilder summary = new StringBuilder("删除表格模板");
    String name = firstNonBlank(text(resultNode, "name"), text(resultNode, "code"));
    if (name != null) {
      summary.append('「').append(name).append('』');
    } else {
      appendResourceId(summary, resourceId);
    }
    return truncate(summary.toString(), MAX_SUMMARY_LENGTH);
  }

  private String buildTemplateApplySummary(String resourceId, JsonNode resultNode) {
    StringBuilder summary = new StringBuilder("应用表格模板");
    String name = text(resultNode, "name");
    if (name != null) {
      summary.append('「').append(name).append('』');
    } else {
      appendResourceId(summary, resourceId);
    }
    return truncate(summary.toString(), MAX_SUMMARY_LENGTH);
  }

  private String buildTemplateExportPreviewSummary(JsonNode bodyNode, JsonNode resultNode) {
    String name =
        firstNonBlank(text(bodyNode, "name"), text(resultNode, "name"), text(bodyNode, "code"));
    if (name != null) {
      return truncate("导出表格模板预览「" + name + "」", MAX_SUMMARY_LENGTH);
    }
    return "导出表格模板预览";
  }

  private String normalizePath(String requestUri) {
    if (requestUri == null || requestUri.isBlank()) {
      return "";
    }
    return requestUri.split("\\?")[0];
  }

  private void appendResourceId(StringBuilder summary, String resourceId) {
    if (resourceId != null && !resourceId.isBlank()) {
      summary.append("（ID ").append(resourceId).append('）');
    }
  }

  private String moduleLabel(String module) {
    return switch (module) {
      case "sys:dept" -> "部门";
      case "sys:user" -> "用户";
      case "sys:role" -> "角色";
      case "customer" -> "客户";
      case "quote" -> "报价单";
      case "cost:road" -> "卡车成本";
      case "cost:sea" -> "海运成本";
      case "cost:fumigation" -> "熏蒸成本";
      case "cost:rail" -> "铁路成本";
      case "cost:template" -> "表格模板";
      case "currency" -> "币种";
      case "exchange_rate" -> "汇率";
      default -> "数据";
    };
  }

  private JsonNode toNode(Object value) {
    if (value == null) {
      return null;
    }
    return objectMapper.valueToTree(value);
  }

  private JsonNode unwrapApiResponse(Object result) {
    if (result instanceof ApiResponse<?> response) {
      if (response.data() == null) {
        return null;
      }
      return objectMapper.valueToTree(response.data());
    }
    return toNode(result);
  }

  private String text(JsonNode node, String field) {
    if (node == null || !node.has(field) || node.get(field).isNull()) {
      return null;
    }
    String value = node.get(field).asText();
    return value.isBlank() ? null : value;
  }

  private int arraySize(JsonNode node, String field) {
    if (node == null || !node.has(field) || !node.get(field).isArray()) {
      return 0;
    }
    return node.get(field).size();
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private String truncate(String value, int max) {
    if (value == null || value.length() <= max) {
      return value;
    }
    return value.substring(0, max) + "…";
  }

  private void maskSensitive(JsonNode node) {
    if (node == null) {
      return;
    }
    if (node.isObject()) {
      ObjectNode objectNode = (ObjectNode) node;
      Map<String, JsonNode> fields = new LinkedHashMap<>();
      objectNode.fields().forEachRemaining(entry -> fields.put(entry.getKey(), entry.getValue()));
      for (Map.Entry<String, JsonNode> entry : fields.entrySet()) {
        if (SENSITIVE_KEY.matcher(entry.getKey()).find()) {
          objectNode.put(entry.getKey(), "***");
        } else {
          maskSensitive(entry.getValue());
        }
      }
      return;
    }
    if (node.isArray()) {
      node.forEach(this::maskSensitive);
    }
  }
}
