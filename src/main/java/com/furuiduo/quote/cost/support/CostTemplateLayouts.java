package com.furuiduo.quote.cost.support;

import java.util.List;

import com.furuiduo.quote.cost.dto.CostTableTemplateGroup;
import com.furuiduo.quote.cost.dto.CostTableTemplateLayout;

public final class CostTemplateLayouts {

  private CostTemplateLayouts() {}

  /** 海运标准列：Excel 七列 + 备注、有效期、状态 */
  public static final List<String> SEA_STANDARD_FIELDS =
      List.of(
          "origin",
          "destination",
          "unitPrice",
          "buc",
          "surchargeValidDate",
          "allIn",
          "carrier",
          "remark",
          "validDate",
          "status");

  public static CostTableTemplateLayout roadDefault() {
    return new CostTableTemplateLayout(
        List.of(
            new CostTableTemplateGroup(
                "basic",
                "page.costLibrary.roadGroups.basic",
                "road-header-green",
                List.of(
                    "validDate",
                    "supplier",
                    "logYardNameAddress",
                    "city",
                    "state",
                    "por",
                    "pol")),
            new CostTableTemplateGroup(
                "freight",
                "page.costLibrary.roadGroups.freight",
                "road-header-freight",
                List.of(
                    "baseFreight",
                    "fsc",
                    "chassis",
                    "owTriAxle",
                    "split",
                    "stopOff",
                    "allIn",
                    "allInNonOak",
                    "allInOak")),
            new CostTableTemplateGroup(
                "surcharge",
                "page.costLibrary.roadGroups.surcharge",
                "road-header-green",
                List.of("waitingFee", "redelivery", "prepull", "nsLift", "remark"))),
        null,
        null,
        null,
        null);
  }

  public static CostTableTemplateLayout seaDefault() {
    return flatFreightLayout(SEA_STANDARD_FIELDS);
  }

  public static CostTableTemplateLayout fumigationDefault() {
    List<String> fieldOrder =
        List.of(
            "port",
            "station",
            "nonOakOutdoor",
            "nonOakIndoor",
            "nonOakQuoteSummer",
            "nonOakQuoteWinter",
            "oakOutdoor",
            "oakIndoor",
            "oakQuoteSummer",
            "oakQuoteWinter",
            "remark",
            "updatedAt");
    return new CostTableTemplateLayout(
        List.of(
            new CostTableTemplateGroup(
                "nonOak",
                "page.costLibrary.fumigationGroups.nonOak",
                "fumigation-header-primary",
                List.of(
                    "nonOakOutdoor",
                    "nonOakIndoor",
                    "nonOakQuoteSummer",
                    "nonOakQuoteWinter")),
            new CostTableTemplateGroup(
                "oak",
                "page.costLibrary.fumigationGroups.oak",
                "fumigation-header-primary",
                List.of(
                    "oakOutdoor",
                    "oakIndoor",
                    "oakQuoteSummer",
                    "oakQuoteWinter"))),
        fieldOrder,
        null,
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
