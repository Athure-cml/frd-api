package com.furuiduo.quote.ai;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.furuiduo.quote.ai.dto.AiCitedCost;
import com.furuiduo.quote.ai.dto.AiOpenPage;
import com.furuiduo.quote.ai.dto.AiProposedCost;
import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.cost.dto.FreightCostResponse;
import com.furuiduo.quote.cost.dto.FumigationCostResponse;
import com.furuiduo.quote.cost.dto.RoadCostResponse;
import com.furuiduo.quote.cost.service.CostFumigationService;
import com.furuiduo.quote.cost.service.CostRoadService;
import com.furuiduo.quote.cost.service.CostSeaService;
import com.furuiduo.quote.quote.dto.QuoteDetailResponse;
import com.furuiduo.quote.quote.service.QuoteQueryService;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

@Component
public class AiToolExecutor {

  public record ToolResult(
      String content,
      List<AiCitedCost> citedCosts,
      List<AiProposedCost> proposedCosts,
      List<AiOpenPage> openPages) {
    public static ToolResult of(String content, List<AiCitedCost> citedCosts) {
      return new ToolResult(content, citedCosts, List.of(), List.of());
    }

    public static ToolResult withPages(
        String content, List<AiOpenPage> openPages) {
      return new ToolResult(content, List.of(), List.of(), openPages);
    }

    public static ToolResult propose(
        String content, List<AiProposedCost> proposedCosts, List<AiOpenPage> openPages) {
      return new ToolResult(content, List.of(), proposedCosts, openPages);
    }

    public static ToolResult error(String message) {
      return new ToolResult(
          "{\"error\":\"" + escape(message) + "\"}", List.of(), List.of(), List.of());
    }
  }

  private final ObjectMapper objectMapper;
  private final PermissionService permissionService;
  private final CostRoadService costRoadService;
  private final CostSeaService costSeaService;
  private final CostFumigationService costFumigationService;
  private final QuoteQueryService quoteQueryService;

  public AiToolExecutor(
      ObjectMapper objectMapper,
      PermissionService permissionService,
      CostRoadService costRoadService,
      CostSeaService costSeaService,
      CostFumigationService costFumigationService,
      QuoteQueryService quoteQueryService) {
    this.objectMapper = objectMapper;
    this.permissionService = permissionService;
    this.costRoadService = costRoadService;
    this.costSeaService = costSeaService;
    this.costFumigationService = costFumigationService;
    this.quoteQueryService = quoteQueryService;
  }

  /**
   * 白名单工具。查询为只读；propose_* 只生成草稿，不落库。无删除/批量覆盖工具。
   */
  public ToolResult execute(SysUser user, String name, JsonNode args) {
    try {
      return switch (name == null ? "" : name) {
        case "open_page" -> openPage(user, args);
        case "search_road_costs" -> searchRoad(user, args);
        case "search_sea_costs" -> searchSea(user, args);
        case "search_fumigation_costs" -> searchFumigation(user, args);
        case "get_quote_context" -> getQuote(user, args);
        case "propose_road_cost" -> proposeRoad(user, args);
        case "propose_sea_cost" -> proposeSea(user, args);
        case "propose_fumigation_cost" -> proposeFumigation(user, args);
        default -> ToolResult.error("unknown or forbidden tool");
      };
    } catch (org.springframework.web.server.ResponseStatusException ex) {
      return ToolResult.error(ex.getReason() == null ? "forbidden" : ex.getReason());
    } catch (Exception ex) {
      return ToolResult.error(ex.getMessage());
    }
  }

  private ToolResult searchRoad(SysUser user, JsonNode args) throws Exception {
    require(user, PermissionCodes.COST_ROAD_VIEW, "缺少卡车成本查看权限");
    PageResult<RoadCostResponse> page =
        costRoadService.list(
            1,
            8,
            text(args, "zipCode"),
            text(args, "city"),
            text(args, "state"),
            text(args, "por"),
            text(args, "pol"),
            text(args, "supplier"),
            null,
            null,
            text(args, "status"));
    List<Map<String, Object>> items = new ArrayList<>();
    List<AiCitedCost> cited = new ArrayList<>();
    for (RoadCostResponse row : page.items()) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", row.id());
      m.put("zipCode", row.zipCode());
      m.put("city", row.city());
      m.put("state", row.state());
      m.put("por", row.por());
      m.put("pol", row.pol());
      m.put("supplier", row.supplier());
      m.put("allInNoFm", money(row.allInNoFm()));
      m.put("allInFmNonOak", money(row.allInFmOneWay()));
      m.put("allInFmOak", money(row.allInFmRound()));
      m.put("validDate", row.validDate());
      items.add(m);
      cited.add(
          new AiCitedCost(
              "road",
              row.id(),
              "卡车#" + row.id() + " " + nullToEmpty(row.por()) + " → " + nullToEmpty(row.pol()),
              "供应商 " + nullToEmpty(row.supplier()) + "，ALL IN NO FM " + money(row.allInNoFm())));
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("total", page.total());
    result.put("items", items);
    return ToolResult.of(objectMapper.writeValueAsString(result), cited);
  }

  private ToolResult searchSea(SysUser user, JsonNode args) throws Exception {
    require(user, PermissionCodes.COST_SEA_VIEW, "缺少海运成本查看权限");
    PageResult<FreightCostResponse> page =
        costSeaService.list(
            1,
            8,
            text(args, "por"),
            text(args, "pol"),
            text(args, "pod"),
            text(args, "ssl"),
            null,
            null,
            null,
            null,
            text(args, "status"));
    List<Map<String, Object>> items = new ArrayList<>();
    List<AiCitedCost> cited = new ArrayList<>();
    for (FreightCostResponse row : page.items()) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", row.id());
      m.put("por", row.por());
      m.put("pol", row.pol());
      m.put("pod", row.pod());
      m.put("ssl", row.ssl());
      m.put("containerType", row.containerType());
      m.put("allIn", money(row.allIn()));
      m.put("freight", money(row.freight()));
      items.add(m);
      cited.add(
          new AiCitedCost(
              "sea",
              row.id(),
              "海运#" + row.id() + " " + nullToEmpty(row.pol()) + " → " + nullToEmpty(row.pod()),
              "船司 " + nullToEmpty(row.ssl()) + "，ALL IN " + money(row.allIn())));
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("total", page.total());
    result.put("items", items);
    return ToolResult.of(objectMapper.writeValueAsString(result), cited);
  }

  private ToolResult searchFumigation(SysUser user, JsonNode args) throws Exception {
    require(user, PermissionCodes.COST_FUMIGATION_VIEW, "缺少熏蒸成本查看权限");
    PageResult<FumigationCostResponse> page =
        costFumigationService.list(
            1, 8, text(args, "region"), text(args, "station"), null, null, text(args, "status"));
    List<Map<String, Object>> items = new ArrayList<>();
    List<AiCitedCost> cited = new ArrayList<>();
    for (FumigationCostResponse row : page.items()) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", row.id());
      m.put("region", row.region());
      m.put("station", row.station());
      m.put("outdoorNonOak", money(row.outdoorNonOak()));
      m.put("outdoorOak", money(row.outdoorOak()));
      m.put("indoorNonOak", money(row.indoorNonOak()));
      m.put("indoorOak", money(row.indoorOak()));
      m.put("address", row.address());
      items.add(m);
      cited.add(
          new AiCitedCost(
              "fumigation",
              row.id(),
              "熏蒸#" + row.id() + " " + nullToEmpty(row.region()) + "/" + nullToEmpty(row.station()),
              "户外非橡木 " + money(row.outdoorNonOak())));
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("total", page.total());
    result.put("items", items);
    return ToolResult.of(objectMapper.writeValueAsString(result), cited);
  }

  private ToolResult getQuote(SysUser user, JsonNode args) throws Exception {
    require(user, PermissionCodes.QUOTE_VIEW, "缺少报价单查看权限");
    Long quoteId = args.path("quoteId").asLong(0L);
    if (quoteId <= 0) {
      return ToolResult.error("quoteId 无效");
    }
    QuoteDetailResponse detail = quoteQueryService.getById(user, quoteId);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", detail.id());
    m.put("quoteNo", detail.quoteNo());
    m.put("customerName", detail.customerName());
    m.put("transportMode", detail.transportMode());
    m.put("routeSummary", detail.routeSummary());
    m.put("status", detail.status());
    m.put("totalAmount", detail.totalAmount());
    m.put("currency", detail.currency());
    m.put("validUntil", detail.validUntil());
    m.put("remark", detail.remark());
    return ToolResult.of(objectMapper.writeValueAsString(m), List.of());
  }

  /** 打开系统页面（真正路由跳转由前端执行）。 */
  private ToolResult openPage(SysUser user, JsonNode args) throws Exception {
    String page = text(args, "page");
    if (page == null || page.isBlank()) {
      return ToolResult.error("page 不能为空");
    }
    PageTarget target = resolvePage(page.trim().toLowerCase());
    if (target == null) {
      return ToolResult.error(
          "未知页面: "
              + page
              + "。可用: workspace, analytics, quote_list, quote_create, cost_road, cost_sea,"
              + " cost_fumigation, customer_list, supplier_list");
    }
    if (target.permission() != null) {
      require(user, target.permission(), "缺少打开「" + target.title() + "」的权限");
    }
    AiOpenPage open = new AiOpenPage(target.page(), target.routeName(), target.title());
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("opened", true);
    result.put("page", target.page());
    result.put("routeName", target.routeName());
    result.put("title", target.title());
    result.put("message", "前端将跳转到「" + target.title() + "」");
    return ToolResult.withPages(objectMapper.writeValueAsString(result), List.of(open));
  }

  private record PageTarget(String page, String routeName, String title, String permission) {}

  private static PageTarget resolvePage(String page) {
    return switch (page) {
      case "workspace", "dashboard", "工作台" ->
          new PageTarget("workspace", "Workspace", "工作台", PermissionCodes.DASHBOARD_VIEW);
      case "analytics", "分析", "数据看板" ->
          new PageTarget("analytics", "Analytics", "分析页", PermissionCodes.DASHBOARD_VIEW);
      case "quote_list", "quotes", "报价列表", "报价单" ->
          new PageTarget("quote_list", "QuoteList", "报价列表", PermissionCodes.QUOTE_VIEW);
      case "quote_create", "new_quote", "新建报价" ->
          new PageTarget("quote_create", "QuoteCreate", "新建报价", PermissionCodes.QUOTE_CREATE);
      case "cost_road", "road", "卡车成本", "卡车成本库" ->
          new PageTarget("cost_road", "CostLibraryRoad", "卡车成本库", PermissionCodes.COST_ROAD_VIEW);
      case "cost_sea", "sea", "海运成本", "海运成本库" ->
          new PageTarget("cost_sea", "CostLibrarySea", "海运成本库", PermissionCodes.COST_SEA_VIEW);
      case "cost_fumigation", "fumigation", "熏蒸成本", "熏蒸成本库" ->
          new PageTarget(
              "cost_fumigation",
              "CostLibraryFumigation",
              "熏蒸成本库",
              PermissionCodes.COST_FUMIGATION_VIEW);
      case "customer_list", "customers", "客户", "客户列表" ->
          new PageTarget("customer_list", "CustomerList", "客户列表", PermissionCodes.CUSTOMER_VIEW);
      case "supplier_list", "suppliers", "供应商", "供应商列表", "卡车供应商" ->
          new PageTarget(
              "supplier_list",
              "SupplierTruckList",
              "卡车供应商列表",
              PermissionCodes.SUPPLIER_TRUCK_VIEW);
      default -> null;
    };
  }

  /** 仅生成卡车成本草稿，不落库；须用户在前端确认后走正式创建接口。 */
  private ToolResult proposeRoad(SysUser user, JsonNode args) throws Exception {
    require(user, PermissionCodes.COST_ROAD_EDIT, "缺少卡车成本编辑权限，无法拟新增");
    Map<String, Object> payload = new LinkedHashMap<>();
    putText(payload, "zipCode", text(args, "zipCode"));
    putText(payload, "city", text(args, "city"));
    putText(payload, "state", text(args, "state"));
    putText(payload, "por", text(args, "por"));
    putText(payload, "pol", text(args, "pol"));
    putText(payload, "supplier", text(args, "supplier"));
    putDecimal(payload, "baseFreight", decimal(args, "baseFreight"));
    putDecimal(payload, "fsc", decimal(args, "fsc"));
    putDecimal(payload, "chassis", decimal(args, "chassis"));
    putDecimal(payload, "triTandemAxle", decimal(args, "triTandemAxle"));
    putDecimal(payload, "split", decimal(args, "split"));
    putDecimal(payload, "stopOff", decimal(args, "stopOff"));
    putDecimal(payload, "allInNoFm", decimal(args, "allInNoFm"));
    putDecimal(payload, "allInFmOneWay", decimal(args, "allInFmOneWay"));
    putDecimal(payload, "allInFmRound", decimal(args, "allInFmRound"));
    putDecimal(payload, "waitingFee", decimal(args, "waitingFee"));
    putDecimal(payload, "redelivery", decimal(args, "redelivery"));
    putDecimal(payload, "prepull", decimal(args, "prepull"));
    putDecimal(payload, "nsLift", decimal(args, "nsLift"));
    putDecimal(payload, "otherFee", decimal(args, "otherFee"));
    putText(payload, "remark", text(args, "remark"));
    putText(payload, "validDate", text(args, "validDate"));
    putText(payload, "logYardNameAddress", text(args, "logYardNameAddress"));
    payload.put("status", "active");

    List<String> warnings = new ArrayList<>();
    requireField(warnings, "zipCode", payload.get("zipCode"), "ZIP CODE");
    requireField(warnings, "city", payload.get("city"), "City");
    requireField(warnings, "state", payload.get("state"), "State");
    requireField(warnings, "por", payload.get("por"), "POR");
    requireField(warnings, "pol", payload.get("pol"), "POL");
    requireField(warnings, "supplier", payload.get("supplier"), "SUPPLIER");
    if (payload.get("allInNoFm") == null
        && payload.get("allInFmOneWay") == null
        && payload.get("allInFmRound") == null) {
      warnings.add("ALL IN 未给出：保存时将尝试按供应商公式计算，若无公式需补全");
    }

    String title =
        "拟新增卡车 "
            + nullToEmpty((String) payload.get("por"))
            + " → "
            + nullToEmpty((String) payload.get("pol"));
    String summary =
        "供应商 "
            + nullToEmpty((String) payload.get("supplier"))
            + " · "
            + nullToEmpty((String) payload.get("city"))
            + "/"
            + nullToEmpty((String) payload.get("state"));
    AiProposedCost proposed =
        new AiProposedCost("road", title, summary, payload, List.copyOf(warnings));

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("proposed", true);
    result.put("persisted", false);
    result.put("message", "已整理字段草稿；前端将打开成本库录入表单，由用户核对后保存");
    result.put("warnings", warnings);
    result.put("payload", payload);
    return ToolResult.propose(
        objectMapper.writeValueAsString(result),
        List.of(proposed),
        List.of(new AiOpenPage("cost_road", "CostLibraryRoad", "卡车成本库")));
  }

  private ToolResult proposeSea(SysUser user, JsonNode args) throws Exception {
    require(user, PermissionCodes.COST_SEA_EDIT, "缺少海运成本编辑权限，无法拟新增");
    Map<String, Object> payload = new LinkedHashMap<>();
    putText(payload, "por", text(args, "por"));
    putText(payload, "pol", text(args, "pol"));
    putText(payload, "pod", text(args, "pod"));
    putText(payload, "cnShortName", text(args, "cnShortName"));
    putText(payload, "enProductName", text(args, "enProductName"));
    putText(payload, "containerType", text(args, "containerType"));
    putDecimal(payload, "freight", decimal(args, "freight"));
    putText(payload, "freightValidDate", text(args, "freightValidDate"));
    putDecimal(payload, "buc", decimal(args, "buc"));
    putText(payload, "bucValidDate", text(args, "bucValidDate"));
    putDecimal(payload, "ebs", decimal(args, "ebs"));
    putText(payload, "ebsValidDate", text(args, "ebsValidDate"));
    putDecimal(payload, "gri", decimal(args, "gri"));
    putText(payload, "griValidDate", text(args, "griValidDate"));
    putDecimal(payload, "others", decimal(args, "others"));
    putText(payload, "othersValidDate", text(args, "othersValidDate"));
    putDecimal(payload, "allIn", decimal(args, "allIn"));
    putText(payload, "ssl", text(args, "ssl"));
    putText(payload, "agent", text(args, "agent"));
    putText(payload, "remark", text(args, "remark"));
    payload.put("status", "active");

    List<String> warnings = new ArrayList<>();
    requireField(warnings, "pol", payload.get("pol"), "POL");
    requireField(warnings, "pod", payload.get("pod"), "POD");
    requireField(warnings, "ssl", payload.get("ssl"), "SSL");
    if (payload.get("freight") == null && payload.get("allIn") == null) {
      warnings.add("运费/ALL IN 未给出，请在表单中补全");
    }

    String title =
        "拟新增海运 "
            + nullToEmpty((String) payload.get("pol"))
            + " → "
            + nullToEmpty((String) payload.get("pod"));
    String summary =
        "船司 "
            + nullToEmpty((String) payload.get("ssl"))
            + (payload.get("containerType") == null
                ? ""
                : " · 箱型 " + payload.get("containerType"));
    AiProposedCost proposed =
        new AiProposedCost("sea", title, summary, payload, List.copyOf(warnings));

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("proposed", true);
    result.put("persisted", false);
    result.put("message", "已整理海运字段草稿；前端将打开成本库录入表单，由用户核对后保存");
    result.put("warnings", warnings);
    result.put("payload", payload);
    return ToolResult.propose(
        objectMapper.writeValueAsString(result),
        List.of(proposed),
        List.of(new AiOpenPage("cost_sea", "CostLibrarySea", "海运成本库")));
  }

  private ToolResult proposeFumigation(SysUser user, JsonNode args) throws Exception {
    require(user, PermissionCodes.COST_FUMIGATION_EDIT, "缺少熏蒸成本编辑权限，无法拟新增");
    Map<String, Object> payload = new LinkedHashMap<>();
    putText(payload, "region", text(args, "region"));
    putText(payload, "station", text(args, "station"));
    putDecimal(payload, "outdoorNonOak", decimal(args, "outdoorNonOak"));
    putDecimal(payload, "outdoorOak", decimal(args, "outdoorOak"));
    putText(payload, "outdoorValidity", text(args, "outdoorValidity"));
    putDecimal(payload, "indoorNonOak", decimal(args, "indoorNonOak"));
    putDecimal(payload, "indoorOak", decimal(args, "indoorOak"));
    putText(payload, "indoorValidity", text(args, "indoorValidity"));
    putText(payload, "address", text(args, "address"));
    payload.put("status", "active");

    List<String> warnings = new ArrayList<>();
    requireField(warnings, "region", payload.get("region"), "REGION");
    requireField(warnings, "station", payload.get("station"), "STATION");

    String title =
        "拟新增熏蒸 "
            + nullToEmpty((String) payload.get("region"))
            + "/"
            + nullToEmpty((String) payload.get("station"));
    String summary =
        payload.get("outdoorNonOak") == null
            ? "请核对户外/户内费用与有效期"
            : "户外非橡木 " + payload.get("outdoorNonOak");
    AiProposedCost proposed =
        new AiProposedCost("fumigation", title, summary, payload, List.copyOf(warnings));

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("proposed", true);
    result.put("persisted", false);
    result.put("message", "已整理熏蒸字段草稿；前端将打开成本库录入表单，由用户核对后保存");
    result.put("warnings", warnings);
    result.put("payload", payload);
    return ToolResult.propose(
        objectMapper.writeValueAsString(result),
        List.of(proposed),
        List.of(new AiOpenPage("cost_fumigation", "CostLibraryFumigation", "熏蒸成本库")));
  }

  private void require(SysUser user, String code, String message) {
    if (!permissionService.hasPermission(user, code)) {
      throw new IllegalStateException(message);
    }
  }

  private static void requireField(
      List<String> warnings, String key, Object value, String label) {
    if (value == null || String.valueOf(value).isBlank()) {
      warnings.add("缺少必填项：" + label);
    }
  }

  private static void putText(Map<String, Object> payload, String key, String value) {
    if (value != null) {
      payload.put(key, value);
    }
  }

  private static void putDecimal(Map<String, Object> payload, String key, BigDecimal value) {
    if (value != null) {
      payload.put(key, value);
    }
  }

  private static String text(JsonNode args, String field) {
    if (args == null || !args.has(field) || args.get(field).isNull()) {
      return null;
    }
    String v = args.get(field).asText();
    return v == null || v.isBlank() ? null : v.trim();
  }

  private static BigDecimal decimal(JsonNode args, String field) {
    if (args == null || !args.has(field) || args.get(field).isNull()) {
      return null;
    }
    JsonNode node = args.get(field);
    try {
      if (node.isNumber()) {
        return node.decimalValue();
      }
      String raw = node.asText();
      if (raw == null || raw.isBlank()) {
        return null;
      }
      return new BigDecimal(raw.trim().replace(",", ""));
    } catch (Exception ex) {
      return null;
    }
  }

  private static String money(BigDecimal v) {
    return v == null ? null : v.toPlainString();
  }

  private static String nullToEmpty(String v) {
    return v == null ? "" : v;
  }

  private static String escape(String message) {
    if (message == null) {
      return "error";
    }
    return message.replace("\\", "\\\\").replace("\"", "'");
  }
}
