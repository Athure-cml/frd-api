package com.furuiduo.quote.quote.dto;

import java.math.BigDecimal;
import java.util.Map;

import com.furuiduo.quote.quote.entity.QuoteOrderLine;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价明细行")
public record QuoteLineResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "排序") Integer sort,
    @Schema(description = "项目名称") String itemName,
    @Schema(description = "规格") String spec,
    @Schema(description = "成本模式") String costMode,
    @Schema(description = "成本引用ID") Long costRefId,
    @Schema(description = "数量") BigDecimal quantity,
    @Schema(description = "单位") String unit,
    @Schema(description = "单价") BigDecimal unitPrice,
    @Schema(description = "金额") BigDecimal amount,
    @Schema(description = "扩展字段") Map<String, Object> extraJson) {

  public static QuoteLineResponse from(QuoteOrderLine line) {
    return new QuoteLineResponse(
        line.getId(),
        line.getSort(),
        line.getItemName(),
        line.getSpec(),
        line.getCostMode().name(),
        line.getCostRefId(),
        line.getQuantity(),
        line.getUnit(),
        line.getUnitPrice(),
        line.getAmount(),
        line.getExtraJson());
  }
}
