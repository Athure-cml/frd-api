package com.furuiduo.quote.quote.dto;

import java.math.BigDecimal;
import java.util.Map;

import com.furuiduo.quote.quote.entity.QuoteCostMode;
import com.furuiduo.quote.quote.entity.QuoteOrderLine;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价明细保存")
public record QuoteLineSaveRequest(
    @Schema(description = "排序") Integer sort,
    @Schema(description = "项目名称") String itemName,
    @Schema(description = "规格") String spec,
    @Schema(description = "成本模式") String costMode,
    @Schema(description = "成本引用ID") Long costRefId,
    @Schema(description = "数量") BigDecimal quantity,
    @Schema(description = "单位") String unit,
    @Schema(description = "单价") BigDecimal unitPrice,
    @Schema(description = "扩展字段") Map<String, Object> extraJson) {

  public QuoteCostMode parsedCostMode() {
    if (costMode == null || costMode.isBlank()) {
      return QuoteCostMode.MANUAL;
    }
    return QuoteCostMode.valueOf(costMode);
  }

  public static QuoteLineSaveRequest from(QuoteOrderLine line) {
    return new QuoteLineSaveRequest(
        line.getSort(),
        line.getItemName(),
        line.getSpec(),
        line.getCostMode().name(),
        line.getCostRefId(),
        line.getQuantity(),
        line.getUnit(),
        line.getUnitPrice(),
        line.getExtraJson());
  }
}
