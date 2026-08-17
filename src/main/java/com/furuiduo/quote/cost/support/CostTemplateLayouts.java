package com.furuiduo.quote.cost.support;

import java.util.List;
import java.util.Map;

import com.furuiduo.quote.cost.dto.CostTableCustomFieldDef;
import com.furuiduo.quote.cost.dto.CostTableFieldOverride;
import com.furuiduo.quote.cost.dto.CostTableTemplateGroup;
import com.furuiduo.quote.cost.dto.CostTableTemplateLayout;

public final class CostTemplateLayouts {

  private CostTemplateLayouts() {}

  /** 海运标准列：字段库全集（默认展示顺序见 seaDefault.fieldOrder） */
  public static final List<String> SEA_STANDARD_FIELDS =
      List.of(
          "por",
          "pol",
          "pod",
          "freight",
          "containerType",
          "freightValidDate",
          "buc",
          "bucValidDate",
          "others",
          "othersValidDate",
          "allIn",
          "ssl",
          "agent",
          "remark",
          "enProductName",
          "cnShortName",
          "ebs",
          "ebsValidDate",
          "gri",
          "griValidDate");

  private static final String SEA_FREIGHT_EFF = "cf_sea_freight_eff";
  private static final String SEA_BUNKER_EFF = "cf_sea_bunker_eff";
  private static final String SEA_OTHERS_EFF = "cf_sea_others_eff";

  private static final String FUM_OUTDOOR_EFF = "cf_fum_outdoor_eff";
  private static final String FUM_INDOOR_EFF = "cf_fum_indoor_eff";

  private static final List<String> SEA_DEFAULT_FIELD_ORDER =
      List.of(
          "por",
          "pol",
          "pod",
          "freight",
          "containerType",
          SEA_FREIGHT_EFF,
          "freightValidDate",
          "buc",
          SEA_BUNKER_EFF,
          "bucValidDate",
          "others",
          SEA_OTHERS_EFF,
          "othersValidDate",
          "allIn",
          "ssl",
          "agent",
          "remark",
          "enProductName");

  private static final String ROAD_YARD_STORAGE = "cf_road_yard_storage";
  private static final String ROAD_EXTRA_CHASSIS = "cf_road_extra_chassis";
  private static final String ROAD_WAITING_UNIT = "cf_road_waiting_unit";
  private static final String ROAD_YARD_STORAGE_UNIT = "cf_road_yard_storage_unit";
  private static final String ROAD_EXTRA_CHASSIS_UNIT = "cf_road_extra_chassis_unit";
  private static final String ROAD_EFF = "cf_road_eff";

  public static CostTableTemplateLayout roadDefault() {
    List<String> fieldOrder =
        List.of(
            "zipCode",
            "city",
            "state",
            "por",
            "supplier",
            "baseFreight",
            "fsc",
            "chassis",
            "triTandemAxle",
            "split",
            "stopOff",
            "allInNoFm",
            "allInFmOneWay",
            "allInFmRound",
            "waitingFee",
            ROAD_WAITING_UNIT,
            "redelivery",
            ROAD_YARD_STORAGE,
            ROAD_YARD_STORAGE_UNIT,
            ROAD_EXTRA_CHASSIS,
            ROAD_EXTRA_CHASSIS_UNIT,
            "prepull",
            "nsLift",
            "otherFee",
            "remark",
            ROAD_EFF,
            "validDate",
            "logYardNameAddress");
    Map<String, CostTableFieldOverride> fieldOverrides = new java.util.LinkedHashMap<>();
    fieldOverrides.put("zipCode", titledRequired("ZIP CODE"));
    fieldOverrides.put("city", titledRequired("CITY"));
    fieldOverrides.put("state", titledRequired("STATE"));
    fieldOverrides.put("por", titledRequired("POR"));
    fieldOverrides.put("supplier", titledRequired("SUPPLIER"));
    fieldOverrides.put("baseFreight", titled("BASE"));
    fieldOverrides.put("fsc", titled("FSC"));
    fieldOverrides.put("chassis", titled("CHASSIS"));
    fieldOverrides.put("triTandemAxle", titled("OW"));
    fieldOverrides.put("split", titled("SPLIT"));
    fieldOverrides.put("stopOff", titled("STOP OFF"));
    fieldOverrides.put("allInNoFm", titledHighlight("ALL IN"));
    fieldOverrides.put("allInFmOneWay", titledHighlight("ALL IN FM NON OAK"));
    fieldOverrides.put("allInFmRound", titledHighlight("ALL IN FM OAK"));
    fieldOverrides.put("waitingFee", titled("WAITING"));
    fieldOverrides.put(ROAD_WAITING_UNIT, titled("WAITING UNIT"));
    fieldOverrides.put("redelivery", titled("REDELIVERY"));
    fieldOverrides.put(ROAD_YARD_STORAGE, titled("YARD STORAGE"));
    fieldOverrides.put(ROAD_YARD_STORAGE_UNIT, titled("YARD STORAGE UNIT"));
    fieldOverrides.put(ROAD_EXTRA_CHASSIS, titled("EXTRA CHASSIS"));
    fieldOverrides.put(ROAD_EXTRA_CHASSIS_UNIT, titled("EXTRA CHASSIS UNIT"));
    fieldOverrides.put("prepull", titled("PREPULL"));
    fieldOverrides.put("nsLift", titled("LIFT"));
    fieldOverrides.put("otherFee", titled("OTHERS"));
    fieldOverrides.put("remark", titled("REMARK"));
    fieldOverrides.put(ROAD_EFF, titled("EFFECTIVE TIME"));
    fieldOverrides.put("validDate", titled("VALID TIME"));
    fieldOverrides.put("logYardNameAddress", titled("PICK UP ADDRESS"));

    List<CostTableCustomFieldDef> customFields =
        List.of(
            new CostTableCustomFieldDef(ROAD_YARD_STORAGE, "YARD STORAGE", null, "number"),
            new CostTableCustomFieldDef(ROAD_YARD_STORAGE_UNIT, "YARD STORAGE UNIT", null, "text"),
            new CostTableCustomFieldDef(ROAD_EXTRA_CHASSIS, "EXTRA CHASSIS", null, "number"),
            new CostTableCustomFieldDef(
                ROAD_EXTRA_CHASSIS_UNIT, "EXTRA CHASSIS UNIT", null, "text"),
            new CostTableCustomFieldDef(ROAD_WAITING_UNIT, "WAITING UNIT", null, "text"),
            new CostTableCustomFieldDef(ROAD_EFF, "EFFECTIVE TIME", null, "text"));

    // 单行英文表头（无分组），对齐业务 Excel
    return new CostTableTemplateLayout(
        List.of(), fieldOrder, fieldOverrides, fieldOrder, customFields);
  }

  private static CostTableFieldOverride titled(String title) {
    return new CostTableFieldOverride(null, null, null, null, title, null, null, null, null);
  }

  private static CostTableFieldOverride titledRequired(String title) {
    return new CostTableFieldOverride(null, null, null, null, title, true, null, null, null);
  }

  private static CostTableFieldOverride titledHighlight(String title) {
    return new CostTableFieldOverride(null, null, null, null, title, null, null, "#E8F1FC", null);
  }

  private static CostTableFieldOverride requiredOverride() {
    return new CostTableFieldOverride(null, null, null, null, null, true, null, null, null);
  }

  /** 必填 + 默认左固定（对齐海运 Excel 橙色区） */
  private static CostTableFieldOverride fixedLeftRequiredOverride() {
    return new CostTableFieldOverride(null, null, null, "left", null, true, null, null, null);
  }

  private static CostTableFieldOverride highlightOverride() {
    return new CostTableFieldOverride(null, null, null, null, null, null, null, "#E8F1FC", null);
  }

  /** 海运 ALL IN 默认绿色底 */
  private static CostTableFieldOverride seaAllInHighlightOverride() {
    return new CostTableFieldOverride(null, null, null, null, null, null, null, "#EAF7F0", null);
  }

  public static CostTableTemplateLayout seaDefault() {
    List<String> fieldOrder = SEA_DEFAULT_FIELD_ORDER;
    Map<String, CostTableFieldOverride> fieldOverrides = new java.util.LinkedHashMap<>();
    fieldOverrides.put("por", fixedLeftRequiredOverride());
    fieldOverrides.put("pol", fixedLeftRequiredOverride());
    fieldOverrides.put("pod", fixedLeftRequiredOverride());
    fieldOverrides.put("freight", fixedLeftRequiredOverride());
    fieldOverrides.put("containerType", fixedLeftRequiredOverride());
    fieldOverrides.put(SEA_FREIGHT_EFF, fixedLeftOverride());
    fieldOverrides.put(
        "freightValidDate",
        new CostTableFieldOverride(null, null, null, "left", "有效期", true, null, null, null));
    fieldOverrides.put(
        "buc", new CostTableFieldOverride(null, null, null, null, "燃油附加费", null, null, null, null));
    fieldOverrides.put(
        "bucValidDate",
        new CostTableFieldOverride(null, null, null, null, "有效期", null, null, null, null));
    fieldOverrides.put(
        "othersValidDate",
        new CostTableFieldOverride(null, null, null, null, "有效期", null, null, null, null));
    fieldOverrides.put("allIn", seaAllInHighlightOverride());
    fieldOverrides.put("enProductName", requiredOverride());

    List<CostTableCustomFieldDef> customFields =
        List.of(
            new CostTableCustomFieldDef(SEA_FREIGHT_EFF, "生效期", null, "text"),
            new CostTableCustomFieldDef(SEA_BUNKER_EFF, "生效期", null, "text"),
            new CostTableCustomFieldDef(SEA_OTHERS_EFF, "生效期", null, "text"));

    return new CostTableTemplateLayout(
        List.of(
            new CostTableTemplateGroup(
                "surcharge",
                "page.costLibrary.seaGroups.surcharge",
                "sea-header-surcharge",
                List.of(
                    "buc",
                    SEA_BUNKER_EFF,
                    "bucValidDate",
                    "others",
                    SEA_OTHERS_EFF,
                    "othersValidDate"))),
        fieldOrder,
        fieldOverrides,
        fieldOrder,
        customFields);
  }

  private static CostTableFieldOverride fixedLeftOverride() {
    return new CostTableFieldOverride(null, null, null, "left", null, null, null, null, null);
  }

  public static CostTableTemplateLayout fumigationDefault() {
    List<String> fieldOrder =
        List.of(
            "region",
            "station",
            "outdoorNonOak",
            "outdoorOak",
            FUM_OUTDOOR_EFF,
            "outdoorValidity",
            "indoorNonOak",
            "indoorOak",
            FUM_INDOOR_EFF,
            "indoorValidity",
            "address");
    Map<String, CostTableFieldOverride> fieldOverrides = new java.util.LinkedHashMap<>();
    fieldOverrides.put("region", requiredOverride());
    fieldOverrides.put("station", requiredOverride());
    fieldOverrides.put("outdoorNonOak", requiredOverride());
    fieldOverrides.put("outdoorOak", requiredOverride());
    fieldOverrides.put(FUM_OUTDOOR_EFF, requiredOverride());
    fieldOverrides.put("outdoorValidity", requiredOverride());
    fieldOverrides.put("indoorNonOak", requiredOverride());
    fieldOverrides.put("indoorOak", requiredOverride());
    fieldOverrides.put(FUM_INDOOR_EFF, requiredOverride());
    fieldOverrides.put("indoorValidity", requiredOverride());
    fieldOverrides.put("address", requiredOverride());

    List<CostTableCustomFieldDef> customFields =
        List.of(
            new CostTableCustomFieldDef(FUM_OUTDOOR_EFF, "生效期", true, "text"),
            new CostTableCustomFieldDef(FUM_INDOOR_EFF, "生效期", true, "text"));

    return new CostTableTemplateLayout(
        List.of(
            new CostTableTemplateGroup(
                "outdoor",
                "page.costLibrary.fumigationGroups.outdoor",
                null,
                List.of("outdoorNonOak", "outdoorOak", FUM_OUTDOOR_EFF, "outdoorValidity")),
            new CostTableTemplateGroup(
                "indoor",
                "page.costLibrary.fumigationGroups.indoor",
                null,
                List.of("indoorNonOak", "indoorOak", FUM_INDOOR_EFF, "indoorValidity"))),
        fieldOrder,
        fieldOverrides,
        fieldOrder,
        customFields);
  }

  /** @deprecated */
  public static CostTableTemplateLayout railDefault() {
    return flatFreightLayout(
        List.of(
            "origin",
            "destination",
            "carrier",
            "spec",
            "unit",
            "unitPrice",
            "currency",
            "validFrom",
            "validTo",
            "status",
            "remark",
            "updatedAt"));
  }

  private static CostTableTemplateLayout flatFreightLayout(List<String> fields) {
    return new CostTableTemplateLayout(null, fields, null, fields, null);
  }
}
