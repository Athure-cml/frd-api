package com.furuiduo.quote.cost.support;

import java.util.List;
import java.util.Map;

import com.furuiduo.quote.cost.dto.CostTableFieldOverride;
import com.furuiduo.quote.cost.dto.CostTableTemplateGroup;
import com.furuiduo.quote.cost.dto.CostTableTemplateLayout;

public final class CostTemplateLayouts {

  private CostTemplateLayouts() {}

  /** 海运标准列：对齐业务 Excel 表头 */
  public static final List<String> SEA_STANDARD_FIELDS =
      List.of(
          "por",
          "pol",
          "pod",
          "cnShortName",
          "enProductName",
          "containerType",
          "freight",
          "freightValidDate",
          "buc",
          "bucValidDate",
          "ebs",
          "ebsValidDate",
          "gri",
          "griValidDate",
          "others",
          "othersValidDate",
          "allIn",
          "ssl",
          "agent",
          "remark");

  public static CostTableTemplateLayout roadDefault() {
    List<String> fieldOrder =
        List.of(
            "zipCode",
            "city",
            "state",
            "por",
            "pol",
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
            "redelivery",
            "prepull",
            "nsLift",
            "otherFee",
            "remark",
            "validDate",
            "logYardNameAddress");
    Map<String, CostTableFieldOverride> fieldOverrides =
        Map.of(
            "zipCode", requiredOverride(),
            "city", requiredOverride(),
            "state", requiredOverride(),
            "por", requiredOverride(),
            "pol", requiredOverride(),
            "supplier", requiredOverride(),
            "allInNoFm", requiredOverride(),
            "allInFmOneWay", requiredOverride(),
            "allInFmRound", requiredOverride());
    return new CostTableTemplateLayout(
        List.of(
            new CostTableTemplateGroup(
                "route",
                "page.costLibrary.roadGroups.route",
                "road-header-route",
                List.of(
                    "zipCode",
                    "city",
                    "state",
                    "por",
                    "pol",
                    "supplier",
                    "baseFreight",
                    "fsc",
                    "chassis",
                    "triTandemAxle",
                    "split",
                    "stopOff")),
            new CostTableTemplateGroup(
                "freight",
                "page.costLibrary.roadGroups.freight",
                "road-header-freight",
                List.of("allInNoFm", "allInFmOneWay", "allInFmRound")),
            new CostTableTemplateGroup(
                "extra",
                "page.costLibrary.roadGroups.extra",
                "road-header-extra",
                List.of(
                    "waitingFee",
                    "redelivery",
                    "prepull",
                    "nsLift",
                    "otherFee",
                    "remark")),
            new CostTableTemplateGroup(
                "meta",
                "page.costLibrary.roadGroups.meta",
                "road-header-meta",
                List.of("validDate", "logYardNameAddress"))),
        fieldOrder,
        fieldOverrides,
        fieldOrder,
        null);
  }

  private static CostTableFieldOverride requiredOverride() {
    return new CostTableFieldOverride(null, null, null, null, null, true, null);
  }

  public static CostTableTemplateLayout seaDefault() {
    List<String> fieldOrder = SEA_STANDARD_FIELDS;
    Map<String, CostTableFieldOverride> fieldOverrides =
        Map.of(
            "por", requiredOverride(),
            "pol", requiredOverride(),
            "pod", requiredOverride(),
            "enProductName", requiredOverride(),
            "containerType", requiredOverride(),
            "freight", requiredOverride(),
            "freightValidDate", requiredOverride());
    return new CostTableTemplateLayout(
        List.of(
            new CostTableTemplateGroup(
                "surcharge",
                "page.costLibrary.seaGroups.surcharge",
                "sea-header-surcharge",
                List.of(
                    "buc",
                    "bucValidDate",
                    "ebs",
                    "ebsValidDate",
                    "gri",
                    "griValidDate",
                    "others",
                    "othersValidDate"))),
        fieldOrder,
        fieldOverrides,
        fieldOrder,
        null);
  }

  public static CostTableTemplateLayout fumigationDefault() {
    List<String> fieldOrder =
        List.of(
            "region",
            "station",
            "outdoorNonOak",
            "outdoorOak",
            "outdoorValidity",
            "indoorNonOak",
            "indoorOak",
            "indoorValidity",
            "address");
    Map<String, CostTableFieldOverride> fieldOverrides =
        Map.of(
            "region", requiredOverride(),
            "station", requiredOverride(),
            "outdoorNonOak", requiredOverride(),
            "outdoorOak", requiredOverride(),
            "outdoorValidity", requiredOverride(),
            "indoorNonOak", requiredOverride(),
            "indoorOak", requiredOverride(),
            "indoorValidity", requiredOverride(),
            "address", requiredOverride());
    return new CostTableTemplateLayout(
        List.of(
            new CostTableTemplateGroup(
                "outdoor",
                "page.costLibrary.fumigationGroups.outdoor",
                "fumigation-header-primary",
                List.of("outdoorNonOak", "outdoorOak", "outdoorValidity")),
            new CostTableTemplateGroup(
                "indoor",
                "page.costLibrary.fumigationGroups.indoor",
                "fumigation-header-primary",
                List.of("indoorNonOak", "indoorOak", "indoorValidity"))),
        fieldOrder,
        fieldOverrides,
        fieldOrder,
        null);
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
