package com.furuiduo.quote.sys.seed;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.PermissionType;
import com.furuiduo.quote.sys.entity.SysPermission;
import com.furuiduo.quote.sys.repository.SysPermissionRepository;

/** 系统权限码全集（新建库 bootstrap 与各增量 seeder 共用）。 */
public final class PermissionCatalog {

  private PermissionCatalog() {}

  public record PermDef(String code, String name, PermissionType type, int sort) {}

  public static List<PermDef> allDefinitions() {
    return List.of(
        new PermDef(PermissionCodes.SYS_DEPT_VIEW, "查看部门", PermissionType.API, 10),
        new PermDef(PermissionCodes.SYS_DEPT_MANAGE, "管理部门", PermissionType.API, 11),
        new PermDef(PermissionCodes.SYS_USER_VIEW, "查看用户", PermissionType.API, 12),
        new PermDef(PermissionCodes.SYS_USER_MANAGE, "管理用户", PermissionType.API, 13),
        new PermDef(PermissionCodes.SYS_ROLE_VIEW, "查看角色", PermissionType.API, 14),
        new PermDef(PermissionCodes.SYS_ROLE_MANAGE, "管理角色", PermissionType.API, 15),
        new PermDef(PermissionCodes.SYS_OPERATION_LOG_VIEW, "操作日志-查看", PermissionType.API, 16),
        new PermDef(PermissionCodes.SYS_ANNOUNCEMENT_VIEW, "系统公告-查看", PermissionType.API, 17),
        new PermDef(PermissionCodes.SYS_ANNOUNCEMENT_MANAGE, "系统公告-管理", PermissionType.API, 18),
        new PermDef(PermissionCodes.DASHBOARD_VIEW, "报价分析", PermissionType.MENU, 20),
        new PermDef(PermissionCodes.COST_ROAD_VIEW, "卡车成本-查看", PermissionType.API, 30),
        new PermDef(PermissionCodes.COST_ROAD_EDIT, "卡车成本-编辑", PermissionType.API, 31),
        new PermDef(PermissionCodes.COST_SEA_VIEW, "海运成本-查看", PermissionType.API, 32),
        new PermDef(PermissionCodes.COST_SEA_EDIT, "海运成本-编辑", PermissionType.API, 33),
        new PermDef(PermissionCodes.COST_FUMIGATION_VIEW, "熏蒸成本-查看", PermissionType.API, 34),
        new PermDef(PermissionCodes.COST_FUMIGATION_EDIT, "熏蒸成本-编辑", PermissionType.API, 35),
        new PermDef(PermissionCodes.COST_ROAD_TEMPLATE_VIEW, "卡车模板-查看", PermissionType.API, 36),
        new PermDef(PermissionCodes.COST_ROAD_TEMPLATE_EDIT, "卡车模板-编辑", PermissionType.API, 37),
        new PermDef(PermissionCodes.COST_ROAD_TEMPLATE_DELETE, "卡车模板-删除", PermissionType.API, 38),
        new PermDef(PermissionCodes.COST_SEA_TEMPLATE_VIEW, "海运模板-查看", PermissionType.API, 39),
        new PermDef(PermissionCodes.COST_SEA_TEMPLATE_EDIT, "海运模板-编辑", PermissionType.API, 40),
        new PermDef(PermissionCodes.COST_SEA_TEMPLATE_DELETE, "海运模板-删除", PermissionType.API, 41),
        new PermDef(PermissionCodes.COST_FUMIGATION_TEMPLATE_VIEW, "熏蒸模板-查看", PermissionType.API, 42),
        new PermDef(PermissionCodes.COST_FUMIGATION_TEMPLATE_EDIT, "熏蒸模板-编辑", PermissionType.API, 43),
        new PermDef(PermissionCodes.COST_FUMIGATION_TEMPLATE_DELETE, "熏蒸模板-删除", PermissionType.API, 44),
        new PermDef(PermissionCodes.QUOTE_VIEW, "报价单-查看", PermissionType.API, 50),
        new PermDef(PermissionCodes.QUOTE_CREATE, "报价单-新建", PermissionType.API, 51),
        new PermDef(PermissionCodes.QUOTE_EDIT, "报价单-编辑", PermissionType.API, 52),
        new PermDef(PermissionCodes.QUOTE_SUBMIT, "报价单-提交", PermissionType.API, 53),
        new PermDef(PermissionCodes.QUOTE_APPROVE, "报价单-审批", PermissionType.API, 54),
        new PermDef(PermissionCodes.QUOTE_EXPORT, "报价单-导出", PermissionType.API, 55),
        new PermDef(PermissionCodes.QUOTE_DELETE, "报价单-删除", PermissionType.API, 56),
        new PermDef(PermissionCodes.CUSTOMER_VIEW, "客户-查看", PermissionType.API, 45),
        new PermDef(PermissionCodes.CUSTOMER_CREATE, "客户-新建", PermissionType.API, 46),
        new PermDef(PermissionCodes.CUSTOMER_EDIT, "客户-编辑", PermissionType.API, 47),
        new PermDef(PermissionCodes.CUSTOMER_DELETE, "客户-删除", PermissionType.API, 48),
        new PermDef(PermissionCodes.SUPPLIER_TRUCK_VIEW, "卡车供应商-查看", PermissionType.API, 63),
        new PermDef(PermissionCodes.SUPPLIER_TRUCK_CREATE, "卡车供应商-新建", PermissionType.API, 64),
        new PermDef(PermissionCodes.SUPPLIER_TRUCK_EDIT, "卡车供应商-编辑", PermissionType.API, 65),
        new PermDef(PermissionCodes.SUPPLIER_TRUCK_DELETE, "卡车供应商-删除", PermissionType.API, 66),
        new PermDef(PermissionCodes.SUPPLIER_FUMIGATION_VIEW, "熏蒸供应商-查看", PermissionType.API, 67),
        new PermDef(PermissionCodes.SUPPLIER_FUMIGATION_CREATE, "熏蒸供应商-新建", PermissionType.API, 68),
        new PermDef(PermissionCodes.SUPPLIER_FUMIGATION_EDIT, "熏蒸供应商-编辑", PermissionType.API, 69),
        new PermDef(PermissionCodes.SUPPLIER_FUMIGATION_DELETE, "熏蒸供应商-删除", PermissionType.API, 70),
        new PermDef(PermissionCodes.SUPPLIER_YARD_VIEW, "仓库堆场-查看", PermissionType.API, 71),
        new PermDef(PermissionCodes.SUPPLIER_YARD_CREATE, "仓库堆场-新建", PermissionType.API, 72),
        new PermDef(PermissionCodes.SUPPLIER_YARD_EDIT, "仓库堆场-编辑", PermissionType.API, 73),
        new PermDef(PermissionCodes.SUPPLIER_YARD_DELETE, "仓库堆场-删除", PermissionType.API, 74),
        new PermDef(PermissionCodes.SUPPLIER_OTHER_VIEW, "其他供应商-查看", PermissionType.API, 75),
        new PermDef(PermissionCodes.SUPPLIER_OTHER_CREATE, "其他供应商-新建", PermissionType.API, 76),
        new PermDef(PermissionCodes.SUPPLIER_OTHER_EDIT, "其他供应商-编辑", PermissionType.API, 77),
        new PermDef(PermissionCodes.SUPPLIER_OTHER_DELETE, "其他供应商-删除", PermissionType.API, 78),
        new PermDef(PermissionCodes.SHIPPING_LINE_VIEW, "船公司-查看", PermissionType.API, 79),
        new PermDef(PermissionCodes.SHIPPING_LINE_CREATE, "船公司-新建", PermissionType.API, 80),
        new PermDef(PermissionCodes.SHIPPING_LINE_EDIT, "船公司-编辑", PermissionType.API, 81),
        new PermDef(PermissionCodes.SHIPPING_LINE_DELETE, "船公司-删除", PermissionType.API, 82),
        new PermDef(PermissionCodes.AGENT_VIEW, "代理商-查看", PermissionType.API, 83),
        new PermDef(PermissionCodes.AGENT_CREATE, "代理商-新建", PermissionType.API, 84),
        new PermDef(PermissionCodes.AGENT_EDIT, "代理商-编辑", PermissionType.API, 85),
        new PermDef(PermissionCodes.AGENT_DELETE, "代理商-删除", PermissionType.API, 86),
        new PermDef(PermissionCodes.CURRENCY_VIEW, "币种-查看", PermissionType.API, 49),
        new PermDef(PermissionCodes.CURRENCY_MANAGE, "币种-管理", PermissionType.API, 50),
        new PermDef(PermissionCodes.EXCHANGE_RATE_VIEW, "汇率-查看", PermissionType.API, 51),
        new PermDef(PermissionCodes.EXCHANGE_RATE_MANAGE, "汇率-管理", PermissionType.API, 52),
        new PermDef(PermissionCodes.UNIT_VIEW, "单位-查看", PermissionType.API, 87),
        new PermDef(PermissionCodes.UNIT_MANAGE, "单位-管理", PermissionType.API, 88),
        new PermDef(PermissionCodes.MD_US_STATE_VIEW, "美国州-查看", PermissionType.API, 53),
        new PermDef(PermissionCodes.MD_US_STATE_MANAGE, "美国州-管理", PermissionType.API, 54),
        new PermDef(PermissionCodes.MD_DEST_ADDRESS_VIEW, "美国州邮政编码-查看", PermissionType.API, 55),
        new PermDef(PermissionCodes.MD_DEST_ADDRESS_MANAGE, "美国州邮政编码-管理", PermissionType.API, 56),
        new PermDef(PermissionCodes.MD_GLOBAL_PORT_VIEW, "全球港口-查看", PermissionType.API, 57),
        new PermDef(PermissionCodes.MD_GLOBAL_PORT_MANAGE, "全球港口-管理", PermissionType.API, 58),
        new PermDef(PermissionCodes.MD_INLAND_POR_VIEW, "内陆POR-查看", PermissionType.API, 59),
        new PermDef(PermissionCodes.MD_INLAND_POR_MANAGE, "内陆POR-管理", PermissionType.API, 60),
        new PermDef(PermissionCodes.MD_CONTAINER_TYPE_VIEW, "箱型-查看", PermissionType.API, 61),
        new PermDef(PermissionCodes.MD_CONTAINER_TYPE_MANAGE, "箱型-管理", PermissionType.API, 62),
        new PermDef(PermissionCodes.MD_QUOTE_RULE_VIEW, "报价单规则-查看", PermissionType.API, 89),
        new PermDef(PermissionCodes.MD_QUOTE_RULE_MANAGE, "报价单规则-管理", PermissionType.API, 90),
        new PermDef(PermissionCodes.AI_USE, "AI助手-使用", PermissionType.API, 91),
        new PermDef(PermissionCodes.REPORT_VIEW, "报表-查看", PermissionType.API, 60),
        new PermDef(PermissionCodes.REPORT_EXPORT, "报表-导出", PermissionType.API, 61));
  }

  public static Map<String, SysPermission> ensureAll(SysPermissionRepository permissionRepository) {
    Map<String, SysPermission> map = new LinkedHashMap<>();
    for (PermDef def : allDefinitions()) {
      SysPermission permission =
          permissionRepository
              .findByCode(def.code())
              .orElseGet(
                  () -> {
                    SysPermission created = new SysPermission();
                    created.setCode(def.code());
                    created.setType(def.type());
                    return created;
                  });
      permission.setName(def.name());
      permission.setType(def.type());
      permission.setSort(def.sort());
      permission = permissionRepository.save(permission);
      map.put(def.code(), permission);
    }
    return map;
  }
}
