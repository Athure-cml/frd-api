package com.furuiduo.quote.cost.dto;

import java.math.BigDecimal;
import java.util.Map;

import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.quote.support.QuoteDateTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "熏蒸成本")
public record FumigationCostResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "PORT") String port,
    @Schema(description = "STATION") String station,
    @Schema(description = "NON-OAK OUTDOOR") BigDecimal nonOakOutdoor,
    @Schema(description = "NON-OAK IN DOOR") BigDecimal nonOakIndoor,
    @Schema(description = "NON-OAK 报价(夏季)") String nonOakQuoteSummer,
    @Schema(description = "NON-OAK 报价(冬季)") String nonOakQuoteWinter,
    @Schema(description = "OAK OUTDOOR") BigDecimal oakOutdoor,
    @Schema(description = "OAK IN DOOR") BigDecimal oakIndoor,
    @Schema(description = "OAK 报价(夏季)") String oakQuoteSummer,
    @Schema(description = "OAK 报价(冬季)") String oakQuoteWinter,
    @Schema(description = "备注") String remark,
    @Schema(description = "扩展字段") Map<String, Object> extraFields,
    @Schema(description = "更新时间") String updatedAt) {

  public static FumigationCostResponse from(CostFumigation entity) {
    return new FumigationCostResponse(
        entity.getId(),
        entity.getPort(),
        entity.getStation(),
        entity.getNonOakOutdoor(),
        entity.getNonOakIndoor(),
        entity.getNonOakQuoteSummer(),
        entity.getNonOakQuoteWinter(),
        entity.getOakOutdoor(),
        entity.getOakIndoor(),
        entity.getOakQuoteSummer(),
        entity.getOakQuoteWinter(),
        entity.getRemark(),
        entity.getExtraFields(),
        QuoteDateTimes.format(entity.getUpdatedAt()));
  }
}
