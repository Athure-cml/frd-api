package com.furuiduo.quote.quote.support;

import java.util.ArrayList;
import java.util.List;

import com.furuiduo.quote.cost.dto.CostExportColumn;
import com.furuiduo.quote.cost.dto.CostTableTemplateLayout;
import com.furuiduo.quote.cost.support.CostTemplateExcelSupport;
/** 报价库默认导出列（与前端报价库表头一致，不含勾选/序号/关联/操作）。 */
public final class QuoteLibraryExportColumns {

  private QuoteLibraryExportColumns() {}

  public static List<CostExportColumn> roadDefaults() {
    return List.of(
        col("zipCode", "ZIP CODE"),
        col("city", "CITY"),
        col("logYardNameAddress", "PICK UP ADDRESS"),
        col("state", "STATE"),
        col("por", "POR"),
        col("allInNoFm", "ALL IN"),
        col("allInFmOneWay", "ALL IN FM NON OAK"),
        col("allInFmRound", "ALL IN FM OAK"),
        col("cf_road_eff", "EFFECTIVE TIME"),
        col("validDate", "VALID TIME"),
        col("cf_road_remark", "REMARK"),
        col("nsLift", "NS LIFT"),
        col("chassis", "CHASSIS"),
        col("waitingFee", "WAITING"),
        col("redelivery", "REDELIVERY"),
        col("supplier", "SUPPLIER"),
        col("status", "状态"));
  }

  public static List<CostExportColumn> seaDefaults() {
    return List.of(
        col("por", "POR"),
        col("pol", "POL"),
        col("pod", "POD"),
        col("containerType", "箱型"),
        col("cf_sea_freight_eff", "生效期"),
        col("freightValidDate", "有效期"),
        col("allIn", "ALL IN (小计)"),
        col("ssl", "SSL (船公司)"),
        col("agent", "AGENT (代理)"),
        col("remark", "REMARK 备注"),
        col("enProductName", "英文品名"),
        col("status", "状态"));
  }

  public static List<CostExportColumn> fumigationDefaults(CostTableTemplateLayout layout) {
    List<CostExportColumn> columns =
        new ArrayList<>(CostTemplateExcelSupport.exportColumns("fumigation", layout));
    if (columns.stream().noneMatch(column -> "status".equals(column.field()))) {
      columns.add(col("status", "状态"));
    }
    return columns;
  }

  private static CostExportColumn col(String field, String header) {
    return new CostExportColumn(field, header);
  }
}
