package com.furuiduo.quote.cost.dto;

import java.math.BigDecimal;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "熏蒸成本保存")
public record FumigationCostSaveRequest(
    @Schema(description = "港口 PORT") String port,
    @Schema(description = "场站 STATION") String station,
    @Schema(description = "NON-OAK OUTDOOR") BigDecimal nonOakOutdoor,
    @Schema(description = "NON-OAK IN DOOR") BigDecimal nonOakIndoor,
    @Schema(description = "NON-OAK 报价(夏季)") String nonOakQuoteSummer,
    @Schema(description = "NON-OAK 报价(冬季)") String nonOakQuoteWinter,
    @Schema(description = "OAK OUTDOOR") BigDecimal oakOutdoor,
    @Schema(description = "OAK IN DOOR") BigDecimal oakIndoor,
    @Schema(description = "OAK 报价(夏季)") String oakQuoteSummer,
    @Schema(description = "OAK 报价(冬季)") String oakQuoteWinter,
    @Schema(description = "备注") String remark,
    @Schema(description = "扩展字段") Map<String, Object> extraFields) {}
