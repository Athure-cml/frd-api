package com.furuiduo.quote.sys;

public final class PermissionCodes {

  private PermissionCodes() {}

  public static final String SYS_DEPT_VIEW = "sys:dept:view";
  public static final String SYS_DEPT_MANAGE = "sys:dept:manage";
  public static final String SYS_USER_VIEW = "sys:user:view";
  public static final String SYS_USER_MANAGE = "sys:user:manage";
  public static final String SYS_ROLE_VIEW = "sys:role:view";
  public static final String SYS_ROLE_MANAGE = "sys:role:manage";

  public static final String DASHBOARD_VIEW = "dashboard:view";

  public static final String COST_ROAD_VIEW = "cost:road:view";
  public static final String COST_ROAD_EDIT = "cost:road:edit";
  public static final String COST_SEA_VIEW = "cost:sea:view";
  public static final String COST_SEA_EDIT = "cost:sea:edit";
  public static final String COST_FUMIGATION_VIEW = "cost:fumigation:view";
  public static final String COST_FUMIGATION_EDIT = "cost:fumigation:edit";

  /** @deprecated 已由熏蒸成本库替代，仅用于权限迁移 */
  public static final String COST_RAIL_VIEW = "cost:rail:view";
  /** @deprecated 已由熏蒸成本库替代，仅用于权限迁移 */
  public static final String COST_RAIL_EDIT = "cost:rail:edit";

  public static final String COST_ROAD_TEMPLATE_VIEW = CostTemplatePermissionCodes.ROAD_VIEW;
  public static final String COST_ROAD_TEMPLATE_EDIT = CostTemplatePermissionCodes.ROAD_EDIT;
  public static final String COST_ROAD_TEMPLATE_DELETE = CostTemplatePermissionCodes.ROAD_DELETE;
  public static final String COST_SEA_TEMPLATE_VIEW = CostTemplatePermissionCodes.SEA_VIEW;
  public static final String COST_SEA_TEMPLATE_EDIT = CostTemplatePermissionCodes.SEA_EDIT;
  public static final String COST_SEA_TEMPLATE_DELETE = CostTemplatePermissionCodes.SEA_DELETE;
  public static final String COST_FUMIGATION_TEMPLATE_VIEW =
      CostTemplatePermissionCodes.FUMIGATION_VIEW;
  public static final String COST_FUMIGATION_TEMPLATE_EDIT =
      CostTemplatePermissionCodes.FUMIGATION_EDIT;
  public static final String COST_FUMIGATION_TEMPLATE_DELETE =
      CostTemplatePermissionCodes.FUMIGATION_DELETE;

  /** @deprecated 已由熏蒸模板替代 */
  public static final String COST_RAIL_TEMPLATE_VIEW = CostTemplatePermissionCodes.RAIL_VIEW;
  /** @deprecated 已由熏蒸模板替代 */
  public static final String COST_RAIL_TEMPLATE_EDIT = CostTemplatePermissionCodes.RAIL_EDIT;
  /** @deprecated 已由熏蒸模板替代 */
  public static final String COST_RAIL_TEMPLATE_DELETE = CostTemplatePermissionCodes.RAIL_DELETE;

  public static final String QUOTE_VIEW = "quote:view";
  public static final String QUOTE_CREATE = "quote:create";
  public static final String QUOTE_EDIT = "quote:edit";
  public static final String QUOTE_SUBMIT = "quote:submit";
  public static final String QUOTE_APPROVE = "quote:approve";
  public static final String QUOTE_EXPORT = "quote:export";
  public static final String QUOTE_DELETE = "quote:delete";

  public static final String CUSTOMER_VIEW = "customer:view";
  public static final String CUSTOMER_CREATE = "customer:create";
  public static final String CUSTOMER_EDIT = "customer:edit";
  public static final String CUSTOMER_DELETE = "customer:delete";

  public static final String SUPPLIER_TRUCK_VIEW = "supplier:truck:view";
  public static final String SUPPLIER_TRUCK_CREATE = "supplier:truck:create";
  public static final String SUPPLIER_TRUCK_EDIT = "supplier:truck:edit";
  public static final String SUPPLIER_TRUCK_DELETE = "supplier:truck:delete";
  public static final String SUPPLIER_FUMIGATION_VIEW = "supplier:fumigation:view";
  public static final String SUPPLIER_FUMIGATION_CREATE = "supplier:fumigation:create";
  public static final String SUPPLIER_FUMIGATION_EDIT = "supplier:fumigation:edit";
  public static final String SUPPLIER_FUMIGATION_DELETE = "supplier:fumigation:delete";
  public static final String SUPPLIER_YARD_VIEW = "supplier:yard:view";
  public static final String SUPPLIER_YARD_CREATE = "supplier:yard:create";
  public static final String SUPPLIER_YARD_EDIT = "supplier:yard:edit";
  public static final String SUPPLIER_YARD_DELETE = "supplier:yard:delete";
  public static final String SUPPLIER_OTHER_VIEW = "supplier:other:view";
  public static final String SUPPLIER_OTHER_CREATE = "supplier:other:create";
  public static final String SUPPLIER_OTHER_EDIT = "supplier:other:edit";
  public static final String SUPPLIER_OTHER_DELETE = "supplier:other:delete";

  public static final String SHIPPING_LINE_VIEW = "shipping_line:view";
  public static final String SHIPPING_LINE_CREATE = "shipping_line:create";
  public static final String SHIPPING_LINE_EDIT = "shipping_line:edit";
  public static final String SHIPPING_LINE_DELETE = "shipping_line:delete";

  public static final String AGENT_VIEW = "agent:view";
  public static final String AGENT_CREATE = "agent:create";
  public static final String AGENT_EDIT = "agent:edit";
  public static final String AGENT_DELETE = "agent:delete";

  public static final String CURRENCY_VIEW = "currency:view";
  public static final String CURRENCY_MANAGE = "currency:manage";
  public static final String EXCHANGE_RATE_VIEW = "exchange_rate:view";
  public static final String EXCHANGE_RATE_MANAGE = "exchange_rate:manage";
  public static final String UNIT_VIEW = "unit:view";
  public static final String UNIT_MANAGE = "unit:manage";

  public static final String MD_US_STATE_VIEW = "md_us_state:view";
  public static final String MD_US_STATE_MANAGE = "md_us_state:manage";
  public static final String MD_DEST_ADDRESS_VIEW = "md_dest_address:view";
  public static final String MD_DEST_ADDRESS_MANAGE = "md_dest_address:manage";
  public static final String MD_GLOBAL_PORT_VIEW = "md_global_port:view";
  public static final String MD_GLOBAL_PORT_MANAGE = "md_global_port:manage";
  public static final String MD_INLAND_POR_VIEW = "md_inland_por:view";
  public static final String MD_INLAND_POR_MANAGE = "md_inland_por:manage";
  public static final String MD_CONTAINER_TYPE_VIEW = "md_container_type:view";
  public static final String MD_CONTAINER_TYPE_MANAGE = "md_container_type:manage";

  public static final String SYS_OPERATION_LOG_VIEW = "sys:operation_log:view";

  public static final String AI_USE = "ai:use";

  public static final String REPORT_VIEW = "report:view";
  public static final String REPORT_EXPORT = "report:export";

  public static String[] all() {
    return new String[] {
      SYS_DEPT_VIEW,
      SYS_DEPT_MANAGE,
      SYS_USER_VIEW,
      SYS_USER_MANAGE,
      SYS_ROLE_VIEW,
      SYS_ROLE_MANAGE,
      DASHBOARD_VIEW,
      COST_ROAD_VIEW,
      COST_ROAD_EDIT,
      COST_SEA_VIEW,
      COST_SEA_EDIT,
      COST_FUMIGATION_VIEW,
      COST_FUMIGATION_EDIT,
      COST_ROAD_TEMPLATE_VIEW,
      COST_ROAD_TEMPLATE_EDIT,
      COST_ROAD_TEMPLATE_DELETE,
      COST_SEA_TEMPLATE_VIEW,
      COST_SEA_TEMPLATE_EDIT,
      COST_SEA_TEMPLATE_DELETE,
      COST_FUMIGATION_TEMPLATE_VIEW,
      COST_FUMIGATION_TEMPLATE_EDIT,
      COST_FUMIGATION_TEMPLATE_DELETE,
      QUOTE_VIEW,
      QUOTE_CREATE,
      QUOTE_EDIT,
      QUOTE_SUBMIT,
      QUOTE_APPROVE,
      QUOTE_EXPORT,
      QUOTE_DELETE,
      CUSTOMER_VIEW,
      CUSTOMER_CREATE,
      CUSTOMER_EDIT,
      CUSTOMER_DELETE,
      SUPPLIER_TRUCK_VIEW,
      SUPPLIER_TRUCK_CREATE,
      SUPPLIER_TRUCK_EDIT,
      SUPPLIER_TRUCK_DELETE,
      SUPPLIER_FUMIGATION_VIEW,
      SUPPLIER_FUMIGATION_CREATE,
      SUPPLIER_FUMIGATION_EDIT,
      SUPPLIER_FUMIGATION_DELETE,
      SUPPLIER_YARD_VIEW,
      SUPPLIER_YARD_CREATE,
      SUPPLIER_YARD_EDIT,
      SUPPLIER_YARD_DELETE,
      SUPPLIER_OTHER_VIEW,
      SUPPLIER_OTHER_CREATE,
      SUPPLIER_OTHER_EDIT,
      SUPPLIER_OTHER_DELETE,
      SHIPPING_LINE_VIEW,
      SHIPPING_LINE_CREATE,
      SHIPPING_LINE_EDIT,
      SHIPPING_LINE_DELETE,
      AGENT_VIEW,
      AGENT_CREATE,
      AGENT_EDIT,
      AGENT_DELETE,
      CURRENCY_VIEW,
      CURRENCY_MANAGE,
      EXCHANGE_RATE_VIEW,
      EXCHANGE_RATE_MANAGE,
      UNIT_VIEW,
      UNIT_MANAGE,
      MD_US_STATE_VIEW,
      MD_US_STATE_MANAGE,
      MD_DEST_ADDRESS_VIEW,
      MD_DEST_ADDRESS_MANAGE,
      MD_GLOBAL_PORT_VIEW,
      MD_GLOBAL_PORT_MANAGE,
      MD_INLAND_POR_VIEW,
      MD_INLAND_POR_MANAGE,
      MD_CONTAINER_TYPE_VIEW,
      MD_CONTAINER_TYPE_MANAGE,
      SYS_OPERATION_LOG_VIEW,
      AI_USE,
      REPORT_VIEW,
      REPORT_EXPORT
    };
  }
}
