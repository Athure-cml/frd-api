package com.furuiduo.quote.ai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.furuiduo.quote.ai.dto.AiChatMessage;
import com.furuiduo.quote.ai.dto.AiChatRequest;
import com.furuiduo.quote.ai.dto.AiChatResponse;
import com.furuiduo.quote.ai.dto.AiCitedCost;
import com.furuiduo.quote.ai.dto.AiOpenPage;
import com.furuiduo.quote.ai.dto.AiProposedCost;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

@Service
public class AiChatService {

  private static final Logger log = LoggerFactory.getLogger(AiChatService.class);
  private static final int MAX_MESSAGES = 40;
  private static final int MAX_CONTENT_LEN = 8000;

  private final AiClient aiClient;
  private final AiToolExecutor toolExecutor;
  private final PermissionService permissionService;
  private final ObjectMapper objectMapper;
  private final int maxToolRounds;
  private final String systemPrompt;

  public AiChatService(
      AiClient aiClient,
      AiToolExecutor toolExecutor,
      PermissionService permissionService,
      ObjectMapper objectMapper,
      @Value("${quote.ai.max-tool-rounds:3}") int maxToolRounds) {
    this.aiClient = aiClient;
    this.toolExecutor = toolExecutor;
    this.permissionService = permissionService;
    this.objectMapper = objectMapper;
    this.maxToolRounds = Math.max(maxToolRounds, 1);
    this.systemPrompt = loadSystemPrompt();
  }

  public AiChatResponse chat(SysUser user, AiChatRequest request) {
    aiClient.ensureConfigured();
    List<AiChatMessage> input = request == null ? List.of() : request.messages();
    if (input == null || input.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "messages 不能为空");
    }

    boolean enableTools = request.enableTools() == null || Boolean.TRUE.equals(request.enableTools());
    List<Map<String, Object>> allowedTools = enableTools ? toolDefinitionsFor(user) : List.of();
    List<Map<String, Object>> messages = new ArrayList<>();
    messages.add(Map.of("role", "system", "content", systemPrompt));
    appendUserMessages(messages, input);

    List<String> toolCallNames = new ArrayList<>();
    List<AiCitedCost> citedCosts = new ArrayList<>();
    List<AiProposedCost> proposedCosts = new ArrayList<>();
    List<AiOpenPage> openPages = new ArrayList<>();

    for (int round = 0; round < maxToolRounds; round++) {
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("model", aiClient.model());
      body.put("messages", messages);
      if (!allowedTools.isEmpty()) {
        body.put("tools", allowedTools);
        body.put("tool_choice", "auto");
      }

      JsonNode response = aiClient.chatCompletions(body);
      JsonNode choice = response.path("choices").path(0);
      if (choice.isMissingNode()) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 返回异常");
      }
      JsonNode message = choice.path("message");
      JsonNode toolCalls = message.path("tool_calls");
      if (allowedTools.isEmpty() || !toolCalls.isArray() || toolCalls.isEmpty()) {
        String reply = message.path("content").asText("");
        if (reply == null || reply.isBlank()) {
          if (!proposedCosts.isEmpty()) {
            reply = "已整理成本草稿，正在打开录入表单，请核对后保存。";
          } else if (!openPages.isEmpty()) {
            reply = "好的，正在为你打开「" + openPages.get(openPages.size() - 1).title() + "」。";
          } else {
            reply = "（模型未返回内容）";
          }
        }
        return new AiChatResponse(
            reply,
            List.copyOf(toolCallNames),
            List.copyOf(citedCosts),
            List.copyOf(proposedCosts),
            List.copyOf(openPages),
            aiClient.model());
      }

      Map<String, Object> assistantMsg = new LinkedHashMap<>();
      assistantMsg.put("role", "assistant");
      if (message.hasNonNull("content")) {
        assistantMsg.put("content", message.get("content").asText());
      } else {
        assistantMsg.put("content", "");
      }
      assistantMsg.put("tool_calls", objectMapper.convertValue(toolCalls, List.class));
      messages.add(assistantMsg);

      for (JsonNode call : toolCalls) {
        String name = call.path("function").path("name").asText("");
        String callId = call.path("id").asText("tool_" + toolCallNames.size());
        String argsRaw = call.path("function").path("arguments").asText("{}");
        JsonNode args;
        try {
          args = objectMapper.readTree(argsRaw == null || argsRaw.isBlank() ? "{}" : argsRaw);
        } catch (Exception ex) {
          args = objectMapper.createObjectNode();
        }
        toolCallNames.add(name);
        AiToolExecutor.ToolResult result = toolExecutor.execute(user, name, args);
        citedCosts.addAll(result.citedCosts());
        proposedCosts.addAll(result.proposedCosts());
        openPages.addAll(result.openPages());
        Map<String, Object> toolMsg = new LinkedHashMap<>();
        toolMsg.put("role", "tool");
        toolMsg.put("tool_call_id", callId);
        toolMsg.put("content", result.content());
        messages.add(toolMsg);
      }
    }

    // 最后一轮强制不再带 tools，拿到自然语言总结
    Map<String, Object> finalBody = new LinkedHashMap<>();
    finalBody.put("model", aiClient.model());
    finalBody.put("messages", messages);
    JsonNode finalResponse = aiClient.chatCompletions(finalBody);
    String reply = finalResponse.path("choices").path(0).path("message").path("content").asText("");
    if (reply == null || reply.isBlank()) {
      if (!proposedCosts.isEmpty()) {
        reply = "已整理成本草稿，正在打开录入表单，请核对后保存。";
      } else if (!openPages.isEmpty()) {
        reply = "好的，正在为你打开「" + openPages.get(openPages.size() - 1).title() + "」。";
      } else {
        reply = "已查询相关数据，但未能生成总结，请根据引用成本自行核对。";
      }
    }
    return new AiChatResponse(
        reply,
        List.copyOf(toolCallNames),
        List.copyOf(citedCosts),
        List.copyOf(proposedCosts),
        List.copyOf(openPages),
        aiClient.model());
  }

  private void appendUserMessages(List<Map<String, Object>> messages, List<AiChatMessage> input) {
    int from = Math.max(0, input.size() - MAX_MESSAGES);
    for (int i = from; i < input.size(); i++) {
      AiChatMessage msg = input.get(i);
      if (msg == null || msg.role() == null || msg.content() == null) {
        continue;
      }
      String role = msg.role().trim().toLowerCase();
      if (!role.equals("user") && !role.equals("assistant")) {
        continue;
      }
      String content = msg.content().trim();
      if (content.isEmpty()) {
        continue;
      }
      if (content.length() > MAX_CONTENT_LEN) {
        content = content.substring(0, MAX_CONTENT_LEN);
      }
      messages.add(Map.of("role", role, "content", content));
    }
    if (messages.size() <= 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "有效 messages 不能为空");
    }
  }

  /** 仅向模型暴露当前用户有权调用的工具，避免诱导越权查询。 */
  private List<Map<String, Object>> toolDefinitionsFor(SysUser user) {
    List<Map<String, Object>> tools = new ArrayList<>();
    List<String> allowedPages = allowedOpenPages(user);
    if (!allowedPages.isEmpty()) {
      String pageList = String.join(", ", allowedPages);
      tools.add(
          tool(
              "open_page",
              "打开系统页面（真正跳转）。用户说「打开/跳转/去某某页面/菜单」时必须调用。"
                  + "当前用户可打开的 page 取值："
                  + pageList
                  + "。不要打开列表外的页面，不要假装已跳转。",
              Map.of(
                  "type",
                  "object",
                  "properties",
                  Map.of(
                      "page",
                      Map.of(
                          "type",
                          "string",
                          "description",
                          "页面键（仅限：" + pageList + "）")),
                  "required",
                  List.of("page"))));
    }
    if (permissionService.hasPermission(user, PermissionCodes.COST_ROAD_VIEW)) {
      tools.add(
          tool(
              "search_road_costs",
              "按邮编/城市/州/POR/POL/供应商查询卡车成本库（只读）",
              Map.of(
                  "type",
                  "object",
                  "properties",
                  Map.of(
                      "zipCode", Map.of("type", "string"),
                      "city", Map.of("type", "string"),
                      "state", Map.of("type", "string"),
                      "por", Map.of("type", "string", "description", "接货地城市"),
                      "pol", Map.of("type", "string", "description", "装货港"),
                      "supplier", Map.of("type", "string")))));
    }
    if (permissionService.hasPermission(user, PermissionCodes.COST_SEA_VIEW)) {
      tools.add(
          tool(
              "search_sea_costs",
              "按 POR/POL/POD/船公司查询海运成本库（只读）",
              Map.of(
                  "type",
                  "object",
                  "properties",
                  Map.of(
                      "por", Map.of("type", "string"),
                      "pol", Map.of("type", "string"),
                      "pod", Map.of("type", "string"),
                      "ssl", Map.of("type", "string", "description", "船公司")))));
    }
    if (permissionService.hasPermission(user, PermissionCodes.COST_FUMIGATION_VIEW)) {
      tools.add(
          tool(
              "search_fumigation_costs",
              "按区域/站点查询熏蒸成本库（只读）",
              Map.of(
                  "type",
                  "object",
                  "properties",
                  Map.of(
                      "region", Map.of("type", "string"),
                      "station", Map.of("type", "string")))));
    }
    if (permissionService.hasPermission(user, PermissionCodes.QUOTE_VIEW)) {
      tools.add(
          tool(
              "get_quote_context",
              "按报价单 ID 获取当前用户有权查看的报价摘要（只读，不含完整业务表明细）",
              Map.of(
                  "type",
                  "object",
                  "properties",
                  Map.of("quoteId", Map.of("type", "integer")),
                  "required",
                  List.of("quoteId"))));
    }
    if (permissionService.hasPermission(user, PermissionCodes.COST_ROAD_EDIT)) {
      tools.add(
          tool(
              "propose_road_cost",
              "根据用户描述整理一条卡车成本字段草稿（不会写入数据库）。前端会打开成本库录入表单供用户核对后自行保存。每次只拟一条。",
              Map.of(
                  "type",
                  "object",
                  "properties",
                  Map.ofEntries(
                      Map.entry("zipCode", Map.of("type", "string")),
                      Map.entry("city", Map.of("type", "string")),
                      Map.entry("state", Map.of("type", "string")),
                      Map.entry("por", Map.of("type", "string", "description", "接货地")),
                      Map.entry("pol", Map.of("type", "string", "description", "装货港")),
                      Map.entry("supplier", Map.of("type", "string")),
                      Map.entry("baseFreight", Map.of("type", "number")),
                      Map.entry("fsc", Map.of("type", "number")),
                      Map.entry("chassis", Map.of("type", "number")),
                      Map.entry("triTandemAxle", Map.of("type", "number")),
                      Map.entry("split", Map.of("type", "number")),
                      Map.entry("stopOff", Map.of("type", "number")),
                      Map.entry("allInNoFm", Map.of("type", "number")),
                      Map.entry("allInFmOneWay", Map.of("type", "number", "description", "ALL IN FM NON OAK")),
                      Map.entry("allInFmRound", Map.of("type", "number", "description", "ALL IN FM OAK")),
                      Map.entry("waitingFee", Map.of("type", "number")),
                      Map.entry("redelivery", Map.of("type", "number")),
                      Map.entry("prepull", Map.of("type", "number")),
                      Map.entry("nsLift", Map.of("type", "number")),
                      Map.entry("otherFee", Map.of("type", "number")),
                      Map.entry("remark", Map.of("type", "string")),
                      Map.entry("validDate", Map.of("type", "string")),
                      Map.entry("logYardNameAddress", Map.of("type", "string"))),
                  "required",
                  List.of("zipCode", "city", "state", "por", "pol", "supplier"))));
    }
    if (permissionService.hasPermission(user, PermissionCodes.COST_SEA_EDIT)) {
      tools.add(
          tool(
              "propose_sea_cost",
              "根据用户描述整理一条海运成本字段草稿（不会写入数据库）。前端会打开海运成本库录入表单供用户核对后自行保存。每次只拟一条。",
              Map.of(
                  "type",
                  "object",
                  "properties",
                  Map.ofEntries(
                      Map.entry("por", Map.of("type", "string")),
                      Map.entry("pol", Map.of("type", "string")),
                      Map.entry("pod", Map.of("type", "string", "description", "目的港，多个可用 / 分隔")),
                      Map.entry("cnShortName", Map.of("type", "string")),
                      Map.entry("enProductName", Map.of("type", "string")),
                      Map.entry(
                          "containerType",
                          Map.of("type", "string", "description", "箱型，如 40HQ/40GP")),
                      Map.entry("freight", Map.of("type", "number")),
                      Map.entry("freightValidDate", Map.of("type", "string")),
                      Map.entry("buc", Map.of("type", "number")),
                      Map.entry("bucValidDate", Map.of("type", "string")),
                      Map.entry("ebs", Map.of("type", "number")),
                      Map.entry("ebsValidDate", Map.of("type", "string")),
                      Map.entry("gri", Map.of("type", "number")),
                      Map.entry("griValidDate", Map.of("type", "string")),
                      Map.entry("others", Map.of("type", "number")),
                      Map.entry("othersValidDate", Map.of("type", "string")),
                      Map.entry("allIn", Map.of("type", "number")),
                      Map.entry("ssl", Map.of("type", "string", "description", "船公司")),
                      Map.entry("agent", Map.of("type", "string")),
                      Map.entry("remark", Map.of("type", "string"))),
                  "required",
                  List.of("pol", "pod", "ssl"))));
    }
    if (permissionService.hasPermission(user, PermissionCodes.COST_FUMIGATION_EDIT)) {
      tools.add(
          tool(
              "propose_fumigation_cost",
              "根据用户描述整理一条熏蒸成本字段草稿（不会写入数据库）。前端会打开熏蒸成本库录入表单供用户核对后自行保存。每次只拟一条。有效期建议格式 2026/1/1-2026/12/31。",
              Map.of(
                  "type",
                  "object",
                  "properties",
                  Map.ofEntries(
                      Map.entry("region", Map.of("type", "string")),
                      Map.entry("station", Map.of("type", "string")),
                      Map.entry("outdoorNonOak", Map.of("type", "number")),
                      Map.entry("outdoorOak", Map.of("type", "number")),
                      Map.entry(
                          "outdoorValidity",
                          Map.of("type", "string", "description", "如 2026/1/1-2026/12/31")),
                      Map.entry("indoorNonOak", Map.of("type", "number")),
                      Map.entry("indoorOak", Map.of("type", "number")),
                      Map.entry(
                          "indoorValidity",
                          Map.of("type", "string", "description", "如 2026/1/1-2026/12/31")),
                      Map.entry("address", Map.of("type", "string"))),
                  "required",
                  List.of("region", "station"))));
    }
    return tools;
  }

  /** 当前用户有权跳转的页面键（与 AiToolExecutor.open_page 权限一致）。 */
  private List<String> allowedOpenPages(SysUser user) {
    List<String> pages = new ArrayList<>();
    if (permissionService.hasPermission(user, PermissionCodes.DASHBOARD_VIEW)) {
      pages.add("workspace");
      pages.add("analytics");
    }
    if (permissionService.hasPermission(user, PermissionCodes.QUOTE_VIEW)) {
      pages.add("quote_list");
    }
    if (permissionService.hasPermission(user, PermissionCodes.QUOTE_CREATE)) {
      pages.add("quote_create");
    }
    if (permissionService.hasPermission(user, PermissionCodes.COST_ROAD_VIEW)) {
      pages.add("cost_road");
    }
    if (permissionService.hasPermission(user, PermissionCodes.COST_SEA_VIEW)) {
      pages.add("cost_sea");
    }
    if (permissionService.hasPermission(user, PermissionCodes.COST_FUMIGATION_VIEW)) {
      pages.add("cost_fumigation");
    }
    if (permissionService.hasPermission(user, PermissionCodes.CUSTOMER_VIEW)) {
      pages.add("customer_list");
    }
    if (permissionService.hasPermission(user, PermissionCodes.SUPPLIER_VIEW)) {
      pages.add("supplier_list");
    }
    return pages;
  }

  private static Map<String, Object> tool(String name, String description, Map<String, Object> parameters) {
    Map<String, Object> fn = new LinkedHashMap<>();
    fn.put("name", name);
    fn.put("description", description);
    fn.put("parameters", parameters);
    return Map.of("type", "function", "function", fn);
  }

  private static String loadSystemPrompt() {
    try (InputStream in = new ClassPathResource("ai/system-prompt.txt").getInputStream()) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      log.warn("加载 system-prompt 失败，使用内置短提示", ex);
      return "你是福瑞多报价系统 AI 助手，用中文简洁回答系统使用与字段含义问题。查询成本时使用工具，勿编造价格。";
    }
  }
}
