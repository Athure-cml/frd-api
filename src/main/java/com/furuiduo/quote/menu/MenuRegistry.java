package com.furuiduo.quote.menu;

import java.util.List;

import com.furuiduo.quote.menu.dto.MenuRouteDto;
import com.furuiduo.quote.sys.CostTemplatePermissionCodes;
import com.furuiduo.quote.sys.PermissionCodes;

public final class MenuRegistry {

  private MenuRegistry() {}

  public static List<MenuRouteDto> all() {
    return List.of(
        dashboard(),
        quote(),
        costLibrary(),
        templateManage(),
        customer(),
        masterData(),
        system());
  }

  private static MenuRouteDto dashboard() {
    return MenuRouteDto.of("Dashboard", "/dashboard")
        .redirect("/workspace")
        .meta("icon", "lucide:layout-dashboard")
        .meta("order", -1)
        .meta("title", "page.dashboard.title")
        .requirePermission(PermissionCodes.DASHBOARD_VIEW)
        .child(
            MenuRouteDto.of("Workspace", "/workspace")
                .component("/dashboard/workspace/index")
                .meta("affixTab", true)
                .meta("icon", "carbon:workspace")
                .meta("title", "page.dashboard.workspace")
                .requirePermission(PermissionCodes.DASHBOARD_VIEW))
        .child(
            MenuRouteDto.of("Analytics", "/analytics")
                .component("/dashboard/analytics/index")
                .meta("icon", "lucide:area-chart")
                .meta("title", "page.dashboard.analytics")
                .requirePermission(PermissionCodes.DASHBOARD_VIEW));
  }

  private static MenuRouteDto costLibrary() {
    return MenuRouteDto.of("CostLibrary", "/cost-library")
        .redirect("/cost-library/road")
        .meta("icon", "lucide:database")
        .meta("order", 3)
        .meta("title", "page.costLibrary.title")
        .child(
            MenuRouteDto.of("CostLibraryRoad", "/cost-library/road")
                .component("/cost-library/road/index")
                .meta("icon", "lucide:truck")
                .meta("title", "page.costLibrary.road")
                .requirePermission(PermissionCodes.COST_ROAD_VIEW))
        .child(
            MenuRouteDto.of("CostLibrarySea", "/cost-library/sea")
                .component("/cost-library/sea/index")
                .meta("icon", "lucide:ship")
                .meta("title", "page.costLibrary.sea")
                .requirePermission(PermissionCodes.COST_SEA_VIEW))
        .child(
            MenuRouteDto.of("CostLibraryFumigation", "/cost-library/fumigation")
                .component("/cost-library/fumigation/index")
                .meta("icon", "lucide:flame")
                .meta("title", "page.costLibrary.fumigation")
                .requirePermission(PermissionCodes.COST_FUMIGATION_VIEW));
  }

  private static MenuRouteDto templateManage() {
    return MenuRouteDto.of("CostTemplateManage", "/cost-library/templates")
        .redirect("/cost-library/templates/road")
        .meta("icon", "lucide:columns-3")
        .meta("order", 4)
        .meta("title", "page.costLibrary.template.menuTitle")
        .child(
            MenuRouteDto.of("CostTemplateRoad", "/cost-library/templates/road")
                .component("/cost-library/templates/index")
                .meta("costMode", "road")
                .meta("icon", "lucide:truck")
                .meta("title", "page.costLibrary.template.roadMenu")
                .requirePermission(CostTemplatePermissionCodes.ROAD_VIEW))
        .child(
            MenuRouteDto.of("CostTemplateSea", "/cost-library/templates/sea")
                .component("/cost-library/templates/index")
                .meta("costMode", "sea")
                .meta("icon", "lucide:ship")
                .meta("title", "page.costLibrary.template.seaMenu")
                .requirePermission(CostTemplatePermissionCodes.SEA_VIEW))
        .child(
            MenuRouteDto.of("CostTemplateFumigation", "/cost-library/templates/fumigation")
                .component("/cost-library/templates/index")
                .meta("costMode", "fumigation")
                .meta("icon", "lucide:flame")
                .meta("title", "page.costLibrary.template.fumigationMenu")
                .requirePermission(CostTemplatePermissionCodes.FUMIGATION_VIEW));
  }

  private static MenuRouteDto customer() {
    return MenuRouteDto.of("Customer", "/customers")
        .redirect("/customers/list")
        .meta("icon", "lucide:contact")
        .meta("order", 5)
        .meta("title", "page.customer.title")
        .requirePermission(PermissionCodes.CUSTOMER_VIEW)
        .child(
            MenuRouteDto.of("CustomerList", "/customers/list")
                .component("/customer/list/index")
                .meta("icon", "lucide:users")
                .meta("title", "page.customer.list")
                .requirePermission(PermissionCodes.CUSTOMER_VIEW));
  }

  private static MenuRouteDto masterData() {
    return MenuRouteDto.of("MasterData", "/master-data")
        .redirect("/master-data/currency")
        .meta("icon", "lucide:coins")
        .meta("order", 6)
        .meta("title", "page.masterData.title")
        .requirePermission(PermissionCodes.CURRENCY_VIEW)
        .child(
            MenuRouteDto.of("MasterDataCurrency", "/master-data/currency")
                .component("/master-data/currency/index")
                .meta("icon", "lucide:badge-dollar-sign")
                .meta("title", "page.masterData.currency")
                .requirePermission(PermissionCodes.CURRENCY_VIEW))
        .child(
            MenuRouteDto.of("MasterDataExchangeRate", "/master-data/exchange-rate")
                .component("/master-data/exchange-rate/index")
                .meta("icon", "lucide:arrow-left-right")
                .meta("title", "page.masterData.exchangeRate")
                .requirePermission(PermissionCodes.EXCHANGE_RATE_VIEW))
        .child(
            MenuRouteDto.of("MasterDataUsStateZip", "/master-data/us-state-zip")
                .component("/master-data/us-state-zip/index")
                .meta("icon", "lucide:map-pin")
                .meta("title", "page.masterData.usStateZip")
                .requirePermission(PermissionCodes.MD_DEST_ADDRESS_VIEW))
        .child(
            MenuRouteDto.of("MasterDataGlobalPort", "/master-data/global-port")
                .component("/master-data/global-port/index")
                .meta("icon", "lucide:anchor")
                .meta("title", "page.masterData.globalPort")
                .requirePermission(PermissionCodes.MD_GLOBAL_PORT_VIEW));
  }

  private static MenuRouteDto quote() {
    return MenuRouteDto.of("Quote", "/quotes")
        .redirect("/quotes/list")
        .meta("icon", "lucide:file-text")
        .meta("order", 2)
        .meta("title", "page.quote.title")
        .requirePermission(PermissionCodes.QUOTE_VIEW)
        .child(
            MenuRouteDto.of("QuoteList", "/quotes/list")
                .component("/quote/list/index")
                .meta("icon", "lucide:list")
                .meta("title", "page.quote.list")
                .requirePermission(PermissionCodes.QUOTE_VIEW));
  }

  private static MenuRouteDto system() {
    return MenuRouteDto.of("System", "/system")
        .redirect("/system/dept")
        .meta("icon", "lucide:settings")
        .meta("order", 7)
        .meta("title", "page.system.title")
        .requireRole("super_admin", "admin")
        .child(
            MenuRouteDto.of("SystemDept", "/system/dept")
                .component("/system/dept/index")
                .meta("icon", "lucide:building-2")
                .meta("title", "page.system.dept")
                .requireRole("super_admin", "admin"))
        .child(
            MenuRouteDto.of("SystemUser", "/system/user")
                .component("/system/user/index")
                .meta("icon", "lucide:users")
                .meta("title", "page.system.user")
                .requireRole("super_admin", "admin"))
        .child(
            MenuRouteDto.of("SystemRole", "/system/role")
                .component("/system/role/index")
                .meta("icon", "lucide:shield")
                .meta("title", "page.system.role")
                .requireRole("super_admin", "admin"))
        .child(
            MenuRouteDto.of("SystemOperationLog", "/system/operation-log")
                .component("/system/operation-log/index")
                .meta("icon", "lucide:scroll-text")
                .meta("title", "page.system.operationLog")
                .requirePermission(PermissionCodes.SYS_OPERATION_LOG_VIEW));
  }
}
